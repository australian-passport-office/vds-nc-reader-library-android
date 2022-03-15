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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import au.gov.dfat.lib.vdsncchecker.VDSVd

class DetailAdapter(private val details: List<VDSVd>)
    : RecyclerView.Adapter<DetailAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.vaccination_detail_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = details[position]
        holder.apply {
            date.text = item.dvc
            doseNo.text = item.seq.toString()
            country.text = item.ctr
            adminCentre.text = item.adm
            batchNo.text = item.lot
            dueDate.text = item.dvn
        }
    }

    override fun getItemCount(): Int = details.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val date: TextView = view.findViewById(R.id.valueDate)
        val doseNo: TextView = view.findViewById(R.id.valueDoseNo)
        val country: TextView = view.findViewById(R.id.valueCountry)
        val adminCentre: TextView = view.findViewById(R.id.valueAdminCentre)
        val batchNo: TextView = view.findViewById(R.id.valueBatchNo)
        val dueDate: TextView = view.findViewById(R.id.valueDueDate)
    }
}