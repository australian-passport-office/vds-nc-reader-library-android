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

package au.gov.dfat.lib.vdsncchecker.models

import android.content.Context
import au.gov.dfat.lib.vdsncchecker.repository.PreferenceRepository
import au.gov.dfat.lib.vdsncchecker.repository.ScanPreferenceRepository

/**
 *
 * viewmodel for scan view settings
 *
 */
class ScanConfigViewModel(context: Context) : PreferenceViewModel() {

    fun <T> setConfig(key: String, value: T){
            repository.storePreference(key, value)
    }

    override val repository: ScanPreferenceRepository = ScanPreferenceRepository(context)

    val torchConfig: Boolean get() { return repository.torchValue }
    val zoomConfig: Boolean get(){ return repository.zoomValue }
    val navigationTitleConfig: String get(){ return  repository.navigationTitleValue }
    val invalidVdsConfig: String get(){ return  repository.invalidVdsTitleValue }
    val guideConfig: String get(){ return  repository.guideValue }
    val clrWarningConfig: String get(){ return  repository.crlWarningValue }

}