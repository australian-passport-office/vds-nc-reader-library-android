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
package au.gov.dfat.lib.vdsncchecker

import android.util.Base64
import au.gov.dfat.lib.vdsncchecker.repository.ICertificateRepository
import kotlinx.serialization.*
import org.bouncycastle.asn1.*
import org.bouncycastle.asn1.DEROctetString
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier
import org.bouncycastle.util.Arrays
import org.erdtman.jcs.JsonCanonicalizer
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.math.BigInteger
import java.security.MessageDigest
import java.security.Signature
import java.security.cert.*

/**
 *
 * class or Authenticating VDS'
 *
 */
open class VDSAuthenticator : IVDSAuthenticator {


    /**
     *
     * initialise security
     *
     */
    open fun setupSecurity(){

    }

    init {
        setupSecurity()
    }

    var isRevocationEnabled: Boolean = false
    // use AKI vs SKI for comparison
    // consider using a function for use case scenarios
    var useAkiForComparison: Boolean = false
    // for testing
    var skipCRLCheck: Boolean = false
    /**
     * Validates a 'VDS', returning true if it is authentic and throwing an exception otherwise.
     *
     * @param vds A 'VDS'.
     * @param cscaCertData CSCA certificate data (in DER format).
     * @param cscaCertSHA256Hash SHA256 hash of CSCA certificate data - used to ensure the CSCA certificate data has not been tampered with.
     * @param crlData CRL data (in DER format) - used for CRL verification.
     * @return True if the 'VDS' is authentic, otherwise throws an exception.
     * @throws VDSVerifyException if verification fails.
     */
    @ExperimentalSerializationApi
    override fun verifyVDS(vds: VDS, cscaCertData: ByteArray, cscaCertSHA256Hash: String, crlData: Collection<ByteArray>): Boolean {
        // CSCA
        verifyCSCACertHash(cscaCertData, cscaCertSHA256Hash)

        // CRL
        verifyCRLSignatureUsingCSCACertPublicKey(crlData, cscaCertData)

        // BSC
        verifyBSCCertNotRevokedInCRL(vds, crlData)
        verifyBSCCertAKIMatchesCSCACertAKI(vds, cscaCertData)
        verifyBSCCertIncludesCSCACertInCertPath(vds, cscaCertData)

        // VDS
        verifyVDSSignature(vds)

        return true
    }

    /**
     *
     * override of verifyVDS to allow single crl
     *
     */
    override fun verifyVDS(
        vds: VDS,
        cscaCertData: ByteArray,
        cscaCertSHA256Hash: String,
        crlData: ByteArray
    ): Boolean {
        return verifyVDS(vds, cscaCertData, cscaCertSHA256Hash, arrayListOf(crlData))
    }

    /**
     *
     * override method of verifyVDS to allow certificate repository to verify csca/crl data
     *
     * @param certificateRepository - certificate repository holding multiple CSCA entries
     *
     */
    override fun verifyVDS(vds: VDS, certificateRepository: ICertificateRepository): Boolean{

        val cscaData = verifyCSCAExists(certificateRepository, vds)

        if(cscaData?.crl != null){

            return verifyVDS(vds, cscaData.certificate,  cscaData.hash, cscaData.crl)
        }

        return false
    }

    /**
     *
     * Verifies CSCA exists for given VDS
     *
     * @param certificateRepository - repository holding multiple CSCA entries
     * @param vds - a 'VDS'
     *
     */
    private fun verifyCSCAExists(certificateRepository: ICertificateRepository, vds: VDS) : CSCAData?{
        val bscCertData = getBSCCertDataFromVDS(vds)
        var data = certificateRepository.findIssuer(bscCertData, certFactory)
        if(data != null){

            return CSCAData(data.hash, data.certificate, data.crl?.data)
        }

        return null

    }


    //region Security Checks - CSCA

    /**
     * Verifies that our CSCA certificate has not been tampered with by checking if its hash matches a known trusted hash.
     *
     * @param cscaCertData CSCA certificate data (in DER format).
     * @param cscaCertSHA256Hash Trusted SHA256 hash of CSCA certificate data.
     * @throws VDSVerifyException if the hash does not match the trusted hash.
     */
    protected open fun verifyCSCACertHash(cscaCertData: ByteArray, cscaCertSHA256Hash: String) {
        val cscaHash = try {
            MessageDigest
                .getInstance("SHA-256")
                .digest(cscaCertData)
                .fold("", { str, it -> str + "%02x".format(it) })
        } catch (_: Exception) { null }

        if (cscaHash != cscaCertSHA256Hash) {
            throw VDSVerifyException(VDSVerifyError.VERIFY_CSCA_CERT_HASH_ERROR)
        }
    }

