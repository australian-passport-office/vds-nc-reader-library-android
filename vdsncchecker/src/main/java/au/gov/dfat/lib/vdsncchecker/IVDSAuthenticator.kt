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

import au.gov.dfat.lib.vdsncchecker.repository.ICertificateRepository
import kotlinx.serialization.ExperimentalSerializationApi

/**
 *
 * interface for authentication of VDS
 *
 */
interface IVDSAuthenticator {
    /**
     *
     * @param vds - a VDS
     * @param cscaCertData - byte array representing a CSCA certificate
     * @param cscaCertSHA256Hash - string representing SHA256 hash of the CSCA certificate
     * @param crlData - collection of byte arrays representing CRLs
     *
     * @return True if the 'VDS' is authentic, otherwise throws an exception.
     *
     */
    @ExperimentalSerializationApi
    fun verifyVDS(vds: VDS, cscaCertData: ByteArray, cscaCertSHA256Hash: String, crlData: Collection<ByteArray>): Boolean

    /**
     *
     * @param vds - a VDS
     * @param cscaCertData - byte array representing a CSCA certificate
     * @param cscaCertSHA256Hash - string representing SHA256 hash of the CSCA certificate
     * @param crlData - byte array representing a CRL
     *
     * @return True if the 'VDS' is authentic, otherwise throws an exception.
     *
     */
    fun verifyVDS(vds: VDS, cscaCertData: ByteArray, cscaCertSHA256Hash: String, crlData: ByteArray): Boolean

    /**
     *
     * @param vds - a VDS
     * @param certificateRepository - a certificate repository to handle CSCA/CRL selection
     *
     * @return True if the 'VDS' is authentic, otherwise throws an exception.
     *
     */
    fun verifyVDS(vds: VDS, certificateRepository: ICertificateRepository): Boolean
}