package com.anthony.deepl.manager;

import android.content.Context;
import android.support.annotation.StringDef;

import com.anthony.deepl.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class LanguageManager {

    // We avoid using an enum here form performance reasons
    // For more information : https://android.jlelse.eu/android-performance-avoid-using-enum-on-android-326be0794dc3

    public static final String AUTO = "auto";
    public static final String ENGLISH = "EN";
    public static final String GERMAN = "DE";
    public static final String FRENCH = "FR";
    public static final String SPANISH = "ES";
    public static final String ITALIAN = "IT";
    public static final String DUTCH = "NL";
    public static final String POLISH = "PL";

    @StringDef({AUTO, ENGLISH, GERMAN, FRENCH, SPANISH, ITALIAN, DUTCH, POLISH})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Language {}

    public static String getLanguageString(@Language String language, Context context) {
        int resId;
        switch (language) {
            case AUTO :
                resId = R.string.spinner_detect_language;
                break;
            case ENGLISH :
                resId = R.string.spinner_english;
                break;
            case GERMAN :
                resId = R.string.spinner_german;
                break;
            case FRENCH :
                resId = R.string.spinner_french;
                break;
            case SPANISH :
                resId = R.string.spinner_spanish;
                break;
            case ITALIAN :
                resId = R.string.spinner_italian;
                break;
            case DUTCH :
                resId = R.string.spinner_dutch;
                break;
            case POLISH :
                resId = R.string.spinner_polish;
                break;
            default :
                resId = R.string.spinner_detect_language;
                break;
        }

        return context.getString(resId);
    }

    public static String[] getLanguagesStringArray(Context context, @Language String languageToRemove, boolean addAuto) {
        // We use a LinkedList because Arrays.asList return a fixed size list, not allowing item removal
        List<String> languagesList = new LinkedList<>(Arrays.asList(AUTO, ENGLISH, GERMAN, FRENCH, SPANISH, ITALIAN, DUTCH, POLISH));
        List<String> languagesStringList = new ArrayList<>();
        if (languageToRemove != null) {
            languagesList.remove(ENGLISH);
        }
        if (!addAuto) {
            languagesList.remove(AUTO);
        }

        for (int i = 0, size = languagesList.size(); i < size; i++) {
            languagesStringList.add(getLanguageString(languagesList.get(i), context));
        }

        String[] languagesStringArray = new String[languagesStringList.size()];
        languagesStringArray = languagesStringList.toArray(languagesStringArray);
        return languagesStringArray;
    }

    @Language
    public static String getSavedTranslateFrom() {
        return AUTO;
    }

    @Language
    public static String getSavedTranslateTo() {
        return ENGLISH;
    }
}
