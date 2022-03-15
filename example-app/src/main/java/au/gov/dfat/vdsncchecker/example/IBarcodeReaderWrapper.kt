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

import android.graphics.Bitmap
/**
 *
 * wrapper for barcode readers
 *
 * @param ExpectedProcessType - the expected type passed into the onSuccess event
 *
 */
interface IBarcodeReaderWrapper<ExpectedProcessType> {


    fun processPdf(image: Bitmap)

    var onSuccess: ((MutableList<ExpectedProcessType>) -> Unit )
    var onFailure: () -> Unit
    var onComplete: (Boolean) -> Unit

}