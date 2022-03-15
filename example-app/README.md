# VDS-NC Example App (Android)

This sample app demonstrates parsing and verifying the authenticity of VDS-NC JSON data as defined in the [VDS-NC Visible Digital Seal for non-constrained environments specification](https://www.icao.int/Security/FAL/TRIP/PublishingImages/Pages/Publications/Visible%20Digital%20Seal%20for%20non-constrained%20environments%20%28VDS-NC%29.pdf)

Builds of the attached library are copied into the libs folder of the example app. So any updates in the library are carried through ( if build from Debug or Release)

## Requirements

- Kotlin 1.5+
- Android Studio Arctic Fox and onwards

# Glossary

**BSC** - Barcode signing certificate. This is the certificate used by issuing authorities to sign the barcode data in a VDS to ensure it's authentic.

**CRL** - Certificate Revocation List. A certificate may be revoked by its issuing authority in the instance of the certificates private key becoming compromised, as which point any data signed with said certificate can no longer be trusted. Further information [here](https://en.wikipedia.org/wiki/Certificate_revocation_list).

**CSCA** - Country Signing Certificate Authority. ​Each State issuing an ePassport establishes a single Country Signing Certification Authority (CSCA) as its national trust point in the context of ePassports. Further information [here](https://www.icao.int/Security/FAL/PKD/BVRT/Pages/CSCA.aspx).

**VDS** - Visible Digital Seal. This is the visible, scannable code on a document which can be machine read to verify the integrity of the document. Full specification [here](https://www.icao.int/Security/FAL/TRIP/Documents/TR%20-%20Visible%20Digital%20Seals%20for%20Non-Electronic%20Documents%20V1.31.pdf).

**UVCI** - Unique vaccination certificate identifier

## Usage

The `au.gov.dfat.vdsncchecker.example.vds` package contains classes for parsing and verifying VDS-NC JSON data.

`MainActivity` demonstrates how to use those classes.

`DetailActivity` demonstrates how to display data parsed from a VDS.

`Data` contains example data (a VDS, CSCA certificate and CRL) that can be used for testing.

`DetailAdapter` adapter for displaying VDS data

`EventAdapter` adapter for displaying vaccine event data

`ScanActivity` extension of library ScanActivity for scanning and reading VDS Codes

`VDS.UVCI.kt` extension methods for VDS class for checking UVCI range

`VDSPdfReader` example of reading VDS codes from PDF files using MLKit

`VdsProcessor` wrapper for library VDSReader and VDSAuthenticator to minimise code duplication

`mlkit` package containing examples of using MLKit for camera previewing and QR code scanning

**Example Usage**

```kotlin
private func readVDSJson(vdsJson: String) {
    // Decode VDS
    let vdsReader = VDSReader()
    let vds = try? vdsReader.decodeVDSFrom(jsonString: vdsJson)
    if let vds = vds {
        // Verify VDS
        if let _ = try? vdsReader.verify(
            vds: vds,
            // can pass CertificateRepository into here if dealing with multiple CSCAs
            cscaCertData: Constants.cscaCertData,
            cscaCertSHA256Hash: Constants.cscaCertSHA256Hash,
            crlData: Constants.crlData
        ) {
            // A valid VDS
            showResultForValidVDS(vds: vds)
        } else {
            // Not an authentic VDS
            showNotAuthenticVDSAlert()
        }
    } else {
        // Not a VDS
        showNotAVDSAlert()
    }
}
```

### API

The parsing code is made up of four classes:

#### VDS

The `VDS` class is a model class that stores the data parsed from VDS-NC JSON data. It has the following properties:

| Property             | Purpose                                                                                                           |
| -------------------- | ----------------------------------------------------------------------------------------------------------------- |
| VDS                  |                                                                                                                   |
| `data`               | Data. The actual data for the VDS, including version, person info, vaccination info, etc.                         |
| `sig`                | Signature. The cryptographic signature used to verify the authenticity of the data.                               |
| Data                 |                                                                                                                   |
| `data.hdr`           | Header. Includes type of data, version and issuing country.                                                       |
| `data.msg`           | Message. Includes person and vaccination info.                                                                    |
| Header               |                                                                                                                   |
| `data.hdr.t`         | Type of data. Can be either `icao.test` or `icao.vacc`. Other types possible in the future. Required.             |
| `data.hdr.v`         | Version. Required.                                                                                                |
| `data.hdr.hdrIs`     | Issuing country. In 3 letter country code format (e.g. `AUS`). Required.                                          |
| Message              |                                                                                                                   |
| `data.msg.uvci`      | Unique vaccination certificate identifier. Required.                                                              |
| `data.msg.pid`       | Person identification info. Required.                                                                             |
| `data.msg.ve`        | Array of vaccination events. Required.                                                                            |
| `data.msg.pid.dob`   | Date of birth. In `yyyy-MM-dd` format. Required if `i` (travel document number) is not provided.                  |
| `data.msg.pid.n`     | Name. A double space separates first and last name (e.g. `JANE CITIZEN`). May be truncated. Required.             |
| `data.msg.pid.sex`   | Sex. `M` for male, `F` for female or `X` for unspecified.                                                         |
| `data.msg.pid.i`     | Unique travel document number.                                                                                    |
| `data.msg.pid.ai`    | Additional identifier at discretion of issuer.                                                                    |
| Vaccination Events   |                                                                                                                   |
| `data.msg.ve.des`    | Vaccine type/subtype. Required.                                                                                   |
| `data.msg.ve.nam`    | Brand name. Required.                                                                                             |
| `data.msg.ve.dis`    | Disease targeted by vaccine. Required.                                                                            |
| `data.msg.ve.vd`     | Array of vaccination details. Required.                                                                           |
| Vaccination Details  |                                                                                                                   |
| `data.msg.ve.vd.dvc` | Date of vaccination. In `yyyy-MM-dd` format. Required.                                                            |
| `data.msg.ve.vd.seq` | Dose sequence number. Required.                                                                                   |
| `data.msg.ve.vd.ctr` | Country of vaccination. In 3 letter country code format (e.g. `AUS`). Required.                                   |
| `data.msg.ve.vd.adm` | Administering center. Required.                                                                                   |
| `data.msg.ve.vd.lot` | Vaccine lot number. Required.                                                                                     |
| `data.msg.ve.vd.dvn` | Vaccine lot number. Required.                                                                                     |
| Signature            |                                                                                                                   |
| `sig.alg`            | Crypto algorithm used for the signature. Can be either `ES256`, `ES384` or `ES512` (typically `ES256`). Required. |
| `sig.cer`            | Certificate used for the signature. In Base64 URL encoding (not the same as normal Base64!). Required.            |
| `sig.sigvl`          | Signature value. In Base64 URL encoding (not the same as normal Base64!). Required.                               |

#### VDSException

The `VDSException` file contains two classes that encapsulate errors:

- `VDSDecodeException` class encapsulates exceptions that can be thrown during the parsing of a VDS.
- `VDSVerifyException` class encapsulates exceptions that can be thrown during the verifying of a VDS.

### VDSReader

The `VDSReader` class is used for parsing a VDS. It has the following functions:

```kotlin
fun decodeVDSFromJsonString(jsonString: String): VDS?
```

Decodes VDS data from a VDS JSON string.

**Parameters**

| Parameter    | Type   | Purpose                  |
| ------------ | ------ | ------------------------ |
| `jsonString` | String | The VDS JSON string etc. |

**Returns**

A `VDS` if decoding is successful, otherwise throws an exception.

**Throws**

`VDSDecodeException` if decoding fails.

### VDSAuthenticator

The `VDSAuthenticator` class is used for verifying a VDS. It has the following functions:

```kotlin
fun verifyVDS(
    vds: VDS, 
    certificateRepository: ICertificateRepository
    ): Boolean

fun verifyVDS(
    vds: VDS,
    cscaCertData: ByteArray,
    cscaCertSHA256Hash: String?,
    crlData: ByteArray?
): Boolean
```

Verifies a `VDS`, returning true if it is authentic, otherwise throws an error.

**Parameters**

| Parameter              | Type                     | Purpose                                                                                                     |
| ---------------------- | ------------------------ | ----------------------------------------------------------------------------------------------------------- |
| `vds`                  | `VDS`                    | A `VDS`.                                                                                                    |
|------------------------|--------------------------|-------------------------------------------------------------------------------------------------------------|
| `certificateRepository`| `ICertificateRepository` | Repository that manages CSCAs and CRLs                                                                      |
|------------------------|--------------------------|-------------------------------------------------------------------------------------------------------------|
| `cscaCertData`         | `ByteArray`              | CSCA certificate data (in DER format).                                                                      |
| `cscaCertSHA256Hash`   | `String`                 | SHA256 hash of CSCA certificate data - used to ensure the CSCA certificate data has not been tampered with. |
| `crlData`              | `ByteArray`              | CRL data (in DER format) - used for CRL verification.                                                       |

**Returns**

True if the `VDS` is authentic, otherwise throws an error.

**Throws**

`VDSVerifyException` if verification fails.



## Acknowledgements

This sample uses the following open-source libraries:

- [Bouncy Castle](https://www.bouncycastle.org/licence.html)
- [Kotlin multiplatform/multi-format reflectionless serialization](https://github.com/Kotlin/kotlinx.serialization/blob/master/LICENSE.txt)
- [java-json-canonicalization](https://github.com/erdtman/java-json-canonicalization/blob/master/LICENSE)
- [Material design icons](https://github.com/google/material-design-icons/blob/master/LICENSE)

Please refer to LICENSE and NOTICE in the root of this repository for further license and copyright information.
