package com.anthony.deepl.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;

import com.anthony.deepl.R;
import com.anthony.deepl.fragment.MainFragment;

public class MainActivity extends AppCompatActivity implements MainFragment.OnFragmentInteractionListener {

    private MainFragment mMainFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();

        // We handle possible share text intent
        Intent intent = getIntent();
        String intentAction = intent.getAction();
        String intentType = intent.getType();
        if (intentAction != null && intentAction.equals(Intent.ACTION_SEND) &&
                intentType != null && intentType.equals("text/plain")) {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (sharedText != null) {
                mMainFragment.setSharedText(sharedText);
            }

        }
    }

    private void initViews() {
        // Toolbar setup
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mMainFragment = (MainFragment) getSupportFragmentManager().findFragmentById(R.id.main_fragment);
    }

}
