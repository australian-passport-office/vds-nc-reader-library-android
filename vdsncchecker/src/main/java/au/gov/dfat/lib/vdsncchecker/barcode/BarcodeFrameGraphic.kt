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

package au.gov.dfat.lib.vdsncchecker.barcode

import android.graphics.*
import androidx.core.content.ContextCompat
import au.gov.dfat.lib.vdsncchecker.camera.GraphicOverlay
import au.gov.dfat.lib.vdsncchecker.config.Constants
import au.gov.dfat.lib.vdsncchecker.R
import au.gov.dfat.lib.vdsncchecker.utils.toPx

/**
 *
 * Frame that covers scan screen, with section for actual scan
 *
 * @param overlay - A view which renders a series of custom graphics to be overlayed on top of an associated preview ( Google )
 *
 * @property box - the bounds of the scan box. Can use for scan crop
 *
 * eg -
 *
 * using MLKit's image analyzer - proxy.setCropRect(box)
 *
 */
class BarcodeFrameGraphic(private val overlay: GraphicOverlay) : GraphicOverlay.Graphic(overlay) {

    private val edgeLength = 30.toPx

    var box: Rect = Rect()

    private val boxPaint: Paint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.barcode_reticle_stroke)
        style = Paint.Style.STROKE
        strokeWidth =
            context.resources.getDimensionPixelOffset(R.dimen.barcode_reticle_stroke_width).toFloat()
    }

    private val scrimPaint: Paint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.barcode_reticle_background)
    }

    private val eraserPaint: Paint = Paint().apply {
        strokeWidth = boxPaint.strokeWidth
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        style = Paint.Style.FILL_AND_STROKE
    }

    private val cornerPaint: Paint = Paint().apply {
        strokeWidth = boxPaint.strokeWidth
        style = Paint.Style.STROKE
        color = Color.WHITE
    }

    override fun draw(canvas: Canvas) {
        val overlayWidth = overlay.width.toFloat()
        val overlayHeight = overlay.height.toFloat()
        val boxLength = Constants.BARCODE_FRAME_SIZE * overlayWidth
        val cx = overlayWidth / 2
        val cy = overlayHeight / 2
        val left = cx - boxLength / 2
        val right = cx + boxLength / 2
        val top = cy - boxLength / 2
        val bottom = cy + boxLength / 2
        val offset = (cornerPaint.strokeWidth / 4).toInt().toPx

        val boxRect = RectF(left, top, right, bottom)

        canvas.apply {
            // Draws the dark background scrim and leaves the box area clear.
            drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), scrimPaint)
            // As the stroke is always centred, so erase twice with FILL and STROKE respectively to
            // clear all area that the box rect would occupy.
            drawRect(boxRect, eraserPaint)
            // Draws the box.
            drawRect(boxRect, boxPaint)

            drawLine(left - offset, top, left + edgeLength, top, cornerPaint)
            drawLine(left, top - offset, left, top + edgeLength, cornerPaint)
            drawLine(right + offset, top, right - edgeLength, top, cornerPaint)
            drawLine(right, top - offset, right, top + edgeLength, cornerPaint)
            drawLine(left - offset, bottom, left + edgeLength, bottom, cornerPaint)
            drawLine(left, bottom + offset, left, bottom - edgeLength, cornerPaint)
            drawLine(right + offset, bottom, right - edgeLength, bottom, cornerPaint)
            drawLine(right, bottom + offset, right, bottom - edgeLength, cornerPaint)
        }
        boxRect.round(box)
    }
}