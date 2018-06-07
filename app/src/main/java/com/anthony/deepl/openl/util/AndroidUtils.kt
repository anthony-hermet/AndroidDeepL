package com.anthony.deepl.openl.util

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.util.TypedValue

object AndroidUtils {

    fun convertDpToPixel(dp: Float, context: Context): Float {
        val resources = context.resources
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)
    }

    fun convertPixelsToDp(px: Float, context: Context): Float {
        val resources = context.resources
        val metrics = resources.displayMetrics
        return px / (metrics.densityDpi / 160f)
    }

    fun getClipboardText(clipboardManager: ClipboardManager?): String? {
        var text: String? = null
        if (clipboardManager != null && clipboardManager.hasPrimaryClip()) {
            val data = clipboardManager.primaryClip
            if (data.description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) || data.description.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)) {
                text = data.getItemAt(0).text.toString()
            }
        }
        return if (!text.isNullOrEmpty()) text else null
    }

}

