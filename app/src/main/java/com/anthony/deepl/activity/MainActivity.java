package com.anthony.deepl.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.anthony.deepl.R;
import com.anthony.deepl.fragment.MainFragment;

public class MainActivity extends AppCompatActivity implements MainFragment.OnFragmentInteractionListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
