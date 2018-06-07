package com.anthony.deepl.openl.manager

import android.content.Context
import android.speech.tts.TextToSpeech
import android.support.annotation.StringDef

import com.anthony.deepl.openl.R

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.util.ArrayList
import java.util.Arrays
import java.util.LinkedList
import java.util.Locale

import android.content.Context.MODE_PRIVATE

object LanguageManager {

    const val AUTO = "auto"
    const val ENGLISH = "EN"
    const val GERMAN = "DE"
    const val FRENCH = "FR"
    const val SPANISH = "ES"
    const val ITALIAN = "IT"
    const val DUTCH = "NL"
    const val POLISH = "PL"

    private const val LANGUAGE_SHARED_PREFERENCES = "deepl_language_manager"
    private const val LAST_TRANSLATE_FROM_PREFERENCES_KEY = "last_translate_from"
    private const val LAST_TRANSLATE_TO_PREFERENCES_KEY = "last_translate_to"

    @StringDef(AUTO, ENGLISH, GERMAN, FRENCH, SPANISH, ITALIAN, DUTCH, POLISH)
    @Retention(RetentionPolicy.SOURCE)
    annotation class Language

    fun getLanguageString(@Language language: String?, context: Context): String {
        val resId = when (language) {
            ENGLISH -> R.string.spinner_english
            GERMAN -> R.string.spinner_german
            FRENCH -> R.string.spinner_french
            SPANISH -> R.string.spinner_spanish
            ITALIAN -> R.string.spinner_italian
            DUTCH -> R.string.spinner_dutch
            POLISH -> R.string.spinner_polish
            else -> R.string.spinner_detect_language
        }

        return context.getString(resId)
    }

    @Language
    fun getLanguageValue(languageString: String, context: Context): String {
        val languagesList = Arrays.asList(AUTO, ENGLISH, GERMAN, FRENCH, SPANISH, ITALIAN, DUTCH, POLISH)
        languagesList.forEach {
            if (languageString == getLanguageString(it, context)) {
                return it
            }
        }
        return AUTO
    }

    fun getLocaleFromLanguageValue(@Language language: String?, textToSpeech: TextToSpeech?): Locale {
        return when (language) {
            ENGLISH -> Locale.ENGLISH
            GERMAN -> Locale.GERMAN
            FRENCH -> Locale.FRENCH
            SPANISH -> Locale("es")
            ITALIAN -> Locale.ITALIAN
            DUTCH -> {
                var locale = Locale("nl")
                if (textToSpeech == null || textToSpeech.isLanguageAvailable(locale) == TextToSpeech.LANG_AVAILABLE) {
                    return locale
                }

                locale = Locale("nl_NL")
                if (textToSpeech.isLanguageAvailable(locale) == TextToSpeech.LANG_AVAILABLE) {
                    return locale
                }

                locale = Locale("de_NL")
                if (textToSpeech.isLanguageAvailable(locale) == TextToSpeech.LANG_AVAILABLE) {
                    return locale
                }

                Locale("fy_NL")
            }
            POLISH -> {
                val locale = Locale("pl")
                if (textToSpeech == null || textToSpeech.isLanguageAvailable(locale) == TextToSpeech.LANG_AVAILABLE) {
                    return locale
                }

                Locale("pl_PL")
            }
            else -> Locale.UK
        }
    }

    fun getLanguagesStringArray(context: Context, @Language languageToRemove: String?, addAuto: Boolean): Array<String> {
        // We use a LinkedList because Arrays.asList return a fixed size list, not allowing item removal
        val languagesList = LinkedList(Arrays.asList(AUTO, ENGLISH, GERMAN, FRENCH, SPANISH, ITALIAN, DUTCH, POLISH))
        val languagesStringList = ArrayList<String>()
        if (languageToRemove != null) {
            languagesList.remove(languageToRemove)
        }
        if (!addAuto) {
            languagesList.remove(AUTO)
        }

        languagesList.forEach {
            languagesStringList.add(getLanguageString(it, context))
        }
        return languagesStringList.toTypedArray()
    }

    @Language
    fun getLastUsedTranslateFrom(context: Context): String {
        val prefs = context.getSharedPreferences(LANGUAGE_SHARED_PREFERENCES, MODE_PRIVATE)
        return prefs.getString(LAST_TRANSLATE_FROM_PREFERENCES_KEY, AUTO)
    }

    fun saveLastUsedTranslateFrom(context: Context, @Language language: String) {
        val editor = context.getSharedPreferences(LANGUAGE_SHARED_PREFERENCES, MODE_PRIVATE).edit()
        editor.putString(LAST_TRANSLATE_FROM_PREFERENCES_KEY, language)
        editor.apply()
    }

    @Language
    fun getLastUsedTranslateTo(context: Context): String {
        val prefs = context.getSharedPreferences(LANGUAGE_SHARED_PREFERENCES, MODE_PRIVATE)
        return prefs.getString(LAST_TRANSLATE_TO_PREFERENCES_KEY, ENGLISH)
    }

    fun saveLastUsedTranslateTo(context: Context, @Language language: String) {
        val editor = context.getSharedPreferences(LANGUAGE_SHARED_PREFERENCES, MODE_PRIVATE).edit()
        editor.putString(LAST_TRANSLATE_TO_PREFERENCES_KEY, language)
        editor.apply()
    }
}
