<style>
    summary{
        color: #03fcf8;
        
    }
    summary:hover{
        cursor: pointer;
    }
    details{
        margin-left:20px;
    }
    
</style>

# VDS-NC Checker Library for Android

To better inform your usage of this library, please see [here](https://www.passports.gov.au/vds-nc-checker) for the business case for the original VDS-NC Checker.

This library is intended for use by entities and organisations that need to validate [Visible Digital Seals](https://www.icao.int/Security/FAL/TRIP/PublishingImages/Pages/Publications/Visible%20Digital%20Seal%20for%20non-constrained%20environments%20%28VDS-NC%29.pdf), typically employed in passports and other identifying/travel documentation.

## Installation

You may add the library via your build.gradle file; firstly add [TBC] to your repository list:
```gradle
allprojects {
    repositories {
        maven { url "[TBC]" }
    }
}
```
and then add the dependency:

```gradle
dependencies {
    implementation 'au.gov.dfat.lib.vdsncchecker:{ReleaseNumber}'
}
```

Alternatively, you may download the source code directly from this repository and modify it to fit your purpose, as per the [Apache License](http://www.apache.org/licenses/LICENSE-2.0)

## Expected Workflow

Features which are up to the caller to implement are surrounded in {braces}.

### 1 - Set up the certificate repository
```Kotlin

var cscaCertificate = {loadByteArrayCSCACertificate()}
var initialCrlData = {loadCRLData()}

var certRepo = CertificateRepository.instance

certRepo.setContext(applicationContext)
certRepo.addCertificate(cscaCertificate, generateHash(cscaCertificate), CRLStoreWrapper(null, initialCrlData))
// Optionally add further certificates depending on the number of regions/countries you intend to support

// By default update CRLs once per day
certRepo.startAutoUpdatingCRLData()           
```

### Step 2 - obtain VDS information 
```Kotlin
// extend ScanActivity, which implements IBarcodeListener
// override getCameraView function and return camera view for camera preview
// implement onBarcodeFound to process data returned from camera/barcode implementations
class ScanActivity: au.gov.dfat.lib.vdsncchecker.ui.ScanActivity {

    override fun onBarcodeFound(data: String) {
        if(!processingScan){
            processingScan = true
            processVds(data)
        }
    }

    override fun getCameraView(): View {
        camera = CameraImplementation()
        return camera.getCameraView()
    }

}

```

### Step 3 - process and interpret the VDS payload
```Kotlin
var data = {getVDSStringFromCamera()}
val vdsReader = VDSReader()
val vds = vdsReader.decodeVDSFromJsonString(data)
var authenticator = VDSAuthenticator()
val isVdsValid =
            try {
                authenticator.verifyVDS(
                    vds,
                    CertificateRepository.instance
                )
            } catch (vdsException: VDSVerifyException) {
                /** Check the error property of vdsException to determine the exact type of failure, and proceed according to your own business rules **/
                return false
            }
if(!isVdsValid)
    {handleFailure()}

```



# Glossary

**BSC** - Barcode signing certificate. This is the certificate used by issuing authorities to sign the barcode data in a VDS to ensure it's authentic.

**CRL** - Certificate Revocation List. A certificate may be revoked by its issuing authority in the instance of the certificates private key becoming compromised, as which point any data signed with said certificate can no longer be trusted. Further information [here](https://en.wikipedia.org/wiki/Certificate_revocation_list).

**CSCA** - Country Signing Certificate Authority. ​Each State issuing an ePassport establishes a single Country Signing Certification Authority (CSCA) as its national trust point in the context of ePassports. Further information [here](https://www.icao.int/Security/FAL/PKD/BVRT/Pages/CSCA.aspx).

**VDS** - Visible Digital Seal. This is the visible, scannable code on a document which can be machine read to verify the integrity of the document. Full specification [here](https://www.icao.int/Security/FAL/TRIP/Documents/TR%20-%20Visible%20Digital%20Seals%20for%20Non-Electronic%20Documents%20V1.31.pdf).

# API Definition

## VDSReader
This class contains a single helper method to assist with converting VDS data read from a camera into a reusable object.

### **decodeVDSFromJsonString(jsonString: String): VDS**
Decodes VDS data from a VDS JSON string.

This helper method translates the raw JSON read from a VDS image into a usable strongly typed object for reuse throughout the library. 

## VDSDecodeException
This is the exception type thrown if invalid JSON is passed to decodeVDSFromJsonString. The *error* property will contain further information regarding the cause of the failure, if available.

## VDSVerifyException
This is the exception type thrown if any part of validating a VDS fails. The *error* property will contain further information regarding the cause of the failure, if available.

## VDSAuthenticator
*implements IVDSAuthenticator*

This class contains numerous helper methods to perform the bulk of the work of verifying a VDS using information issued by varying certificate authorities.

*Note, all member functions of the VDSAuthenticator class can be overridden by an inheriting class if you wish to customise the validation logic/flow.*

### **verifyVDS(vds: VDS, cscaCertData: ByteArray, cscaCertSHA256Hash: String, crlData: Collection<ByteArray>): Boolean**
Validates a VDS object (generated via a call to VDSReader.decodeVDSFromJsonString), returning true if it is authentic and throwing an exception otherwise. The exception will contain further details regarding the reason for failure.

In order to validate the VDS object, this function calls the following methods in order:
- verifyCSCACertHash
- verifyCRLSignatureUsingCSCACertPublicKey
- verifyBSCCertNotRevokedInCRL
- verifyBSCCertAKIMatchesCSCACertAKI
- verifyBSCCertIncludesCSCACertInCertPath
- verifyVDSSignature

The parameters to this method are:
- vds - a VDS object generated via the VDSReader.decodeVDSFromJsonString method
- cscaCertData - A byte array containing the certificate information of the authority of the VDS
- cscaCertSHA256Hash - A hash of the cscaCertData parameter, to verify the integrity of the certifcate. It is expected that the caller has a hard-coded hash of the certificate available for maximum security; otherwise the caller may dynamically calculate the hash as required.
- crlData - A array of data representing a collection of Certificate Revocation Lists against which the cscaCertData is to be checked

### **verifyVDS(vds: VDS, certificateRepository: ICertificateRepository): Boolean**
Validates a VDS object, assuming a properly populated ICertificateRepository is provided. Internally, this method calls the other verifyVDS using information retrieved from the ICertificateRepository.

### **verifyVDS(vds: VDS, cscaCertData: ByteArray, cscaCertSHA256Hash: String,crlData: ByteArray): Boolean**
A convenience method to validate a VDS object as per the other verifyVDS variants, but with a single Certificate Revocation List object.

### **verifyCSCACertHash(cscaCertData: ByteArray, cscaCertSHA256Hash: String)**
Verifies that our CSCA certificate has not been tampered with by checking if its hash matches a known trusted hash.
Throws an exception if the hash does not match the trusted hash.

### **verifyCRLSignatureUsingCSCACertPublicKey(crlData: Collection<ByteArray>, cscaCertData: ByteArray)**
Verifies a CRL's signature using a CSCA public key.
Throws an exception if any element in the CRL list cannot be verified.

### **verifyBSCCertNotRevokedInCRL(vds: VDS, crlData: Collection<ByteArray>)**
Verifies a given BSC certificate is not revoked in a given CRL. Throws an exception if the certificate is proven to be revoked.

### **verifyBSCCertAKIMatchesCSCACertAKI(vds: VDS, cscaCertData: ByteArray)**
Verifies a BSC certificates AKI (Authority Key Identifier) matches a CSCA certificate's AKI. Throws an exception if the match fails.

### **verifyBSCCertIncludesCSCACertInCertPath(vds: VDS, cscaCertData: ByteArray)**
Verifies a BSC certificate includes a given CSCA certificate in its certification path. Throws an exception if the CSCA certificate is not present.

### **verifyVDSSignature(vds: VDS)**
Verifies a VDS's signature.

### **getAlgorithmFromVDS(vds: VDS): String?**
Gets the algorithm for the given VDS. From the VDS spec:
The SignatureAlgo field MUST be only one of the following values:
- ES256 – denotes ECDSA with Sha256 hashing algorithm
- ES384 – denotes ECDSA with Sha384 hashing algorithm
- ES512 – denotes ECDSA with Sha512 hashing algorithm

The algorithm returned by this function can be passed into [java.security.Signature.getInstance()].

## CertificateRepository

The CertificateRepository class contains facilities to store certificates and certificate revokation lists long-term in a secure manner, along with helper functions to assist performing automatic CRL updates.

*Note, all member functions of the CertificateRepository class can be overridden by an inheriting class if you wish to customise the logic/flow.*

### **addCertificate(certificate: ByteArray, hash: String, crl: CRLStoreWrapper)**
Adds a CSCA certificate to the store. The common name is extracted from the certificate and used as the lookup key. This function constructs a CertificateData object from the parameters provided for long term storage.
- certificate - The certificate files binary contents
- hash - The SHA256 hash of the certificate contents
- crl - The certificate revokation list (CRLStoreWrapper) item to be associated with this certificate

### **addCertificate(certificate: CertificateData)**
Adds a CSCA certificate to the store. The caller is expected to have properly constructed the CertificateData object before hand.

### **findIssuer(certificate: ByteArray, certFactory: CertificateFactory): CertificateData?**
Given certificate data from a VDS, this function searches the certificate repository for a matching CSCA certificate and returns it if found. Returns null if not found, and throws a CertificateException if any of the certificates involved are corrupted.

### **setContext(app: Context)**
Gives the CertificateRepository access to the application context, in order to configure long term storage. This method must be called before using any other repository features.

### **isUpdateOverdue() : Boolean**
Inspects the CRL list and checks if any entries are older than the configured *maxSecondsBeforeOverdue* age in seconds. Returns true if an update is required/expected.

### **setupCRLs(data: Array<CRLStoreWrapper>)**
Adds the given array of CRLStoreWrapper objects to the stored list of CRLs. Also searches the CSCA list for any matching items based on the CRL URL and updates the CRL reference if a match is found.

### **startAutoUpdatingCRLData(p_secondsBetweenUpdates : Int? = null)**
Begin a background process which updates the CRL list from the sources provided on a periodic basis. Default update interval is 1 day/86400 seconds, and can be overriden via the p_secondsBetweenUpdates parameter.

When complete, all subscribers to the CRLManagerUpdateEvent event will be notified.

### **stopAutoUpdatingCRLData()**
Cancels the auto update timer if it is running.

### **updateCRLData(complete: ((count: Int) -> Unit)? = null)**
Manually initiate a refresh of CRL data based on the URLs provided in the CRLStoreWrapper list. This method is also called internally by the timer function started by *startAutoUpdatingCRLData*. 

Upon completion of the update, the callback parameter will be called if provided (with the number of records updated as the count), and the *CRLManagerUpdateEvent* event will be rasied.

## CertificateRepository.CertificateRepositoryDelegate
Interface for implementing listeners for CRL updates

### **didUpdateCRLData(size: Int?)**
Called when CRLs are updated with amount of CRLs updated

## CRLStoreWrapper
The CRLStoreWrapper class represents a CRL file and that same files source location/URL. This bundling/coupling enables certain library features such as automatic updates and easy retreival from the long term local store.

### **init(url: URL?, data: ByteArray)**
Initialise a CRLStoreWrapper in one of a variety of different modes, depending on the parameters given and the current state of the CertificateRepository. Once constructed, instances of CRLStoreWrapper automatically add themselves to the CertificateRepository.

- url and data both provided - If both parameters are provided, the system will first check the repository for a CRLStoreWrapper object matching the URL. If found, it will preference using the stored data; otherwise it will use the data from the constructor. This allows the caller to specific a "default" CRL list which will be ignored if a more up-to-date list is already in the store.
- url not provided, but data provided - This option will disable automatic updates and long term storage, as without a URL there is no key with which to store the item in the repository. The object can then be used for transient/temporary purposes and operations.
- url provided, but data not provided - This option will cause the repository to be checked for an entry with a matching URL. If found, the current data from the repository will be copied into the object. If not found, the internal data/byte array will be left blank until *download* is called, whether that occur manually or via an automatic timed updated.
- Neither parameter provided - Invalid state.

### **download() : Boolean**
If no URL is set on the object, this method will immediately return a **true** result.

Otherwise, the system will grab the content present at the URL as a byte array and store it on the object and in the repository, then return **true**.

Any failure to complete the download will return a **false** result.

## ScanActivity 
*abstract*
*implements IBarcodeListener*
*requires camera permissions*

Abstract activity for scanning VDS codes

### **getCameraView(): View**
*open*
Override this function to inject an implementation of a camera preview. This view covers the entire layout. Buttons and banners overlay this.

### **setTitleFromSettings(viewModel: ScanConfigViewModel)**
*open*

Sets action bar text from preferences. Override to implement own logic

## IBarcodeListener
Interface for listening for barcode implementations

### **onBarcodeFound(data: String)**

Invoke when barcode is scanned

### **onError(error: String?)**

Invoke when a barcode scanning errors

### **onComplete()**

Invoke when barcode scanning complete

## ScanSettingsActivity
*open*

Activity for showing scan related preferences

### **commitSettingsFragment()**
*open*

Commits ScanSettingsFragment. Override to commit own fragment implementation.

## ScanSettingsFragment
*open*

Fragment for managing Scan Settings layout. Contains open functions for configuring extra preferences.

## PreferenceRepository
*abstract*

Abstract repository class for accessing/storing preferences. 

### **<T> storePreference(key: String, value: T)**
*open*

 Open helper function to store preferences based on type

 ### **getPreference(key: String, default: T): T**
 *open*

 Open helper function to get preferences based on type

 ## PreferenceViewModel
 *abstract*

 Abstract viewmodel for handling access to preferences via abstract repository

