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

import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import au.gov.dfat.lib.vdsncchecker.VDSData

class DetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val vds = intent.getParcelableExtra<VDSData>(VDS_DATA_KEY)!!

        with(vds) {
            findViewById<TextView>(R.id.valueType).text =  hdr.t
            findViewById<TextView>(R.id.valueVersion).text =  hdr.v.toString()
            findViewById<TextView>(R.id.valueIssuingCountry).text =  hdr.hdrIs

            findViewById<TextView>(R.id.valueUvci).text =  msg.uvci
            findViewById<TextView>(R.id.valueName).text =  msg.pid.n
            findViewById<TextView>(R.id.valueDob).text =  msg.pid.dob
            findViewById<TextView>(R.id.valueTravelDocumentNo).text =  msg.pid.i
            findViewById<TextView>(R.id.valueOtherDocumentNo).text =  msg.pid.ai
            findViewById<TextView>(R.id.valueSex).text =  msg.pid.sex
        }

        // Bind list of events
        findViewById<RecyclerView>(R.id.events).apply {
            layoutManager = object : LinearLayoutManager(context, VERTICAL, false) {
                override fun canScrollVertically(): Boolean {
                    return false
                }
            }
            adapter = EventAdapter(vds.msg.ve)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        const val VDS_DATA_KEY = "VDS_DATA"
    }
}