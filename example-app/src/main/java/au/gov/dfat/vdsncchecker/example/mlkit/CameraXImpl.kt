package au.gov.dfat.vdsncchecker.example.mlkit

import android.app.Activity
import android.graphics.Rect
import android.util.DisplayMetrics
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import au.gov.dfat.lib.vdsncchecker.barcode.IBarcodeListener
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraXImp(private val context: Activity, private val barcodeListener: IBarcodeListener, private val cropRect: Rect?) {

    constructor(context: Activity, barcodeListener: IBarcodeListener): this(context, barcodeListener, null)

    private lateinit var previewView: PreviewView

    private lateinit var camera: Camera

    private val cameraExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    private val imageAnalysis by lazy {
        var metrics = DisplayMetrics()
        context.windowManager.defaultDisplay.getMetrics(metrics)
        ImageAnalysis.Builder()
            .setTargetResolution(Size(metrics.widthPixels, metrics.heightPixels))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, BarcodeAnalyzer(barcodeListener, cropRect))
            }

    }

    fun getCameraView(): PreviewView {
        return previewView
    }

    fun changeTorchState(state: Boolean){
        if(camera?.cameraInfo.hasFlashUnit()){
            camera.cameraControl.enableTorch(state)
        }
    }

    fun changeZoomState(state: Boolean){
        var zoomAmount = if(state) 2f else 1f
        camera.cameraControl.setZoomRatio(zoomAmount)
    }

    init {
        try{
            previewView  = PreviewView(context)

            cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener( {
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)

                    }
                try{
                    cameraProviderFuture.get().let {
                        it.unbindAll()
                        camera = it.bindToLifecycle(context as LifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis )

                    }
                }
                catch(exception: Exception){

                }

            }, ContextCompat.getMainExecutor(context))
        }
        catch(exception: Exception){

        }


    }

}