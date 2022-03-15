package au.gov.dfat.vdsncchecker.example.mlkit

import android.graphics.Bitmap
import au.gov.dfat.vdsncchecker.example.IBarcodeReaderWrapper
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

class BarcodeReader : IBarcodeReaderWrapper<Barcode> {

    override lateinit var onSuccess: ((MutableList<Barcode>) -> Unit)
    override lateinit var onFailure: () -> Unit
    override lateinit var onComplete: (Boolean) -> Unit

    // barcode formats to scan
    // https://developers.google.com/ml-kit/vision/barcode-scanning/android#1.-configure-the-barcode-scanner
    private val barcodeOptions = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_QR_CODE
        )
        .build()



    override fun processPdf(image: Bitmap) {
        // https://developers.google.com/ml-kit/vision/barcode-scanning/android#3.-get-an-instance-of-barcodescanner
        val scanner = BarcodeScanning.getClient(barcodeOptions)
        var found = false
        // https://developers.google.com/ml-kit/vision/barcode-scanning/android#4.-process-the-image

        // https://developers.google.com/ml-kit/vision/barcode-scanning/android#using-a-bitmap
        var input = InputImage.fromBitmap( image, 0)

        // process the image and fire off events
        scanner.process(input)
            .addOnSuccessListener{ barcodeList ->
                if(barcodeList.count() > 0){
                    found = true
                }
                onSuccess?.invoke(barcodeList)
            }
            .addOnCompleteListener {
                onComplete?.invoke(found)
            // close/release scanner
            scanner.close()
        }

    }
}