    //endregion

    //region Security Checks - CRL

    /**
     * Verifies a CRL's signature using a CSCA public key.
     *
     * @param crlData CRL data (in DER format)
     * @param cscaCertData CSCA certificate data (in DER format)
     * @throws VDSVerifyException if verification fails
     */
    protected open fun verifyCRLSignatureUsingCSCACertPublicKey(crlData: Collection<ByteArray>, cscaCertData: ByteArray) {



        // Prepare
        val cscaCert = getCSCACertFromCSCACertData(cscaCertData)

        crlData.forEach{
            val crl = getCRLFromCRLData(it) as X509CRL

            // Verify signature
            try {
                crl.verify(cscaCert.publicKey)
            } catch (_: Exception) {
                if(skipCRLCheck){
                    return
                }
                throw VDSVerifyException(VDSVerifyError.VERIFY_CRL_ERROR)
            }
        }
    }

    //endregion

    //region Security Checks - BSC

    /**
     * Verifies a BSC certificate is not revoked in a CRL.
     *
     * @param vds a `VDS`.
     * @param crlData CRL data  (in DER format).
     * @throws VDSVerifyException if verification fails.
     */
    protected open fun verifyBSCCertNotRevokedInCRL(vds: VDS, crlData: Collection<ByteArray>) {

        // Prepare
        val bscCertData = getBSCCertDataFromVDS(vds)
        val bscCert = getBSCCertFromBSCCertData(bscCertData)

        crlData.forEach{
            val crl = getCRLFromCRLData(it)

            // Check if BSC certificate is revoked in CRL
            if (crl.isRevoked(bscCert)) {
                if(skipCRLCheck){
                    return
                }
                throw VDSVerifyException(VDSVerifyError.VERIFY_BSC_CERT_NOT_IN_CRL_ERROR)
            }
        }
    }


    /**
     *
     * extract SKI from certificate
     *
     * @param certificate - the certificate to extract SKI from
     *
     * @return the SKI in a list of Bytes
     *
     */
    private fun getCertificateSKI(certificate: X509Certificate): List<Byte>?{
        // Get certificate SKI
        val skiExtOid = Extension.subjectKeyIdentifier.id
        try{
            val skiExtensionValue = certificate.getExtensionValue(skiExtOid)
            val skiOctets = DEROctetString.getInstance(skiExtensionValue).octets
            val skisubjectKeyIdentifier = SubjectKeyIdentifier.getInstance(skiOctets)
            return skisubjectKeyIdentifier.keyIdentifier.asList()
        }catch(exception: Exception){

        }
        return null
    }

    /**
     *
     * extract AKI from certificate
     *
     * @param certificate - the certificate to extract AKI from
     *
     * @return the AKI in a list of Bytes
     *
     */
    private fun getCertificateAKI(certificate: X509Certificate): List<Byte>?{
        // Get certificate AKI
        val akiExtOid = Extension.authorityKeyIdentifier.id
        try{
            val akiFullExtVal = certificate.getExtensionValue(akiExtOid)
            val akiExtVal = ASN1OctetString.getInstance(akiFullExtVal).octets
            val aki = AuthorityKeyIdentifier.getInstance(akiExtVal)
            return aki.keyIdentifier.asList()
        }catch(exception: Exception){

        }
        return null
    }

    /**
     * Verifies a BSC certificate's AKI (Authority Key Identifier) matches a CSCA certificate's AKI.
     *
     * @param vds a `VDS`.
     * @param cscaCertData CSCA certificate data (in DER format).
     * @throws VDSVerifyException if verification fails.
     */
    protected open fun verifyBSCCertAKIMatchesCSCACertAKI(vds: VDS, cscaCertData: ByteArray) {
        // Prepare
        val bscCertData = getBSCCertDataFromVDS(vds)
        val bscCert = getBSCCertFromBSCCertData(bscCertData)
        val cscaCert = getCSCACertFromCSCACertData(cscaCertData)

        try {
            // Get BSC AKI
            val bscAKIKeyIdentifier = getCertificateAKI(bscCert)

            // get CSCA identifier
            val cscaSkiKeyIdentifier = if(useAkiForComparison){
                getCertificateAKI(cscaCert)
            }else{
                getCertificateSKI(cscaCert)
            }

            // Check if BSC certificate's AKI matches CSCA certificate's AKI
            if (bscAKIKeyIdentifier != cscaSkiKeyIdentifier) {
                throw Exception()
            }
        } catch (_: Exception) {
            throw VDSVerifyException(VDSVerifyError.VERIFY_BSC_CERT_AKI_MATCHES_CSCA_CERT_AKI_ERROR)
        }
    }

