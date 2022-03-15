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

import android.os.Parcelable
import androidx.annotation.Keep
import au.gov.dfat.lib.vdsncchecker.repository.CRLStoreWrapper
import au.gov.dfat.lib.vdsncchecker.utils.IVdsMsg
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.*


@Keep
@Serializable
data class VDS(
    /** Data. The actual data for the VDS, including version, person info, vaccination info, etc. */
    val data: VDSData,
    /** Signature. The cryptographic signature used to verify the authenticity of the data. */
    val sig: VDSSig,
    /** Original JSON. Not part of VDS spec, used by VDSReader internally. */
    var originalJson: String = ""
)

@Keep
@Serializable
@Parcelize
data class VDSData (
    /** Header. Includes type of data, version and issuing country. */
    val hdr: VDSHdr,
    /** Message. Includes person and vaccination info. */
    val msg: VDSMsg
) : Parcelable

@Keep
@Serializable
@Parcelize
data class VDSHdr (
    /** Version. Required. */
    val v: Long,
    /** Type of data. Can be either `icao.test` or `icao.vacc`. Other types possible in the future. Required. */
    val t: String,
    @SerialName("is")
    /** Issuing country. In 3 letter country code format (e.g. `AUS`). Required. */
    val hdrIs: String
) : Parcelable

@Keep
@Serializable
@Parcelize
data class VDSMsg (
    /** Unique vaccination certificate identifier. Required. */
    override val uvci: String,
    /** Person identification info. Required. */
    val pid: VDSPID,
    /** Array of vaccination events. Required. */
    val ve: List<VDSVe>
) : Parcelable, IVdsMsg

@Keep
@Serializable
@Parcelize
data class VDSPID (
    /** Date of birth. In `yyyy-MM-dd` format. Required if `i` (travel document number) is not provided. */
    val dob: String? = null,
    /** Name. A double space separates first and last name (e.g .`JANE  CITIZEN`). May be truncated. Required. */
    val n: String,
    /** Sex. `M` for male, `F` for female or `X` for unspecified. */
    val sex: String? = null,
    /** Unique travel document number. */
    val i: String? = null,
    /** Additional identifier at discretion of issuer. */
    val ai: String? = null
) : Parcelable

@Keep
@Serializable
@Parcelize
data class VDSVe (
    /** Vaccine type/subtype. Required. */
    val des: String,
    /** Brand name. Required. */
    val nam: String,
    /** Disease targeted by vaccine. Required. */
    val dis: String,
    /** Array of vaccination details. Required. */
    val vd: List<VDSVd>
) : Parcelable

@Keep
@Serializable
@Parcelize
data class VDSVd (
    /** Date of vaccination. In `yyyy-MM-dd` format. Required. */
    val dvc: String,
    /** Dose sequence number. Required. */
    val seq: Long,
    /** Country of vaccination. In 3 letter country code format (e.g. `AUS`). Required. */
    val ctr: String,
    /** Administering center. Required. */
    val adm: String,
    /** Vaccine lot number. Required. */
    val lot: String,
    /** Date of next vaccination. In `yyyy-MM-dd` format. */
    val dvn: String? = null
) : Parcelable

@Keep
@Serializable
data class VDSSig (
    /** Crypto algorithm used for the signature. Can be either `ES256`, `ES384` or `ES512` (typically `ES256`). Required. */
    val alg: String,
    /** Certificate used for the signature. In Base64 URL encoding (not the same as normal Base64!). Required. */
    val cer: String,
    /** Signature value. In Base64 URL encoding (not the same as normal Base64!). Required. */
    val sigvl: String
)

@Keep
data class CertificateData(
    val hash: String,
    val certificate: ByteArray,
    var crl: CRLStoreWrapper?,
    var issuingCountry: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CertificateData

        if (hash != other.hash) return false
        if (!certificate.contentEquals(other.certificate)) return false
        if (crl != other.crl) return false
        if(issuingCountry != other.issuingCountry) return false

        return true
    }

    override fun hashCode(): Int {
        var result = hash.hashCode()
        result = 31 * result + certificate.contentHashCode()
        result = 31 * result + (crl?.hashCode() ?: 0)
        result = 31 * result + (issuingCountry?.hashCode() ?: 0)
        return result
    }


}


@Keep
data class CSCAData(
    val hash: String,
    val certificate: ByteArray,
    val crl: ByteArray?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CSCAData

        if (hash != other.hash) return false
        if (!certificate.contentEquals(other.certificate)) return false
        if (!crl.contentEquals(other.crl)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = hash.hashCode()
        result = 31 * result + certificate.contentHashCode()
        result = 31 * result + crl.contentHashCode()
        return result
    }
}

