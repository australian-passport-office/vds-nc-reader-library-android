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

package au.gov.dfat.lib.vdsncchecker.ui

import au.gov.dfat.lib.vdsncchecker.barcode.BarcodeFrameGraphic
import au.gov.dfat.lib.vdsncchecker.barcode.IBarcodeListener
import au.gov.dfat.lib.vdsncchecker.camera.CameraWrapper
import au.gov.dfat.lib.vdsncchecker.camera.GraphicOverlay
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.doOnLayout
import au.gov.dfat.lib.vdsncchecker.R
import au.gov.dfat.lib.vdsncchecker.config.Constants
import au.gov.dfat.lib.vdsncchecker.controls.showAlertDialog
import au.gov.dfat.lib.vdsncchecker.data.ScanButtons
import au.gov.dfat.lib.vdsncchecker.models.ScanConfigViewModel
import au.gov.dfat.lib.vdsncchecker.models.ScanConfigViewModelFactory
import au.gov.dfat.lib.vdsncchecker.repository.CertificateRepository
import au.gov.dfat.lib.vdsncchecker.utils.*
import java.util.*


/**
 *
 * abstract Scan activity for scanning VDS codes
 *
 * extend to implement camera and barcode scanning logic
 *
 */

abstract class ScanActivity : BaseActivity(), IBarcodeListener {

    private val loggerTag: String = "logger-ScanActivity"

    private lateinit var graphicOverlay: GraphicOverlay

    protected open var showAllWarnings: Boolean = false
     // banner showing network/crl warnings
    private lateinit var warningBanner: TextView
    // banner down bottom ( align QR Code )
    private lateinit var instructionsBanner: TextView
    // popup when code is invalid
    protected open lateinit var invalidCode: TextView

    // buttons
    protected open lateinit var torchButton: ImageView
    protected open lateinit var zoomButton: ImageView

    // whether or not barcode frame graphic has been added
    private var frameAdded: Boolean = false
    private lateinit var parentLayout: ConstraintLayout

    // wrapper view to contain camera preview
    protected open lateinit var wrapper: CameraWrapper

    lateinit var barcodeFrameGraphic: BarcodeFrameGraphic

    // any warnings to show ( network connectivity, etc )
    private val warnings: MutableMap<Int, String> = mutableMapOf()

    protected open val buttonStateMap: MutableMap<Int, Boolean> =
        mutableMapOf(Pair(R.id.zoom_button, false), Pair(R.id.torch_button, false))

    // whether a code is currently scanned and being processed
    // can be used to halt any other processing, etc
    protected var processingScan: Boolean = false

    var logger: Logger? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        processingScan = false
        buttonStateMap.forEach {
            buttonStateMap[it.key] = false
        }

