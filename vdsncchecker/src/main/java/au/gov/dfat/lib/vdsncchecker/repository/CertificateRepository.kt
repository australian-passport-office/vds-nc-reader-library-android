//  Copyright (c) 2021, Commonwealth of Australia. vds.support@dfat.gov.au
//
//  Licensed under the Apache License, Version 2.0 (the "License"); you may not
//  use this file except in compliance with the License. You may obtain a copy
//  of the License at:
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
//  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
//  License for the specific language governing permissions and limitations
//  under the License.

package au.gov.dfat.lib.vdsncchecker.repository

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import au.gov.dfat.lib.vdsncchecker.CertificateData
import au.gov.dfat.lib.vdsncchecker.data.CertificateVault
import au.gov.dfat.lib.vdsncchecker.utils.Logger
import au.gov.dfat.lib.vdsncchecker.utils.NetworkMonitor
import au.gov.dfat.lib.vdsncchecker.utils.NetworkState
import kotlinx.coroutines.*
import java.security.cert.CertificateFactory
import org.bouncycastle.asn1.x500.style.IETFUtils
import org.bouncycastle.cert.X509CertificateHolder
import java.lang.Exception
import java.lang.ref.WeakReference
import java.net.URL
import java.security.KeyException
import java.security.cert.CRL
import java.security.cert.CertificateException
import java.util.*
import kotlin.concurrent.fixedRateTimer


/**
 *
 * manages multiple CSCA and CRLs
 *
 */
class CertificateRepository private constructor() : ICertificateRepository {

    private var logger: Logger? = null
    private val loggerTag = "Logger-CertificateRepository"

    // list of certificates
    var cscaCertificates: MutableList<CertificateData> = mutableListOf()

    // filter certificates by country name
    var filterCertificatesByCountryName = false

    // timer period for reconnection timer
    private var reconnectTimerPeriod: Int = 600
    // just one second in milliseconds
    private var oneSecondInMillis: Int = 1000

    // instance factory
    private object Factory {
        val INSTANCE = CertificateRepository()
    }

    // instance accessor
    companion object {
        val instance: CertificateRepository by lazy { Factory.INSTANCE }
    }

    fun setLogger(logger: Logger){
        this.logger = logger
    }

    /**
     *
     * delegate listeners
     *
     */
    private val didUpdateCRLDataListeners: MutableList<WeakReference<CertificateRepositoryDelegate>> =
        mutableListOf()

    /**
     *
     * network listeners
     *
     */
    private val networkListeners: MutableList<WeakReference<NetworkMonitor.NetworkDelegate>> =
        mutableListOf()

    /**
     *
     * add listener to list of listeners
     *
     * @param listener - the CertificateRepositoryDelegate to listen for updated CRLs
     *
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun setDidUpdateCRLDataListener(listener: CertificateRepositoryDelegate) {
        val ref = WeakReference(listener)
        if (!didUpdateCRLDataListeners.contains(ref)) {
            didUpdateCRLDataListeners.add(ref)
        }
        didUpdateCRLDataListeners.removeIf { l -> l.get() == null }
    }

    /**
     *
     * remove listeners on destroy, etc
     *
     * @param listener - the CertificateRepositoryDelegate listening for CRL updates
     *
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun removeDidUpdateCRLDataListener(listener: CertificateRepositoryDelegate) {
        didUpdateCRLDataListeners.removeIf { l -> l.get() == listener }
    }


    /**
     *
     * CRLS mapped from stored CSCA certificates
     *
     */
    val crls: Array<CRLStoreWrapper>
        get(){
            return cscaCertificates.filter { it.crl != null }.map { certs ->
                // filtered, cannot be null
                certs.crl!!
            }.toTypedArray()
    }

    /**
     *
     * find the certificate Commonm Name
     *
     * @param certificate - certificate in ByteArray format
     *
     */
    private fun findCN(certificate: ByteArray): String? {
        try {
            val holder = X509CertificateHolder(certificate)
            // get country entry, should always be first
            val rdn = holder.subject.rdNs[0]
            return IETFUtils.valueToString(rdn.first.value)
        } catch (exception: Exception) {

        }

        return null

    }

