package au.gov.dfat.vdsncchecker.example.mlkit

import android.annotation.SuppressLint
import android.graphics.Rect
import android.media.Image
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import au.gov.dfat.lib.vdsncchecker.barcode.IBarcodeListener
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.lang.Exception

class BarcodeAnalyzer(private val barcodeFoundListener: IBarcodeListener, private val cropRect: Rect?)
    : ImageAnalysis.Analyzer {

    constructor(barcodeFoundListener: IBarcodeListener) : this(barcodeFoundListener, null)

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
        .build()

    private val scanner = BarcodeScanning.getClient(options)

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(proxy: ImageProxy) {
        proxy.image?.let{
            if(cropRect != null){
                proxy.setCropRect(cropRect)
            }
            process(it, proxy)

        }

    }

    private fun process(image: Image, imageProxy: ImageProxy){
        try{
            val input = InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)

            scanner.process(input)
                .addOnSuccessListener { barcodes ->
                if(barcodes.count() > 0){

                    val data = barcodes.first().rawValue
                    barcodeFoundListener.onBarcodeFound(data)
                }
            }
                .addOnFailureListener{

                    barcodeFoundListener.onError(it.message)
                }
                .addOnCompleteListener{
                    imageProxy.close()
                }
        }catch(exception: Exception){

        }
    }

}