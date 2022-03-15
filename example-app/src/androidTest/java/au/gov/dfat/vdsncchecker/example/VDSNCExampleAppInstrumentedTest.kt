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

package au.gov.dfat.vdsncchecker.example

import androidx.test.ext.junit.runners.AndroidJUnit4
import au.gov.dfat.lib.vdsncchecker.*
import au.gov.dfat.lib.vdsncchecker.repository.CertificateRepository

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class VDSNCExampleAppInstrumentedTest {
    @Test
    fun testValidVDS() {
        // Decode VDS
        val vdsReader = VDSReader()
        val authenticator = VDSAuthenticator()

        val vds = try { vdsReader.decodeVDSFromJsonString(Data.validVDSJson) } catch (_: Exception) { null }
        assertNotNull(vds)

        // Verify VDS
        val isVdsValid = try { authenticator.verifyVDS(vds as VDS, Data.cscaCertData, Data.cscaCertSHA256Hash, Data.crlData) } catch (_: Exception) { null }
        assertNotNull(isVdsValid)
        assertEquals(isVdsValid, true)
    }

    @Test
    fun testInvalidVDS() {
        // Decode VDS
        val vdsReader = VDSReader()
        val authenticator = VDSAuthenticator()
        val vds = try { vdsReader.decodeVDSFromJsonString(Data.invalidVDSJson) } catch (_: Exception) { null }
        assertNotNull(vds)

        // Verify VDS
        try {
            authenticator.verifyVDS(vds as VDS, Data.cscaCertData, Data.cscaCertSHA256Hash, Data.crlData)
            fail()
        } catch (e: VDSVerifyException) {
            assertEquals(e.error, VDSVerifyError.VERIFY_SIGNATURE_ERROR)
        }
    }

    @Test
    fun testNonVDS() {
        // Decode VDS
        val vdsReader = VDSReader()

        try {
            vdsReader.decodeVDSFromJsonString(Data.nonVDSJson)
            fail()
        } catch (e: VDSDecodeException) {
            assertEquals(e.error, VDSDecodeError.JSON_DECODING_ERROR)
        }
    }

    @Test
    fun testUVCIRange(){
        // Decode VDS
        val vdsReader = VDSReader()
        val vds = try { vdsReader.decodeVDSFromJsonString(Data.validVDSJson) } catch (_: Exception) { null }
        assertNotNull(vds)

        var range = vds?.UVCIRange()
        if(range != null){

        }

    }

    @Test
    fun checkUVCISpecimen(){
        try{
            val vds = UVCIChecker.checkRange("VB0009990025")
            assert(vds == UVCIRange.Specimen)
        }catch(exception: java.lang.Exception){
            fail()
        }

    }

    @Test
    fun checkUVCISpecimen2(){
        try{
            val vds = UVCIChecker.checkRange("VB0009990012")
            assert(vds == UVCIRange.Specimen)
        }catch(exception: java.lang.Exception){
            fail()
        }

    }

    @Test
    fun checkUVCITest(){
        try{
            val vds = UVCIChecker.checkRange("VB0000003498")
            assert(vds == UVCIRange.Test)
        }catch(exception: java.lang.Exception){
            fail()
        }

    }
    @Test
    fun testCertificateRepositoryAndNetworkConnection()
    {
        val certRepo = CertificateRepository.instance



    }

}