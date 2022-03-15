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

package au.gov.dfat.lib.vdsncchecker.controls

import android.content.Context
import android.content.DialogInterface
import androidx.annotation.StringRes
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 *
 * generic alert dialog
 *
 */
fun showAlertDialog(
    context: Context,
    @StringRes titleRes: Int,
    @StringRes messageRes: Int,
    @StringRes okButtonLabel: Int = android.R.string.ok,
    showCancelButton: Boolean = false,
    @StringRes cancelButtonLabel: Int = android.R.string.cancel,
    okCallback: (dialog: DialogInterface, which: Int) -> Unit = { dialog, _ -> dialog.dismiss() },
    cancelCallback: (dialog: DialogInterface, which: Int) -> Unit = { dialog, _ -> dialog.dismiss() }
) {
    val builder = MaterialAlertDialogBuilder(context)
        .setTitle(titleRes)
        .setMessage(messageRes)
        .setPositiveButton(okButtonLabel) { dialog, which ->
            okCallback.invoke(dialog, which)
        }
        .setCancelable(false)

    if (showCancelButton) {
        builder.setNegativeButton(cancelButtonLabel) { dialog, which ->
            cancelCallback.invoke(dialog, which)
        }
    }

    builder.show()
}

fun showAlertDialog(
    context: Context,
    title: String,
    message: String,
    okCallback: (dialog: DialogInterface, which: Int) -> Unit = { dialog, _ -> dialog.dismiss() }
) {
    val builder = MaterialAlertDialogBuilder(context)
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(android.R.string.ok) { dialog, which ->
            okCallback.invoke(dialog, which)
        }
        .setCancelable(false)

    builder.show()
}