    /**
     * Verifies a BSC certificate's includes a CSCA certificate in its certification path.
     *
     * @param vds a `VDS`.
     * @param cscaCertData CSCA certificate data (in DER format).
     * @throws VDSVerifyException if verification fails.
     */
    protected open fun verifyBSCCertIncludesCSCACertInCertPath(vds: VDS, cscaCertData: ByteArray) {
        // Prepare
        val bscCertData = getBSCCertDataFromVDS(vds)
        val bscCert = getBSCCertFromBSCCertData(bscCertData)
        val cscaCert = getCSCACertFromCSCACertData(cscaCertData)

        // Validate
        try {
            val certPathValidator = CertPathValidator.getInstance("PKIX")
            val certPath = certFactory.generateCertPath(listOf(bscCert))
            val trustAnchors = hashSetOf(TrustAnchor(cscaCert, null))
            val pkixParams = PKIXParameters(trustAnchors)

            // CRL already checked if revoked in previous method verifyBSCCertNotRevokedInCRL
            // Note: the default validator cannot properly determine revocation status and throws a CertPathValidatorException
            // with a message of "could not determine revocation status"
            pkixParams.isRevocationEnabled = isRevocationEnabled

            // Check if BSC certificate includes CSCA certificate in its certification path
            certPathValidator.validate(certPath, pkixParams) ?: throw Exception()


        } catch (e: Exception) {

            throw VDSVerifyException(VDSVerifyError.VERIFY_BSC_CERT_PATH_INCLUDES_CSCA_CERT_ERROR)
        }
    }

    //endregion

    //region Security Checks - VDS

    /**
     * Verifies a VDS's signature.
     *
     * @param vds a `VDS`.
     * @throws VDSVerifyException if verification fails.
     */
    protected open fun verifyVDSSignature(vds: VDS) {
        // Prepare
        val bscCertData = getBSCCertDataFromVDS(vds)
        val bscCert = getBSCCertFromBSCCertData(bscCertData)
        val canonicalJson = getCanonicalJsonFromVDS(vds)
        val sigData = getSignatureDataFromVDS(vds)
        val algo = getAlgorithmFromVDS(vds)

        // Verify signature
        try {
            val signature = Signature.getInstance(algo)
            signature.initVerify(bscCert)
            signature.update(canonicalJson.encodeToByteArray())

            // We need to use a DER-encoded signature for verification
            val derSigData = getDERSignatureFromECDSASignature(sigData)

            if (!signature.verify(derSigData)) {
                throw Exception()
            }
        } catch (e: Exception) {
            throw VDSVerifyException(VDSVerifyError.VERIFY_SIGNATURE_ERROR)
        }
    }

    //endregion

    //region Helper - CSCA

    /**
     * Gets a CSCA certificate from the given CSCA certificate data.
     *
     * @param cscaCertData CSCA certificate data (in DER format).
     * @throws VDSVerifyException if getting the public key fails.
     */
    private fun getCSCACertFromCSCACertData(cscaCertData: ByteArray): X509Certificate {
        return try { certFactory.generateCertificate(ByteArrayInputStream(cscaCertData)) as? X509Certificate } catch (_: Exception) { null }
                ?: throw VDSVerifyException(VDSVerifyError.LOAD_CSCA_CERT_ERROR)
    }

    //endregion

    //region Helper - CRL

    /**
     * Gets a CRL from the given CRL data.
     *
     * @param crlData CRL data (in DER format)
     * @return The CRL.
     * @throws VDSVerifyException if creating the CRL fails
     */
    private fun getCRLFromCRLData(crlData: ByteArray): CRL {
        return try { certFactory.generateCRL(ByteArrayInputStream(crlData)) as? X509CRL } catch (_: Exception) { null }
            ?: throw VDSVerifyException(VDSVerifyError.LOAD_CRL_ERROR)
    }

    //endregion

    //region Helper - BSC

    /**
     * Gets a BSC certificate from the given BSC certificate data.
     *
     * @param bscCertData BSC certificate data (in DER format).
     * @return BSC certificate.
     * @throws VDSVerifyException if creating the BSC certificate fails.
     */
    private fun getBSCCertFromBSCCertData(bscCertData: ByteArray): X509Certificate {
        return try { certFactory.generateCertificate(ByteArrayInputStream(bscCertData)) as? X509Certificate } catch (_: Exception) { null }
            ?: throw VDSVerifyException(VDSVerifyError.LOAD_BSC_CERT_ERROR)
    }

