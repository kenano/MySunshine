package com.kenano.android.mysunshine;

import android.os.Bundle;
import android.preference.PreferenceActivity;


/**
 * Created by KenanO on 8/25/16.
 */
public class SettingsActivity extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment()).commit();

    }

}