    /**
     *
     * add a certificate to the list of certificates
     *
     * @param certificate - certificate in ByteArray format
     * @param hash - SHA256 hash of the certificate
     * @param crl - CRL wrapper
     *
     */
    override fun addCertificate(certificate: ByteArray, hash: String, crl: CRLStoreWrapper) {
        val name = findCN(certificate)
        cscaCertificates.add(CertificateData(hash, certificate, crl, name))

    }

    /**
     *
     * add a certificate to the list of certificates
     *
     * @param certificate - certificate in certificate data object format
     *
     */
    override fun addCertificate(certificate: CertificateData) {
        cscaCertificates.add(certificate)
    }

    /**
     *
     * find issuer of certificate
     *
     * @param certificate - the certificate needing an issuer found
     * @param certFactory - java security CertificateFactory
     *
     * @return CertificateData? - the CSCA data
     *
     */
    override fun findIssuer(
        certificate: ByteArray,
        certFactory: CertificateFactory
    ): CertificateData? {

        var ca = matchCertificateWithCa(certificate, certFactory)
        if (ca != null) {
            return ca
        }

        return null

    }


    /**
     *
     *
     *
     * @param certificateData - certificate to be matched
     * @param certFactory - java security CertificateFactory
     *
     * @return CertificateData? - the CSCA data
     *
     */
    @Throws(CertificateException::class)
    private fun matchCertificateWithCa(
        certificateData: ByteArray,
        certFactory: CertificateFactory
    ): CertificateData? {

        var name = if (filterCertificatesByCountryName) {
            findCN(certificateData)
        } else {
            null
        }

        var certificate = try {
            certFactory.generateCertificate(certificateData.inputStream())
        } catch (exception: CertificateException) {
            // no point in continuing if we cannot generate a certificate
            throw exception
        }
        var certs = getCertificates(name)
        certs.iterator().forEach {
            try {
                val csca = certFactory.generateCertificate(it.certificate.inputStream())
                certificate.verify(csca.publicKey)

                return it
            } catch (exception: KeyException) {
                // not this one

            } catch (exception: Exception) {
                // something unexpected happened

            }

        }

        return null
    }

    /**
     *
     *  get any certificates passed into the repository if issuing country exists, filter by it
     *
     *  @param issuingCountry - the issuing country to filter by
     *
     *  @return array of any certificates matching the issuing country
     *
     */
    private fun getCertificates(issuingCountry: String? = null): Array<CertificateData> {

        if (issuingCountry != null) {
            return cscaCertificates.filter { i -> i.issuingCountry == issuingCountry }
                .toTypedArray()
        }

        return cscaCertificates.toTypedArray()
    }

    // start CRL management

    private var updateTimer: Timer? = null

    private var reconnectTimer: Timer? = null

    private var connectionFailure = false

    private var isAutoUpdating = false

    var secondsBetweenUpdates = 86400 // 1 day

    var maxSecondsBeforeOverdue = 864000 // 10 days

    private var appContext: Context? = null

    /**
     *
     * set the context of the certificate repository
     *
     * this is needed for CRL functionality
     *
     */
    fun setContext(app: Context) {
        appContext = app

        CertificateVault.setContext(app);
    }

