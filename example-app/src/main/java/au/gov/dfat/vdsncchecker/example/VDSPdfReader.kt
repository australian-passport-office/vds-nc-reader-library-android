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

import android.app.Activity
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import java.io.File
import android.graphics.pdf.PdfRenderer
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.lang.AssertionError
import java.util.*


// Request code for selecting a PDF document. Older API calls
const val PICK_PDF_FILE = 2

/**
 *
 * VDSPdfReader class for reading pdf files containing VDS codes
 *
 */
open class VDSPdfReader<ExpectedProcessType>(private val context: Activity, private val reader: IBarcodeReaderWrapper<ExpectedProcessType>) {


    /**
     *
     * Open PDF file picker with optional starting Uri
     * @param pickerInitialUri Uri - starting Uri
     *
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun openPdfFile(pickerInitialUri: Uri) {
        val intent = createPdfLoadIntent()

        // optional starting Uri
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)

        launchPdfActivityForResult(intent)

    }

    /**
     *
     *  Overload method for openPdfFile that does not require API 26
     *  Require API code set to minimum project required
     *
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun openPdfFile() {
        val intent = createPdfLoadIntent()
        launchPdfActivityForResult(intent)

    }

    /**
     *
     * Creates PDF file picker intent
     * @return Intent - the file picker intent
     *
     */
    private fun createPdfLoadIntent(): Intent {
        return Intent(Intent.ACTION_GET_CONTENT).apply {

            // only allow pdf to be chosen
            type = "application/pdf"

        }
    }

    /**
     *
     *  Launch PDF file picker intent
     *  @param intent Intent - intent to launch
     *
     **/
    private fun launchPdfActivityForResult(intent: Intent) {
        // optional using latest APIs via interface
        if (context is IResultLauncher) {
            (context as IResultLauncher).resultLauncher.launch(intent)
        } else {
            context.startActivityForResult(intent, PICK_PDF_FILE)
        }
    }

    /**
     *  Processes target PDF file and returns task with any barcodes found
     *  @param data Intent - PDF file picker intent with Uri
     *  @throws VDSPdfReadException if image cannot be processed
     **/
    @Throws(VDSPdfReadException::class)
    open fun processPdfResult(data: Intent?) {
        if (data != null && data.data != null) {
            val image = processPdf(data.data as Uri)

            reader.processPdf(image)

        } else{
            throw VDSPdfReadException(VDSPdfReadError.PROCESS_PDF_ERROR)
        }
    }

    /**
     *  Generate bitmap from pdf
     *  @param uri Uri - Uri to Pdf file
     *  @return InputImage - image format for usage in MLKit Barcode Scanner
     *  @throws VDSPdfReadException if pdf cannot be rendered to an image
     **/
    @Throws(VDSPdfReadException::class)
    private fun processPdf(uri: Uri): Bitmap {

        try{
            return renderPdfToImage(uri)
        }
        catch(exception: Exception){
            throw VDSPdfReadException(VDSPdfReadError.PROCESS_PDF_ERROR)
        }

    }

    /**
     *  Temporary file of PDF to bitmap
     *  @param uri Uri - Uri to PDF file
     *  @return Bitmap? - converted PDF to bitmap.
     *  @throws VDSPdfReadException if bitmap cannot be created from PDF
     **/
    private fun renderPdfToImage(uri: Uri): Bitmap {
        try {
            // create temporary file and create image to scan
            createTemporaryFile(uri).let { outputPdf ->
                assert(outputPdf != null)

                val bitmap = createBitmapFromPdf(outputPdf!!)
                //delete temp file
                outputPdf.delete()

                return bitmap

            }
        } catch (exception: Exception) {
            throw VDSPdfReadException(VDSPdfReadError.PROCESS_PDF_ERROR)
        }

    }

    /**
     *  Temporary file of PDF
     *  @param uri Uri - Uri to PDF file
     *  @return outputPDF - temporary file from pdf to use in rendering
     *  @Throws AssertionError - if an input stream cannot be created
     *  @Throws IOException - when error writing to temp file
     **/
    @Throws(AssertionError::class, IOException::class)
    private fun createTemporaryFile(uri: Uri): File? {
        // get file cache directory for temp file
        val tempDir = context.cacheDir

        // create an output for the temp file
        val outputName = UUID.randomUUID().toString()
        val outputPDF = File.createTempFile(outputName, ".pdf", tempDir)

        // get input stream for the selected file
        context.contentResolver.openInputStream(uri).use { input ->
            assert(input != null)
            // read file into bytes
            val bytes = input!!.readBytes()
            input.close()

            FileOutputStream(outputPDF).use { outStream ->
                outStream.write(bytes)
                outStream.flush()
                outStream.close()
            }
        }

        return outputPDF
    }

    /**
     *  Render PDF to a bitmap
     *  @param outputPDF File - temporary PDF file to render
     *  @return Bitmap - rendered PDF in bitmap format to use in barcode scanner
     *  @Throws FileNotFoundException - if temp file cannot be read
     *  @Throws IOException - if PDF renderer cannot read PDF
     **/
    @Throws(FileNotFoundException::class, IOException::class)
    private fun createBitmapFromPdf(outputPDF: File): Bitmap {
        val pdf = File(outputPDF.absolutePath)
        val pfd = ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY)

        // safest resolutions. Any less and MLKit couldn't read one of the test codes
        val width = 1240 * 2
        val height = 1754 * 2

        // instantiate renderer
        val renderer = PdfRenderer(pfd)

        val page = renderer.openPage(0)
        var bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // check alpha. If alpha exists, white out alpha, otherwise may have odd black background artifacts
        // for example, MLKit will not read anything if the alpha exists
        if (bitmap.hasAlpha()) {
            val canvasBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(canvasBitmap)
            canvas.drawColor(Color.WHITE)
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            bitmap = canvasBitmap
        }

        // render the page
        page.render(
            bitmap,
            Rect(0, 0, width, height),
            null,
            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
        )

        // close everything
        page.close()
        renderer.close()

        return bitmap
    }


}

enum class VDSPdfReadError{
    PROCESS_PDF_ERROR
}

class VDSPdfReadException(var error: VDSPdfReadError): Exception(){

}