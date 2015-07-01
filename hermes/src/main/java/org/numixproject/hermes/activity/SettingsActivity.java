/*
Yaaic - Yet Another Android IRC Client

Copyright 2009-2013 Sebastian Kaspari

This file is part of Yaaic.

Yaaic is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Yaaic is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Yaaic.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.numixproject.hermes.activity;

import org.numixproject.hermes.R;
import org.numixproject.hermes.utils.iap;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.facebook.FacebookSdk;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

/**
 * Settings
 *
 * @author Sebastian Kaspari <sebastian@yaaic.org>
 */
public class SettingsActivity extends ActionBarActivity {

    InterstitialAd mInterstitialAd;
    BillingProcessor bp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA5B4Oomgmm2D8XVSxh1DIFGtU3p1N2w6Xi2ZO7MoeZRAhvVjk3B8MfrOatlO9HfozRGhEkCkq0MfstB4Cjci3dsnYZieNmHOVYIFBWERqdwfdtnUIfI554xFsAC3Ah7PTP3MwKE7qTT1VLTTHxxsE7GH4sLtvLwrAzsVrLK+dgQk+e9bDJMvhhEPBgabRFaTvKaTtSzB/BBwrCa5mv0pte6WfrNbugFjiAJC43b7NNY2PV9UA8mukiBNZ9mPrK5fZeSEfcVqenyqbvZZG+P+O/cohAHbIEzPMuAS1EBf0VBsZtm3fjQ45PgCvEB7Ye3ucfR9BQ9ADjDwdqivExvXndQIDAQAB";

        //Initialize Facebook SDK
        FacebookSdk.sdkInitialize(getApplicationContext());

        iap inAppPayments = new iap();
        bp = inAppPayments.getBilling(this, key);
        bp.loadOwnedPurchasesFromGoogle();
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId("ca-app-pub-2834532364021285/4571969451");
        requestNewInterstitial();

        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                requestNewInterstitial();
                finish();
            }
        });

        setContentView(R.layout.preferences);

        getFragmentManager().beginTransaction()
                .replace(R.id.preferencesFrame, new MyPreferenceFragment()).commit();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }


    public static class MyPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.preferences);
        }
    }

    private void requestNewInterstitial() {
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("YOUR_DEVICE_HASH")
                .build();

        mInterstitialAd.loadAd(adRequest);
    }

    private void showAd() {
        if (!bp.isPurchased("remove_ads")) {
            if (mInterstitialAd.isLoaded()) {
                mInterstitialAd.show();
            } else {
                finish();
            }
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed()
    {
        showAd();
    }

    /**
     * On menu item selected.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() ==  android.R.id.home) {
            showAd();
        }
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bp != null) {
            bp.release();
        }
    }

}