    /**
     * Inspects the CRL list and verifies if any entries are older than the maxSecondsBeforeOverdue age in seconds
     *
     * @return whether an update is due or not
     *
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun isUpdateOverdue(): Boolean {
        val c = Calendar.getInstance()

        crls.filter { crl -> crl.url != null }.forEach {

            if (it.dateLastDownloaded == null){
                logger?.printLine("dateLastDownloaded if ${it.url} is null", loggerTag)
                return true
            }

            val date = Date()
            c.time = it.dateLastDownloaded!!
            c.add(Calendar.SECOND, maxSecondsBeforeOverdue)
            logger?.printLine("it.dateLastDownloaded = ${it.dateLastDownloaded}", loggerTag)
            if (date.after(c.time)){
                logger?.printLine("$date --- $c", loggerTag)
                return true
            }
        }
        logger?.printLine("CRLs all up to date", loggerTag)
        return false
    }

    /**
     * Begin a background process which updates the CRL list from the sources provided on a periodic basis.
     * Default update interval is 1 day/86400 seconds.
     *
     * @param p_secondsBetweenUpdates Overrides the default update interval
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun startAutoUpdatingCRLData(p_secondsBetweenUpdates: Int? = null) {
        if (p_secondsBetweenUpdates != null){
            this.secondsBetweenUpdates = p_secondsBetweenUpdates
        }

        isAutoUpdating = true

        updateTimer?.cancel() // cancel the timer if it already exists

        val asMilliseconds = (this.secondsBetweenUpdates * 1000).toLong()

        updateTimer = fixedRateTimer("crlCheckTimer", true, asMilliseconds, asMilliseconds) {
            updateCRLData()
        }
    }

    /**
     * Cancels the CRL auto update process
     */
    fun stopAutoUpdatingCRLData() {
        isAutoUpdating = false
        updateTimer?.cancel()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun removeReconnectListener(){
        val networkMonitor = NetworkMonitor.getInstance(appContext!!)
        networkMonitor.removeNetworkListener(retryOnReconnect)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private val retryOnReconnect = NetworkMonitor.NetworkDelegate { state ->
        if(state == NetworkState.Connected){
            logger?.printLine("reconnect trying now that network state is connected again", loggerTag)
            retryCrlUpdate()
            // remove this listener after it has completed
            removeReconnectListener()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun retryCrlUpdate(){
        updateCRLData() // perform an immediate update no matter what

        // if the user had set up auto-updates, recommence them
        if (isAutoUpdating) {
            startAutoUpdatingCRLData()

        }
    }

    /**
     *
     * in the event of loss of network connection, we wait for a reconnection
     *
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun waitForReconnection() {

        if (appContext == null){
            throw Exception("Application context must first be initialised via a call to CRLManager.setContext")
        }

        //set a network listener to try updating again
        val networkMonitor = NetworkMonitor.getInstance(appContext!!)
        if(!networkMonitor.available){
            logger?.printLine("setting network listener for retry connection", loggerTag)
            networkMonitor.setNetworkListener(retryOnReconnect)
        } else{
            logger?.printLine("retrying CRL download straight away", loggerTag)
            retryCrlUpdate()
        }

    }

    /**
     *
     * failed downloads of CRLs
     *
     */
    private val failedCrls: MutableList<URL?> = mutableListOf()

    /**
     * Manually initiate a refresh of CRL data based on the URLs provided in the CRLStoreWrapper list
     *
     * @param complete A callback function called on successful update of the list
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun updateCRLData(complete: ((count: Int) -> Unit)? = null) {

        if(appContext != null){

            val networkMonitor = NetworkMonitor.getInstance(appContext!!)

            GlobalScope.launch {

                connectionFailure = !networkMonitor.available

                if (connectionFailure ) // If connection literally failed, we need to perform additional steps
                {
                    withContext(Dispatchers.Main){
                        waitForReconnection()
                    }
                }
                else{
                    // Since this parent method is itself asynchronous, we will call .download synchronously and download each item individually in sequence
                    // if they fail, we will store them

                    attemptDownloadOfCRLs()

                    didUpdateCRLDataListeners.forEach { listener ->
                        listener.get()?.didUpdateCRLData(crls.size - failedCrls.size)
                    }
                    if (complete != null) {
                        complete(crls.size - failedCrls.size)
                    }
                    if(failedCrls.any()){

                        reconnectTimer?.cancel() // just in case
                        val asMilliseconds = (reconnectTimerPeriod * oneSecondInMillis).toLong()

                        reconnectTimer = fixedRateTimer("crlReconnectTimer", true, asMilliseconds, asMilliseconds) {

                            reconnectTimer?.cancel() // stop the reconnect timer

                            retryCrlUpdate()
                        }
                    }

                }
            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun attemptDownloadOfCRLs(){
        crls.filter { c -> c.url != null }.forEach {

            if(!it.download(logger)){
                if(!failedCrls.contains(it.url)){
                    failedCrls.add(it.url)
                }
            }else{
                if(failedCrls.contains(it.url)){
                    failedCrls.remove(it.url)
                }

                cscaCertificates.filter { c -> c.crl?.url != null }.forEach{ cert ->
                    if(cert.crl?.url == it.url){
                        cert.crl = it
                    }
                }

            }
        }
    }


    /**
     *
     * listener for CRL updates
     *
     */
    fun interface CertificateRepositoryDelegate {
        fun didUpdateCRLData(size: Int?)
    }

}



