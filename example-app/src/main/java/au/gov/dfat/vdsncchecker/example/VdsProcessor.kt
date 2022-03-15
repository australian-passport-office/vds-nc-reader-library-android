package au.gov.dfat.vdsncchecker.example

import au.gov.dfat.lib.vdsncchecker.VDS
import au.gov.dfat.lib.vdsncchecker.VDSAuthenticator
import au.gov.dfat.lib.vdsncchecker.VDSReader
import au.gov.dfat.lib.vdsncchecker.repository.CertificateRepository

/**
 *
 * processing class primarily for eliminating duplicate code
 *
 */
class VdsProcessor {

    fun parseVdsData(data: String): VDS? {
        val vdsReader = VDSReader()
        val vds = try {
            vdsReader.decodeVDSFromJsonString(data)
        } catch (_: Exception) {
            null
        }
        return vds
    }

    fun authenticateVdsData(vds: VDS): Boolean{

        var certRepo = CertificateRepository.instance
        var authenticator = LocalAuthenticator()

        val isVdsValid =
            try {
                authenticator.verifyVDS(
                    vds,
                    certRepo
                )
            } catch (_: Exception) {
                return false
            }
        return isVdsValid
    }

    fun authenticateVdsData(vds: VDS, cscaCertData: ByteArray, cscaCertSHA256Hash: String, crls: MutableList<ByteArray>): Boolean{

        var authenticator = VDSAuthenticator()
        val isVdsValid =
            try {
                authenticator.verifyVDS(
                    vds,
                    cscaCertData,
                    cscaCertSHA256Hash,
                    crls
                )
            } catch (_: Exception) {
                return false
            }
        return isVdsValid
    }

}