        checkPreferencesAndUpdateLayout()
    }

    /**
     *
     * camera permission popup
     *
     */
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions.entries.all {
                it.value == true
            }

            if (granted) {
                initialiseCamera()
            } else {
                // Let the user know we need camera access
                showAlertDialog(
                    this,
                    R.string.cameraPermissionsTitle,
                    R.string.cameraPermissionsText,
                    R.string.appInfoButton,
                    true,
                    okCallback = { _, _ ->
                        startActivity(createApplicationSettingsIntent(this))
                        initialiseCamera()
                    }
                )
            }
        }

    /**
     *
     * called from crl update delegate
     *
     */
    @RequiresApi(Build.VERSION_CODES.O)
    protected open fun onDidUpdateCrl(updatedCount: Int? = null) {
        checkCRLStatus()
    }

    /**
     *
     * update warning banner text if conditions are met
     *
     */
    @RequiresApi(Build.VERSION_CODES.N)
    private fun updateWarningText() {
        runOnUiThread {
            var warningText: String = ""

            if(showAllWarnings){
                warnings.forEach { (_, s) ->
                    warningText += s + '\n'
                }
                warningText.trimEnd('\n')
            } else{
                // take connection warning before anything else
                warningText = if(warnings.containsKey(R.string.connectToInternetWarning)){
                    warnings[R.string.connectToInternetWarning]!!
                }else{
                    warnings.values.firstOrNull() ?: ""
                }
            }

            warningBanner.text = warningText

            if (warnings.count() > 0) {
                warningBanner.visibility = View.VISIBLE
            } else {
                warningBanner.visibility = View.GONE
            }
        }

    }


    /**
     *
     * Called when the state of the network changes
     *
     * Possible states are Connected ( fine ), Dropped ( just lost ), Lost ( lost for at least a minute )
     *
     * @param state - the currently detected state of the network
     *
     */
    @RequiresApi(Build.VERSION_CODES.N)
    protected open fun onNetworkStateChanged(state: NetworkState) {

        if (state == NetworkState.Lost) {
            addNetworkWarning()
        }

        if (state == NetworkState.Connected) {
            removeNetworkWarning()
        }

    }

    /**
     *
     * add a warning regarding network connection
     *
     */
    @RequiresApi(Build.VERSION_CODES.N)
    private fun addNetworkWarning(){
        logger?.printLine("Adding warning text", loggerTag)
        if (!warnings.containsKey(R.string.connectToInternetWarning)) {
            warnings[R.string.connectToInternetWarning] = getString(R.string.connectToInternetWarning)
            updateWarningText()
        }
    }

    /**
     *
     * remove a warning regarding network connection
     *
     */
    @RequiresApi(Build.VERSION_CODES.N)
    private fun removeNetworkWarning(){
        if (warnings.containsKey(R.string.connectToInternetWarning)) {
            warnings.remove(R.string.connectToInternetWarning)
            updateWarningText()
        }

    }


    /**
     *
     * certificate repository delegate for crl updates
     * calls onDidUpdateCrl to extend
     *
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private val didUpdateCrl: CertificateRepository.CertificateRepositoryDelegate =
        CertificateRepository.CertificateRepositoryDelegate {
            onDidUpdateCrl(it)
        }

    /**
     *
     * network monitor delegate for change in network state
     * calls onNetworkStateChanged to extend
     *
     */
    @RequiresApi(Build.VERSION_CODES.N)
    private val networkStateChanged: NetworkMonitor.NetworkDelegate =
        NetworkMonitor.NetworkDelegate {
            logger?.printLine("Network state changed to $it", loggerTag)
            onNetworkStateChanged(it)
        }

    /**
     *
     * override of AppCompatActivity.onDestroy
     *
     *
     *
     */
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onDestroy() {

        onRemoveListeners()
        onCancelTimers()

        super.onDestroy()
    }

    /**
     *
     * remove any listeners that have been set
     *
     */
    @RequiresApi(Build.VERSION_CODES.O)
    protected open fun onRemoveListeners(){
        // remove any listeners
        CertificateRepository.instance.removeDidUpdateCRLDataListener(didUpdateCrl)

        // remove network monitor listeners
        var networkMonitor = NetworkMonitor.getInstance(this)
        networkMonitor.removeNetworkListener(networkStateChanged)
    }

    /**
     *
     * cancel any timers that are running
     *
     */
    @RequiresApi(Build.VERSION_CODES.N)
    protected open fun onCancelTimers(){
        //cancel the timer if we destroy the activity
        var networkMonitor = NetworkMonitor.getInstance(this)
        networkMonitor.timer.cancel()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        // setup listeners
        CertificateRepository.instance.setDidUpdateCRLDataListener(didUpdateCrl)

        // set content view
        setContentView(R.layout.activity_scan)

        // get views from layout
        invalidCode = findViewById(R.id.invalidCode)
        warningBanner = findViewById(R.id.warningBanner)
        instructionsBanner = findViewById(R.id.instructions)
        graphicOverlay = findViewById(R.id.graphic_overlay)

        addBarcodeFrameIfNeeded()
        checkPreferencesAndUpdateLayout()

        wrapper = findViewById(R.id.camera_view)

    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onStart() {
        super.onStart()

        processingScan = false
        if (!allPermissionsGranted()) {
            getRuntimePermissions()
        } else {
            initialiseCamera()
            setupNetworkMonitor()
        }
    }

    /**
     *
     * get actual camera preview view
     * @return View - the camera view ( default is a blank view )
     *
     * eg.
     *  camera = CameraXImp(this, this, barcodeFrameGraphic.box)
     *  return camera.getCameraView()
     *
     *  where class CameraXImp(private val context: Activity, private val barcodeListener: BarcodeListener, private val cropRect: Rect?)
     *
     *  camera is an implementation of CameraX
     *  camera.getCameraView() returns a PreviewView
     *
     */
    open fun getCameraView(): View {
        return View(this)
    }

    /**
     *
     * prepare camera
     * if the camera wrapper does not yet have the camera view as a child, attach it
     *
     * onInitialiseCamera called at beginning to allow extension preparation
     *
     */
    private fun initialiseCamera() {
        onInitialiseCamera()

        val cameraView = getCameraView()
        if (!wrapper.children.contains(cameraView)) {
            wrapper.setCameraView(cameraView)
        }
    }

    /**
     *
     * setup the network monitor to keep track of network connectivity
     * shows warnings when connectivity lost for more than a minute
     *
     */
    @RequiresApi(Build.VERSION_CODES.N)
    private fun setupNetworkMonitor() {
        val networkMonitor = NetworkMonitor.getInstance(this)
        networkMonitor.setNetworkListener(networkStateChanged)
        if (!networkMonitor.available) {
            logger?.printLine("no network available?", loggerTag)
            onNetworkStateChanged(NetworkState.Lost)
        } else{
            logger?.printLine("Network is available", loggerTag)

        }
    }

    /**
     *
     * Open function to allow extra initialisation
     *
     */
    protected open fun onInitialiseCamera() {}

    /**
     *
     * sets default button images
     *
     * @param targets - enums representing target buttons to set images for
     *
     */
    protected open fun setDefaultImageButtons(targets: EnumSet<ScanButtons>) {
        if (targets.contains(ScanButtons.Zoom)) {
            zoomButton.setImageResource(R.drawable.ic_zoom_in)
        }
        if (targets.contains(ScanButtons.Torch)) {
            torchButton.setImageResource(R.drawable.ic_torch_off)
        }
    }

    /**
     *
     * gets the settings view model
     *
     * @return - the settings view model
     *
     */
    protected open fun getSettingsViewModel(): ScanConfigViewModel {
        return viewModels<ScanConfigViewModel> {
            ScanConfigViewModelFactory(this)
        }.value
    }

    /**
     *
     * check the settings and update the layout accordingly
     *
     */
    @RequiresApi(Build.VERSION_CODES.O)
    protected open fun checkPreferencesAndUpdateLayout() {


        val configViewModel = getSettingsViewModel()

        runOnUiThread {

            setTextFromSettings(configViewModel)

            parentLayout = findViewById(R.id.parentLayout)
            torchButton = findViewById(R.id.torch_button)
            zoomButton = findViewById(R.id.zoom_button)

            val hasTorch = configViewModel.torchConfig
            val hasZoom = configViewModel.zoomConfig

            // hide these until we know they are usable
            parentLayout.doOnLayout {
                zoomButton.visibility = View.GONE
                torchButton.visibility = View.GONE
            }


            if (hasTorch && hasZoom) {

                parentLayout.doOnLayout {
                    zoomButton.visibility = View.VISIBLE
                    torchButton.visibility = View.VISIBLE
                    setVerticalPosition(R.id.torch_button)
                    setVerticalPosition(R.id.zoom_button)

                    setDefaultImageButtons(EnumSet.of(ScanButtons.Torch, ScanButtons.Zoom))

                    zoomButton.setOnClickListener(onClick)
                    torchButton.setOnClickListener(onClick)


                    ConstraintSet().apply {
                        clone(parentLayout)
                        clear(R.id.torch_button, ConstraintSet.END)
                        connect(
                            R.id.torch_button,
                            ConstraintSet.END,
                            R.id.zoom_button,
                            ConstraintSet.START,
                            0
                        )
                        connect(
                            R.id.torch_button,
                            ConstraintSet.RIGHT,
                            R.id.zoom_button,
                            ConstraintSet.LEFT,
                            0
                        )
                        setHorizontalChainStyle(R.id.torch_button, ConstraintSet.CHAIN_PACKED)

                        clear(R.id.zoom_button, ConstraintSet.START)
                        connect(
                            R.id.zoom_button,
                            ConstraintSet.START,
                            R.id.torch_button,
                            ConstraintSet.END,
                            0
                        )
                        connect(
                            R.id.zoom_button,
                            ConstraintSet.LEFT,
                            R.id.torch_button,
                            ConstraintSet.RIGHT,
                            0
                        )
                        setHorizontalChainStyle(R.id.zoom_button, ConstraintSet.CHAIN_PACKED)

                        applyTo(parentLayout)
                    }
                }

            } else {
                if (hasZoom) {
                    parentLayout.doOnLayout {
                        zoomButton.visibility = View.VISIBLE
                        setDefaultImageButtons(EnumSet.of(ScanButtons.Zoom))
                        setVerticalPosition(R.id.zoom_button)
                        zoomButton.setOnClickListener(onClick)
                        clearHorizontalConstraints(zoomButton.id)


                    }
                }// can only have one or the other

                if (hasTorch) {
                    parentLayout.doOnLayout {
                        torchButton.visibility = View.VISIBLE
                        setDefaultImageButtons(EnumSet.of(ScanButtons.Torch))
                        setVerticalPosition(R.id.torch_button)
                        torchButton.setOnClickListener(onClick)
                        clearHorizontalConstraints(torchButton.id)
                    }
                }
            }

            checkCRLStatus()

            updateWarningText()

        }

    }

    /**
     *
     * clear horizontal constraints for a view.
     *
     * used when updating view based on settings
     *
     * @param id - the view id
     *
     */
    private fun clearHorizontalConstraints(@IdRes id: Int) {
        ConstraintSet().apply {
            clear(id, ConstraintSet.RIGHT)
            clear(id, ConstraintSet.RIGHT)
            clear(id, ConstraintSet.LEFT)
            applyTo(parentLayout)

        }
    }

    /**
     *
     * override to start background task, etc
     *
     */
    protected open fun onUpdateCrlData(){}

    /**
     *
     * check CRL status and add warning if an update is overdue
     *
     */
    @RequiresApi(Build.VERSION_CODES.O)
    protected open fun checkCRLStatus() {

        val updateIsNeeded = CertificateRepository.instance.isUpdateOverdue()
        // fire off the usual warning for network issues
        if (updateIsNeeded) {
            logger?.printLine("CRL update is needed", loggerTag)
            onUpdateCrlData()
        }
    }

    /**
     *
     * set the text found within settings
     *
     * @param viewModel - viewmodel to access settings
     *
     */
    protected open fun setTextFromSettings(viewModel: ScanConfigViewModel) {
        val actionBar = supportActionBar

        actionBar?.title = viewModel.navigationTitleConfig
        instructionsBanner?.text = viewModel.guideConfig

    }

    /**
     *
     * generic onClick listener for all buttons
     *
     */
    private val onClick: View.OnClickListener = View.OnClickListener { view ->
        onButtonClicked(view)
    }


    /**
     *
     * gets the current state of an image button
     *
     * @param view - the button the state belongs to
     *
     * @return Boolean - the state ( active, not active )
     *
     */
    protected open fun getImageViewState(view: ImageView): Boolean {

        if (buttonStateMap.containsKey(view.id)) {
            val state = buttonStateMap[view.id] ?: false
            buttonStateMap[view.id] = !state
            return !state
        }

        return false
    }

    /**
     *
     * override to deal with button clicks
     *
     * @param button - enum representing button clicked
     *
     * @param state - on or off state
     *
     */
    protected open fun buttonClicked(button: ScanButtons, state: Boolean) {}

    /**
     *
     * change state of button and fire button clicked button
     *
     * @param view - the button that was clicked
     *
     */
    protected open fun onButtonClicked(view: View) {
        if (view == zoomButton) {
            buttonClicked(
                ScanButtons.Zoom,
                zoomButton.onSetViewState(
                    R.drawable.ic_zoom_out,
                    R.drawable.ic_zoom_in,
                    ::getImageViewState
                )
            )
        }
        if (view == torchButton) {
            buttonClicked(
                ScanButtons.Torch,
                torchButton.onSetViewState(
                    R.drawable.ic_torch_on,
                    R.drawable.ic_torch_off,
                    ::getImageViewState
                )
            )
        }
    }

    /**
     *
     * helper function for setting the layout
     *
     * @param id - id of the view having it's vertical position set
     *
     */
    protected open fun setVerticalPosition(id: Int) {

        // - Get bottom bounds of the BarcodeFrameGraphic
        val boxLength = Constants.BARCODE_FRAME_SIZE * wrapper.width
        val cy = wrapper.height / 2
        val frameBottom = cy + boxLength / 2

        // - Set vertical position of torch button to be centered between the bottom of the
        //   scanning frame and bottom of the camera view
        val yPosition = (frameBottom + wrapper.height) / 2

        // - Set constraints of the button within the layout
        ConstraintSet().apply {
            clone(parentLayout)
            // Set the position of the guideline
            setGuidelineBegin(R.id.buttonGuidline, yPosition.toInt())
            // Constrain the button against the guideline
            connect(
                id,
                ConstraintSet.TOP,
                R.id.buttonGuidline,
                ConstraintSet.TOP,
                0
            )
            connect(
                id,
                ConstraintSet.BOTTOM,
                R.id.buttonGuidline,
                ConstraintSet.BOTTOM,
                0
            )
            applyTo(parentLayout)
        }


    }

    /**
     *
     * adds frame to show actual scan portion of the video
     *
     */
    protected open fun addBarcodeFrameIfNeeded() {
        if (!frameAdded) {
            barcodeFrameGraphic = BarcodeFrameGraphic(graphicOverlay)
            graphicOverlay.add(barcodeFrameGraphic)
            frameAdded = true
        }
    }

    //region Permissions code
    /**
     *
     * check to see if all needed permissions are granted
     *
     * @return boolean representing if all permissions are granted
     *
     */
    private fun allPermissionsGranted(): Boolean {
        for (permission in getRequiredPermissions(this)) {
            permission?.let {
                if (!isPermissionGranted(this, it)) {
                    return false
                }
            }
        }
        return true
    }

    /**
     *
     * launch permission acceptance popup if not all permissions are granted/not denied
     *
     */
    private fun getRuntimePermissions() {
        val allNeededPermissions = ArrayList<String>()
        for (permission in getRequiredPermissions(this)) {
            permission?.let {
                if (!isPermissionGranted(this, it)) {
                    allNeededPermissions.add(permission)
                }
            }
        }

        if (allNeededPermissions.isNotEmpty()) {
            // Initiate the OS dialog to request permissions
            requestPermissionLauncher.launch(allNeededPermissions.toTypedArray())
        }
    }

    /**
     *
     * check if permission is granted
     *
     * @param context - the current context
     *
     * @param permission - string representing the permission being checked
     *
     * @return boolean representing whether the permission has been granted
     *
     */
    private fun isPermissionGranted(context: Context, permission: String): Boolean {
        if (ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            return true
        }

        return false
    }

    //endregion

}

