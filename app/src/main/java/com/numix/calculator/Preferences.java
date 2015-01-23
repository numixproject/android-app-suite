package com.numix.calculator;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;

import com.numix.calculator.view.PreferencesFragment;

/**
 * @author Will Harmon
 **/
public class Preferences extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(CalculatorSettings.useLightTheme(this)) {
            super.setTheme(R.style.Theme_Settings_Calculator_Light);
        }

        if(savedInstanceState == null) {
            PreferencesFragment fragment = new PreferencesFragment();
            fragment.setArguments(getIntent().getExtras());
            getFragmentManager().beginTransaction().add(android.R.id.content, fragment).commit();
        }

        ActionBar mActionBar = getActionBar();
        if (mActionBar != null) {
            mActionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    // Handle back button press
    @Override
    public void onBackPressed() {

            startActivity(new Intent(this, AdMob.class));
            finish();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            startActivity(new Intent(this, AdMob.class));
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
