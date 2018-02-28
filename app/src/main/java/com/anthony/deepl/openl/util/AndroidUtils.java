package com.anthony.deepl.openl.util;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.util.TypedValue;

public class AndroidUtils {

    public static float convertDpToPixel(float dp, Context context) {
        Resources resources = context.getResources();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.getDisplayMetrics());
    }

    public static float convertPixelsToDp(float px, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return px / (metrics.densityDpi / 160f);
    }

    public static String getClipboardText(ClipboardManager clipboardManager) {
        String text = null;
        if (clipboardManager != null && clipboardManager.hasPrimaryClip()) {
            ClipData data = clipboardManager.getPrimaryClip();
            if (data.getDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) || data.getDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)) {
                text = data.getItemAt(0).getText().toString();
            }
        }
        return (text != null && text.length() > 0) ? text : null;
    }

}

