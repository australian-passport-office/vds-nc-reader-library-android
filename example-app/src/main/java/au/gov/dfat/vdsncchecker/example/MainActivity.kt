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

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import au.gov.dfat.lib.vdsncchecker.CertificateData
import au.gov.dfat.lib.vdsncchecker.repository.CRLStoreWrapper
import au.gov.dfat.lib.vdsncchecker.repository.CertificateRepository
import au.gov.dfat.vdsncchecker.example.mlkit.BarcodeReader
import java.net.URL


@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : AppCompatActivity(), IResultLauncher {

    private val barcodeReader: BarcodeReader = BarcodeReader()
    private val pdfReader = VDSPdfReader(this, barcodeReader)

    init {

        barcodeReader.apply {
            this.onSuccess = {
                if (it.count() > 0) {
                    var data = it.first().rawValue
                    processVds(data)
                }
            }
            this.onFailure = {
                showAlertDialog(R.string.noVds, R.string.noVdsDescription)
            }
            this.onComplete = {
                if (!it) {
                    showAlertDialog(R.string.noVds, R.string.noVdsDescription)
                }
            }
        }
    }

    private fun initCertificateRepository(){
        // add certs
        try{
            val certRepo = CertificateRepository.instance

            if(certRepo.cscaCertificates.count() == 0){
                certRepo.setContext(applicationContext)
                val protoCrl = CRLStoreWrapper(null, Data.protoCrl)

                val protoData = CertificateData(Data.protoHash, Data.protoCsca,protoCrl)
                certRepo.addCertificate(protoData);

                val specData = CertificateData(Data.cscaCertSHA256Hash, Data.cscaCertData, CRLStoreWrapper(null, Data.crlData))
                certRepo.addCertificate(specData)

                val japanData = CertificateData(Data.JAPAN_HASH, Data.JAPAN_CSCA, CRLStoreWrapper(
                    URL(Data.JAPAN_CRL_URL), Data.JAPAN_CRL))
                certRepo.addCertificate(japanData)

                certRepo.startAutoUpdatingCRLData()

            }

        }catch(exception: Exception){
            var e = exception
        }



    }

    /**
     *
     * For processing results from PDF file picker intent
     *
     * Example shows attaching listeners to the returned processing task from the barcode scanner
     * Currently only processing first found code
     *
     */
    override val resultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                try {
                    pdfReader.processPdfResult(result.data)

                } catch (exception: Exception) {
                    showAlertDialog(R.string.noVds, R.string.noVdsDescription)
                }

            }
        }


    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //TODO: check why anti debug breaks app
//        val security = SecurityManager(this)
//        val results = security.runAllChecks()
//        security.runExternalTests()

        initCertificateRepository()

        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.testScan).setOnClickListener {
            try {
                Intent(this, ScanActivity::class.java).apply {
                    
                    startActivity(this)
                }
            } catch (_: Exception) {

            }
        }




        findViewById<Button>(R.id.buttonImportVdsPdf).setOnClickListener {
            try {
                pdfReader.openPdfFile()
            } catch (exception: Exception) {
                showAlertDialog(R.string.genericError, R.string.pdfProcessError)
            }

        }

        findViewById<Button>(R.id.buttonValidVds).setOnClickListener {

            processVds(Data.validVDSJson)
        }

        findViewById<Button>(R.id.buttonInvalidVds).setOnClickListener {
            processVds(Data.invalidVDSJson)
        }

        findViewById<Button>(R.id.buttonNotVds).setOnClickListener {
            processVds(Data.nonVDSJson)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.action_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_attributions -> {
                startActivity(Intent(this, AttributionsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun processVds(data: String) {
        // Decode VDS
        var processor = VdsProcessor()
        var vds = processor.parseVdsData(data)
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
                showAlertDialog(R.string.invalidVdsTitle, R.string.invalidVdsDescription)
            }
        } else {
            showAlertDialog(R.string.notVdsTitle, R.string.notVdsDescription)
        }
    }

    private fun showAlertDialog(@StringRes titleRes: Int, @StringRes messageRes: Int) {
        val dialog = AlertDialog.Builder(this)
            .setTitle(titleRes)
            .setMessage(messageRes)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
        dialog.show()
    }
}