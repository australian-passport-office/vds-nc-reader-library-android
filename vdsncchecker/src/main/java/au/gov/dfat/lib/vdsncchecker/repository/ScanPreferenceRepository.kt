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

package au.gov.dfat.lib.vdsncchecker.repository

import android.content.Context
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import au.gov.dfat.lib.vdsncchecker.R

/**
 *
 * repository for accessing settings in relation to scan view
 *
 */
open class ScanPreferenceRepository(private val context: Context) : PreferenceRepository(context) {

    val zoomValue: Boolean get() { return getPreference(zoomString, true) }
    val torchValue: Boolean get(){ return getPreference(torchString, true) }
    val navigationTitleValue: String get() { return getPreference(navigationTitleString,getDefaultText(R.string.scanTitle)) }
    val invalidVdsTitleValue: String get() { return getPreference(invalidVdsString, getDefaultText(R.string.invalidVdsTitle)) }
    val guideValue: String get() { return getPreference(guideString, getDefaultText(R.string.scanGuideLabelText)) }
    val crlWarningValue: String get() { return getPreference(clrWarningString, getDefaultText(R.string.connectToInternetWarning)) }

    private fun getDefaultText(@StringRes id: Int): String{

        return context.getString(id)

    }

    companion object {


        const val zoomString = "scan_screen_zoom_preference"
        const val torchString = "scan_screen_torch_preference"
        const val navigationTitleString = "scan_screen_navigation_title"
        const val invalidVdsString = "scan_screen_invalid_vds_nc_label"
        const val guideString = "scan_screen_guide_label"
        const val clrWarningString = "scan_screen_clr_warning_label"
    }

}

