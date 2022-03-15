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
import android.content.SharedPreferences
import java.lang.Exception

/**
 *
 * repository for accessing preferences ( settings )
 *
 */
abstract class PreferenceRepository(private val context: Context) {

    /**
     *
     * generic function for storing/updating preferences
     *
     * @param key - the name of the preference
     * @param value - the value of the preference
     *
     */
    open fun <T> storePreference(key: String, value: T){

        val editor = getSharedPreferences().edit()

        if(value is Boolean){
            editor.putBoolean(key, value).apply()
            return
        }
        if(value is String){
            editor.putString(key, value).apply()
            return
        }

        val stringSet = value as? Set<String>?
        if(value != null){
            editor.putStringSet(key, stringSet).apply()
            return
        }
    }


    /**
     *
     * get preferences to add or pull
     *
     * @return the preference store
     *
     */
    protected open fun getSharedPreferences(): SharedPreferences {

        return context.applicationContext.getSharedPreferences("vdsnc_prefs.dat", Context.MODE_PRIVATE)

    }

    /**
     *
     * generic function for getting preferences
     *
     * @param key - the preference name/key
     * @param default - the default value ( preference not in store )
     *
     */
    open fun <T> getPreference(key: String, default: T): T{
        var preferences = getSharedPreferences()

        if(default is Boolean){
            return preferences.getBoolean(key, default) as T
        }

        if(default is String){
            return getSharedPreferences().getString(key, default) as T
        }

        val stringSet = default as? Set<String>?
        if(stringSet != null){
            return getSharedPreferences().getStringSet(key, default) as T
        }

        throw Exception("Type not supported")

    }

}