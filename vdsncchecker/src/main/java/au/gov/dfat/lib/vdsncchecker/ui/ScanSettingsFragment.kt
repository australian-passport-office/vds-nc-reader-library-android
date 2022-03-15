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

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.preference.*
import au.gov.dfat.lib.vdsncchecker.R
import au.gov.dfat.lib.vdsncchecker.models.ScanConfigViewModel
import au.gov.dfat.lib.vdsncchecker.models.ScanConfigViewModelFactory
import au.gov.dfat.lib.vdsncchecker.repository.ScanPreferenceRepository

/**
 *
 * fragment for displaying and updating scan settings
 *
 */
open class ScanSettingsFragment : PreferenceFragmentCompat() {


    private lateinit var torchPreference: CheckBoxPreference
    private lateinit var zoomPreference: CheckBoxPreference
    private lateinit var guidePreference: EditTextPreference
    private lateinit var navigationTitlePreference: EditTextPreference
    private lateinit var invalidVdsPreference: EditTextPreference
    private lateinit var clrWarningPreference: EditTextPreference

    /**
     *
     * detect changes in settings as user interacts
     *
     * @param pref - preference changing
     *
     * @param value - the value that is changing
     *
     * @return boolean representing whether the change was handled successfully
     *
     */
    private fun changeListener(pref: Preference, value: Any):Boolean{

        var context = activity?.applicationContext
        if(context != null){
            var viewModel = viewModels<ScanConfigViewModel> {
                ScanConfigViewModelFactory(context)
            }.value

            if (pref == guidePreference) {
                viewModel.setConfig(ScanPreferenceRepository.guideString,
                    value
                )
            }
            if (pref == clrWarningPreference) {
                viewModel.setConfig(
                    ScanPreferenceRepository.clrWarningString,
                    value
                )
            }
            if (pref == invalidVdsPreference) {
                viewModel.setConfig(
                    ScanPreferenceRepository.invalidVdsString,
                    value
                )
            }
            if (pref == navigationTitlePreference) {
                viewModel.setConfig(
                    ScanPreferenceRepository.navigationTitleString,
                    value
                )
            }
            // call for derived preference changes
            val result = onChangeListener(context, pref, value)

            if(!result){
                return false
            }

            if(value is String){

                pref.summary = value

            }

            return result;

        }

        return false

    }

    /**
     *
     *  for derived class preference implementations
     *
     *  @param context - the current context
     *
     *  @param pref - the preference changing
     *
     *  @param value - the changing value
     *
     *  @return boolean representing whether handling was successful
     *
     */
    protected open fun onChangeListener(context:Context, pref: Preference, value: Any):Boolean{

        return true;
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        setPreferencesFromResource(R.xml.scan_preferences, rootKey)

        findPreference<CheckBoxPreference>(ScanPreferenceRepository.torchString).let {
            if (it != null) {
                torchPreference = it

            }
        }
        findPreference<CheckBoxPreference>(ScanPreferenceRepository.zoomString).let {
            if (it != null) {
                zoomPreference = it
            }
        }
        findPreference<EditTextPreference>(ScanPreferenceRepository.guideString).let {
            if (it != null) {
                guidePreference = it
                guidePreference.summary = guidePreference.text
                guidePreference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener(::changeListener)
            }
        }
        findPreference<EditTextPreference>(ScanPreferenceRepository.invalidVdsString).let {
            if (it != null) {
                invalidVdsPreference = it
                invalidVdsPreference.summary = invalidVdsPreference.text
                invalidVdsPreference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener(::changeListener)
            }
        }
        findPreference<EditTextPreference>(ScanPreferenceRepository.navigationTitleString).let {
            if (it != null) {
                navigationTitlePreference = it
                navigationTitlePreference.summary = navigationTitlePreference.text

                navigationTitlePreference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener(::changeListener)
            }
        }
        findPreference<EditTextPreference>(ScanPreferenceRepository.clrWarningString).let {
            if (it != null) {
                clrWarningPreference = it
                clrWarningPreference.summary = clrWarningPreference.text
                clrWarningPreference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener(::changeListener)

            }
        }

        setConfigValues()

    }


    override fun onPreferenceTreeClick(preference: Preference?): Boolean {

        var context = activity?.applicationContext
        if (preference != null && context != null) {

            var viewModel = viewModels<ScanConfigViewModel> {
                ScanConfigViewModelFactory(context)
            }.value

            if (preference == torchPreference) {
                viewModel.setConfig(ScanPreferenceRepository.torchString, torchPreference.isChecked)
            }
            if (preference == zoomPreference) {
                viewModel.setConfig(ScanPreferenceRepository.zoomString, zoomPreference.isChecked)
            }

        }

        return super.onPreferenceTreeClick(preference)
    }

    /**
     *
     *  for derived setting of config values
     *
     *  @param context - the current context
     *
     */
    protected open fun onSetConfigValues(context: Context){}

    private fun setConfigValues() {

        var context = activity?.applicationContext

        if (context != null) {

            var viewModel = viewModels<ScanConfigViewModel> {
                ScanConfigViewModelFactory(context)
            }.value
            // torch checking

            var torchConfig = viewModel.torchConfig
            torchPreference.isEnabled = deviceHasTorch(context)
            torchPreference.isChecked = torchConfig

            // zoom checking

            var zoomConfig = viewModel.zoomConfig
            zoomPreference.isEnabled = deviceCanZoom(context)
            zoomPreference.isChecked = zoomConfig


            navigationTitlePreference.text = viewModel.navigationTitleConfig

            guidePreference.text = viewModel.guideConfig

            invalidVdsPreference.text = viewModel.invalidVdsConfig

            clrWarningPreference.text = viewModel.clrWarningConfig

            // call for derived implementations without needing to override onCreatePreferences
            onSetConfigValues(context)
        }

    }

    /**
     *
     * check if device has a torch
     *
     * @param context - the current context
     *
     * @return boolean representing whether the device has a torch
     *
     */
    private fun deviceHasTorch(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
    }

    /**
     *
     * check if the device can zoom ( or has camera )
     *
     * @param context - the current context
     *
     * @return boolean representing whether the device can zoom
     *
     */
    private fun deviceCanZoom(context: Context): Boolean {

        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }


}


