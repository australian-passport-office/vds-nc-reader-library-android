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

import android.os.Build
import androidx.annotation.RequiresApi
import au.gov.dfat.lib.vdsncchecker.data.CertificateVault
import au.gov.dfat.lib.vdsncchecker.utils.Logger
import java.net.URL
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.util.*
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection

/**
 *
 * Wrapper for CRL data. Used within Certificate Repository, to store data into Vault and to download updated CRL data
 *
 * @param url - CRL URL for updating the CRL data
 * @param data - the actual CRL data in ByteArray
 *
 */
@RequiresApi(Build.VERSION_CODES.O)
class CRLStoreWrapper(url: URL?, data: ByteArray?) {

    private val keychainKeyDatePrefix = "vdsncchecker.downloaded."
    private val keychainKeyCrlPrefix = "vdsncchecker.crldata."

    var url: URL? = null
    var data: ByteArray? = null
    var dateLastDownloaded: Date? = null

    init{
        this.url = url

        if(url == null && data != null)
        {
            // This option uses the given data and will never auto-update, given the lack of a URL
            this.data = data
        }
        else if(url != null && data != null){
            // In this option, the system will still use the value from the vault if it is present. Otherwise it will use the given data.
            val tempData = getCRLDataFromVault()
            if(tempData != null)
                this.data = tempData
            else
                this.data = data
        }
        else if(url != null && data == null){
            // no action necessary
        }
        else
        {
            // Can't do anything if there are no parameters
            throw IllegalArgumentException("Provide URL, data, or both")
        }

        if(data == null){
            // if no initial data is provided, check the Vault for it
            val temp = Base64.getDecoder().decode(CertificateVault.getInstance()!!.getEntry(keychainKeyCrlPrefix + url.toString()))
            if(temp != null && !temp.isEmpty())
                this.data = temp
        }
        else {
            this.data = data
        }

        this.saveCRLDataToVault()
        this.dateLastDownloaded = getUpdatedDateFromVault()
    }

    /**
     *
     * Save CRL data to the Vault
     *
     */
    private fun saveCRLDataToVault(){
        if(url == null)
            return

        CertificateVault.getInstance()!!.setEntry(keychainKeyCrlPrefix + this.url.toString(), Base64.getEncoder().encodeToString(this.data))

    }

    /**
     *
     * Save updated CRL date to Vault
     *
     */
    private fun saveUpdatedDateToVault(){
        if(dateLastDownloaded == null)
            return

        CertificateVault.getInstance()!!.setEntry(keychainKeyDatePrefix + this.url.toString(), this.dateLastDownloaded!!.toString())
    }

    /**
     *
     * Get CRL date from vault
     *
     * @return the CRL updated date
     *
     */
    private fun getUpdatedDateFromVault() : Date? {
        val value = CertificateVault.getInstance()!!.getEntry(keychainKeyDatePrefix + this.url.toString())
        if(value == null || value == "")
            return null

        return Date(value)
    }

    /**
     *
     * Get CRL from the vault
     *
     * @return the CRL data in ByteArray
     *
     */
    private fun getCRLDataFromVault() : ByteArray? {
        if(url == null) // not in vault if theres no URL
        {
            if(data != null) // but if theres already data on the object, return that
                return data
            return null
        }

        val entryAsString = CertificateVault.getInstance()!!.getEntry(keychainKeyCrlPrefix + this.url.toString())
            ?: return null

        return Base64.getDecoder().decode(entryAsString)
    }


    /**
     *
     * Download CRL update from stored URL
     *
     * @return false on any exception that would indicate a network failure
     *
     */
    suspend fun download(logger: Logger? = null) : Boolean {

        try {
            if (url == null) {
                return true
            }

            var connection = if (url!!.protocol == "https") {
                url!!.openConnection() as HttpsURLConnection

                val allHostsValid = HostnameVerifier { hostname, session -> true }
                HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid)

            } else {
                url!!.openConnection() as HttpURLConnection
            }
            logger?.printLine("URL: ${url?.path}")
            val input = BufferedInputStream(url!!.openStream(), 8192)

            val output = input.readBytes()

            input.close()

            this.data = output
            this.dateLastDownloaded = Date()
            logger?.printLine("Downloaded: $dateLastDownloaded")

            this.saveCRLDataToVault()
            this.saveUpdatedDateToVault()

            return true
        }
        catch(e: Exception)
        {
            // return false so that the parent can handle rescheduling
            logger?.printLine(e.message ?: "Exception occured when trying to download from $url")
            return false
        }
    }


}