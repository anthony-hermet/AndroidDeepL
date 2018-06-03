package com.anthony.deepl.openl.view.main

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView

import com.anthony.deepl.openl.R

class ShrinkSpinnerAdapter<T>(private val mContext: Context, resource: Int, objects: Array<T>) : ArrayAdapter<T>(mContext, resource, objects) {
    private var mDetectedLanguage: String? = null

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var selectedItemPosition = position
        if (parent is AdapterView<*>) {
            selectedItemPosition = parent.selectedItemPosition
        }
        return if (selectedItemPosition == 0 && mDetectedLanguage != null) {
            makeLayout(convertView, parent)
        } else super.getView(selectedItemPosition, convertView, parent)
    }

    private fun makeLayout(convertView: View?, parent: ViewGroup): View {
        val tv =
                if (convertView != null) {
                    convertView as TextView
                } else {
                    LayoutInflater.from(mContext).inflate(R.layout.item_language_spinner, parent, false) as TextView
                }

        tv.text = mDetectedLanguage
        return tv
    }

    fun setDetectedLanguage(detectedLanguage: String) {
        mDetectedLanguage = detectedLanguage
    }

    fun clearDetectedLanguage() {
        mDetectedLanguage = null
    }
}
