package com.anthony.deepl.openl.util

import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.content.res.Resources
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment

// DIMENSIONS
val Int.pxToDp: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()
val Int.dpToPx: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

// CLIPBOARD
fun Fragment.getClipboardText(): String? {
    var text: String? = null
    val clipboardManager = context?.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager?
    if (clipboardManager != null && clipboardManager.hasPrimaryClip()) {
        clipboardManager.primaryClip?.let {
            if (it.description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) || it.description.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)) {
                text = it.getItemAt(0).text.toString()
            }
        }
    }
    return text
}

fun Fragment.copyToClipboard(text: String) {
    val clipboardManager = context?.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager?
    clipboardManager?.let {
        val clip = ClipData.newPlainText(text, text)
        it.primaryClip = clip
    }
}

// KEYBAORD
fun Activity.hideKeyboard() {
    hideKeyboard(if (currentFocus == null) View(this) else currentFocus)
}

fun Fragment.hideKeyboard() {
    view?.let {
        activity?.hideKeyboard(it)
    }
}

fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}