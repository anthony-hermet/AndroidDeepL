package com.anthony.deepl.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.StringDef;

import com.anthony.deepl.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

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

    private static final String LANGUAGE_SHARED_PREFERENCES = "deepl_language_manager";
    private static final String LAST_TRANSLATE_FROM_PREFERENCES_KEY = "last_translate_from";
    private static final String LAST_TRANSLATE_TO_PREFERENCES_KEY = "last_translate_to";

    public static String getLanguageString(@Language String language, Context context) {
        int resId;
        switch (language) {
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

    public static String formatStringForPostRequest(String toTranslate) {
        toTranslate = toTranslate.replace("\n", "%0D");
        return toTranslate;
    }

    public static String unformatStringResponseForDisplay(String translated) {
        translated = translated.replace("%0D", "\n");
        translated = translated.replace("%0", "\n");
        return translated;
    }

    @Language
    public static String getLanguageValue(String languageString, Context context) {
        List<String> languagesList = Arrays.asList(AUTO, ENGLISH, GERMAN, FRENCH, SPANISH, ITALIAN, DUTCH, POLISH);
        for (int i = 0, size = languagesList.size(); i < size; i++) {
            if (languageString.equals(getLanguageString(languagesList.get(i), context))) {
                return languagesList.get(i);
            }
        }
        return AUTO;
    }

    public static String[] getLanguagesStringArray(Context context, @Language String languageToRemove, boolean addAuto) {
        // We use a LinkedList because Arrays.asList return a fixed size list, not allowing item removal
        List<String> languagesList = new LinkedList<>(Arrays.asList(AUTO, ENGLISH, GERMAN, FRENCH, SPANISH, ITALIAN, DUTCH, POLISH));
        List<String> languagesStringList = new ArrayList<>();
        if (languageToRemove != null) {
            languagesList.remove(languageToRemove);
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
    public static String getLastUsedTranslateFrom(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(LANGUAGE_SHARED_PREFERENCES, MODE_PRIVATE);
        return prefs.getString(LAST_TRANSLATE_FROM_PREFERENCES_KEY, AUTO);
    }

    public static void saveLastUsedTranslateFrom(Context context, @Language String language) {
        SharedPreferences.Editor editor = context.getSharedPreferences(LANGUAGE_SHARED_PREFERENCES, MODE_PRIVATE).edit();
        editor.putString(LAST_TRANSLATE_FROM_PREFERENCES_KEY, language);
        editor.apply();
    }

    @Language
    public static String getLastUsedTranslateTo(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(LANGUAGE_SHARED_PREFERENCES, MODE_PRIVATE);
        return prefs.getString(LAST_TRANSLATE_TO_PREFERENCES_KEY, ENGLISH);
    }

    public static void saveLastUsedTranslateTo(Context context, @Language String language) {
        SharedPreferences.Editor editor = context.getSharedPreferences(LANGUAGE_SHARED_PREFERENCES, MODE_PRIVATE).edit();
        editor.putString(LAST_TRANSLATE_TO_PREFERENCES_KEY, language);
        editor.apply();
    }
}
