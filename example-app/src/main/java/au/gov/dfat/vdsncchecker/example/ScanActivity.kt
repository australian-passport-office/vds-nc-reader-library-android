package au.gov.dfat.vdsncchecker.example

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.RequiresApi
import au.gov.dfat.lib.vdsncchecker.data.ScanButtons
import au.gov.dfat.lib.vdsncchecker.ui.ScanSettingsActivity
import au.gov.dfat.lib.vdsncchecker.utils.showAlertDialog
import au.gov.dfat.vdsncchecker.example.mlkit.CameraXImp

/**
 *
 * override the library Scan Activity
 *
 */
@RequiresApi(Build.VERSION_CODES.P)
class ScanActivity : au.gov.dfat.lib.vdsncchecker.ui.ScanActivity() {

    private lateinit var camera: CameraXImp
    private lateinit var topMenu: Menu



    /**
     *
     * add an optional menu item to access scan settings from the scan screen
     *
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.scan_menu, menu)
        if(menu != null){
            topMenu = menu
        }
        return true
    }

    /**
     *
     * scan settings activity
     *
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.scan_action -> {
                startActivity(Intent(this, ScanSettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     *
     * override get camera view to return implementation of CameraX preview
     *
     * @return view - the view the camera will use
     *
     */
    override fun getCameraView(): View {
        camera = CameraXImp(this, this, barcodeFrameGraphic.box)
        return camera.getCameraView()
    }



    /**
     *
     * override of onBarcodeFound to process barcode
     *
     * check processing and set processing to true to not process multiple reads
     *
     * @param data - string from barcode
     *
     */
    override fun onBarcodeFound(data: String) {
        if(!processingScan){
            processingScan = true
            processVds(data)
        }
    }

    override fun onError(error: String?) {
        TODO("Not yet implemented")
    }

    /**
     *
     * when complete, set processing to false
     *
     */
    override fun onComplete() {
        processingScan = false
    }

    /**
     *
     * check button clicked and update state of camera/phone
     *
     * @param button - enum button identifier
     * @param state - boolean active/not active
     *
     */
    override fun buttonClicked(button: ScanButtons, state: Boolean) {
        when (button){
            ScanButtons.Torch -> camera.changeTorchState(state)
            ScanButtons.Zoom -> camera.changeZoomState(state)
        }
    }



    /**
     *
     * process VDS pulled from barcode
     *
     * @param data - the VDS data
     *
     */
    private fun processVds(data: String) {
        // Decode VDS
        val processor = VdsProcessor()
        val vds = processor.parseVdsData(data)

        if (vds != null) {

            // Verify VDS
            if (processor.authenticateVdsData(vds)) {
                val intent = Intent(this, DetailActivity::class.java)
                val bundle = Bundle().apply {
                    putParcelable(DetailActivity.VDS_DATA_KEY, vds.data)
                }
                intent.putExtras(bundle)
                startActivity(intent)

            } else {
                showAlertDialog( R.string.invalidVdsTitle, R.string.invalidVdsDescription ){
                    processingScan = false
                }
            }
        } else {
            showAlertDialog(R.string.notVdsTitle, R.string.notVdsDescription){
                processingScan = false
            }
        }
    }
}