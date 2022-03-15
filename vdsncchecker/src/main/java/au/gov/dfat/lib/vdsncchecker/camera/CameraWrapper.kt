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

package au.gov.dfat.lib.vdsncchecker.camera

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout

/**
 *
 * wrapper class to allow custom camera preview, extends FrameLayout implements ICameraWrapper
 *
 */
open class CameraWrapper(context: Context, attrs: AttributeSet?): FrameLayout(context, attrs),
    ICameraWrapper
{
    constructor(context:Context) : this(context, null)

    /**
     *
     * Set the camera view from chosen implementation
     *
     */
    override fun setCameraView(cameraView: View){
        cameraView.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )

        addView(cameraView)
    }



}