    //endregion

    //region Helper - VDS

    /**
     * Gets BSC certificate data from the given VDS.
     *
     * @param vds A `VDS`.
     * @return BSC certificate data (in DER format).
     * @throws VDSVerifyException if extracting the BSC certificate data fails.
     */
    private fun getBSCCertDataFromVDS(vds: VDS): ByteArray {
        return try { Base64.decode(vds.sig.cer, Base64.URL_SAFE) } catch (_: Exception) { null }
            ?: throw VDSVerifyException(VDSVerifyError.PARSE_BSC_CERT_ERROR)
    }

    /**
     * Gets signature data from the given VDS.
     *
     * @param vds A `VDS`.
     * @return Signature data.
     * @throws VDSVerifyException if extracting the signature data fails.
     */
    private fun getSignatureDataFromVDS(vds: VDS): ByteArray {
        return try { Base64.decode(vds.sig.sigvl, Base64.URL_SAFE) } catch (_: Exception) { null }
            ?: throw VDSVerifyException(VDSVerifyError.PARSE_SIGNATURE_ERROR)
    }

    /**
     * Gets canonical JSON data from the given VDS. We must use canonical JSON to ensure our hash
     * comparisons work.
     *
     * Canonical JSON is described here: http://gibson042.github.io/canonicaljson-spec/
     *
     * @param vds A 'VDS'
     * @return Canonical JSON representation of the 'VDS'.
     */
    @ExperimentalSerializationApi
    private fun getCanonicalJsonFromVDS(vds: VDS): String {
        return try {
            val jsonObject = JSONObject(vds.originalJson)
            val dataJsonObject = jsonObject.get("data")

            val jsonCanonicalizer = JsonCanonicalizer(dataJsonObject.toString())
            jsonCanonicalizer.encodedString
        } catch (e: Exception) {
            null
        } ?: throw VDSVerifyException(VDSVerifyError.PARSE_JSON_ERROR)
    }

    /**
     * Gets the algorithm for the given VDS.
     *
     * From the VDS spec:
     *
     * The SignatureAlgo field MUST be only one of the following values:
     * - ES256 – denotes ECDSA with Sha256 hashing algorithm
     * - ES384 – denotes ECDSA with Sha384 hashing algorithm
     * - ES512 – denotes ECDSA with Sha512 hashing algorithm
     *
     * The algorithm returned by this function can be passed into
     * [java.security.Signature.getInstance()].
     *
     * @param vds A `VDS`.
     * @return The algorithm for the given VDS algorithm string.
     */
    protected open fun getAlgorithmFromVDS(vds: VDS): String? {
        return when (vds.sig.alg) {
            "ES256" -> "SHA256withECDSA"
            "ES384" -> "SHA384withECDSA"
            "ES512" -> "SHA512withECDSA"
            else -> null
        }
    }

    //endregion

    //region Helper - Misc

    /**
     * Certificate factory used to parse CRL and CSCA/BSC certificates.
     * @throws VDSVerifyException if creating the certificate factory fails.
     */
    private val certFactory: CertificateFactory
        get() {
            return try {
                onGetCertificateFactory()
            } catch (_: Exception) { null }
                ?: throw VDSVerifyException(VDSVerifyError.CREATE_CERTIFICATE_FACTORY_ERROR)
        }

    open fun onGetCertificateFactory(): CertificateFactory{
        return  CertificateFactory.getInstance("X.509")
    }

    //endregion

    //region Helper - Misc

    /**
     * Gets the DER-encoded signature from the given ECDSA signature.
     *
     * We can only verify using DER-encoded signatures, and the VDS contains a ECDSA signature.
     * The DER-encoded signature can be passed into [java.security.Signature.verify()].
     *
     * @param signature An ECDSA signature (extracted from a VDS)
     * @return The DER-encoded signature for the given ECDSA signature.
     */
    private fun getDERSignatureFromECDSASignature(signature: ByteArray): ByteArray? {
        val r = BigInteger(1, Arrays.copyOfRange(signature, 0, 32))
        val s = BigInteger(1, Arrays.copyOfRange(signature, 32, 64))

        val v = ASN1EncodableVector()
        v.add(ASN1Integer(r))
        v.add(ASN1Integer(s))

        return DERSequence(v).getEncoded(ASN1Encoding.DER)
    }

    //endregion
}
