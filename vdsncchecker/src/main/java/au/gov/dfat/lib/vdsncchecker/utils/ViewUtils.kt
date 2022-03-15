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

package au.gov.dfat.lib.vdsncchecker.utils

import android.content.Context
import android.content.res.Resources
import android.view.View
import android.widget.ImageView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog

val Int.toDp: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()
val Int.toPx: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

fun Context.showAlertDialog(@StringRes titleRes: Int, @StringRes messageRes: Int) {
    val dialog = AlertDialog.Builder(this)
        .setTitle(titleRes)
        .setMessage(messageRes)
        .setPositiveButton(android.R.string.ok) { dialog, _ ->
            dialog.dismiss()
        }
        .create()
    dialog.show()
}

fun Context.showAlertDialog(@StringRes titleRes: Int, @StringRes messageRes: Int, okCallback:() -> Unit) {
    val dialog = AlertDialog.Builder(this)
        .setTitle(titleRes)
        .setMessage(messageRes)
        .setPositiveButton(android.R.string.ok) { dialog, _ ->
            dialog.dismiss()
            okCallback()
        }
        .create()
    dialog.show()
}

/**
 *
 * Sets image resource based on a boolean value
 * @param onImageId - resource id of image when on
 * @param offImageId - resource id of image when off
 * @param getViewState - function that returns boolean when checking for state of imageView representative
 *
 */
fun ImageView.onSetViewState(onImageId: Int, offImageId: Int, getViewState: (view: ImageView) -> Boolean): Boolean{
    // return state based on checking of actual hardware state, etc
    var state = getViewState.invoke(this)
    // set image resource based on state
    if(state){
        setImageResource(onImageId)
    } else{
        setImageResource(offImageId)
    }
    return state
}
