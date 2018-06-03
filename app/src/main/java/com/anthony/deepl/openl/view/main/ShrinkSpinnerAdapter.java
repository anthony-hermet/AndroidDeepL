package com.anthony.deepl.openl.view.main;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.anthony.deepl.openl.R;

public class ShrinkSpinnerAdapter<T> extends ArrayAdapter<T> {

    private Context mContext;
    private String mDetectedLanguage;

    public ShrinkSpinnerAdapter(@NonNull Context context, int resource, @NonNull T[] objects) {
        super(context, resource, objects);
        mContext = context;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        int selectedItemPosition = position;
        if (parent instanceof AdapterView) {
            selectedItemPosition = ((AdapterView) parent).getSelectedItemPosition();
        }
        if (selectedItemPosition == 0 && mDetectedLanguage != null) {
            return makeLayout(convertView, parent);
        }
        return super.getView(selectedItemPosition, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return super.getDropDownView(position, convertView, parent);
    }

    private View makeLayout(final View convertView, final ViewGroup parent) {
        TextView tv;
        if (convertView != null) {
            tv = (TextView) convertView;
        } else {
            tv = (TextView) LayoutInflater.from(mContext).inflate(R.layout.item_language_spinner, parent, false);
        }

        tv.setText(mDetectedLanguage);
        return tv;
    }

    public void setDetectedLanguage(String detectedLanguage) {
        mDetectedLanguage = detectedLanguage;
    }

    public void clearDetectedLanguage() {
        mDetectedLanguage = null;
    }
}
