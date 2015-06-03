package org.numixproject.hermes;

import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;

import com.anjlab.android.iab.v3.BillingProcessor;

import org.numixproject.hermes.adapter.ServerListAdapter;
import org.numixproject.hermes.db.Database;
import org.numixproject.hermes.irc.IRCBinder;
import org.numixproject.hermes.activity.SettingsActivity;
import org.numixproject.hermes.irc.IRCService;
import org.numixproject.hermes.listener.ServerListener;
import org.numixproject.hermes.model.Broadcast;
import org.numixproject.hermes.receiver.ServerReceiver;
import org.numixproject.hermes.utils.iap;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import it.neokree.materialnavigationdrawer.elements.MaterialSection;
import it.neokree.materialnavigationdrawer.elements.listeners.MaterialSectionListener;

public class MainActivity extends MaterialNavigationDrawer implements ServiceConnection, ServerListener {
    private static int instanceCount = 0;
    public static IRCBinder binder;
    private ServerReceiver receiver;
    private ServerListAdapter adapter;
    private HomeFragment fragment = null;
    SharedPreferences prefs = null;
    String key;
    BillingProcessor bp;

    @Override
    public void init(Bundle savedInstanceState) {
        MaterialSection home = newSection("Connect to...", R.drawable.ic_ic_swap_horiz_24px, new HomeFragment());
        MaterialSection settings = newSection("Settings", R.drawable.ic_ic_settings_24px , new Intent(this, SettingsActivity.class));
        getSupportActionBar().setElevation(3);
        addSection(home);

        key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA5B4Oomgmm2D8XVSxh1DIFGtU3p1N2w6Xi2ZO7MoeZRAhvVjk3B8MfrOatlO9HfozRGhEkCkq0MfstB4Cjci3dsnYZieNmHOVYIFBWERqdwfdtnUIfI554xFsAC3Ah7PTP3MwKE7qTT1VLTTHxxsE7GH4sLtvLwrAzsVrLK+dgQk+e9bDJMvhhEPBgabRFaTvKaTtSzB/BBwrCa5mv0pte6WfrNbugFjiAJC43b7NNY2PV9UA8mukiBNZ9mPrK5fZeSEfcVqenyqbvZZG+P+O/cohAHbIEzPMuAS1EBf0VBsZtm3fjQ45PgCvEB7Ye3ucfR9BQ9ADjDwdqivExvXndQIDAQAB";
        iap inAppPayments = new iap();
        bp = inAppPayments.getBilling(this, key);
        bp.loadOwnedPurchasesFromGoogle();

        fragment = (HomeFragment) home.getTargetFragment();

        // Add only in DEBUG release
        //this.addSection(newSection("Help", R.drawable.ic_ic_help_24px, new MaterialSectionListener() {
        //    @Override
        //    public void onClick(MaterialSection section) {
        //    startIntro();
        //    }
        //}));

        if (!bp.isPurchased("remove_ads")) {
            this.addBottomSection(newSection("Remove Ads", R.drawable.ic_ic_dnd_on_24px, new MaterialSectionListener() {
                @Override
                public void onClick(MaterialSection section) {
                    fragment.iap();
                }
            }));
        }

        addSection(settings);

        this.addSection(newSection("Contact us", R.drawable.ic_ic_mail_24px, new MaterialSectionListener() {
            @Override
            public void onClick(MaterialSection section) {
                Intent intent = new Intent (Intent.ACTION_VIEW , Uri.parse("mailto:" + "team@numixproject.org"));
                startActivity(intent);
            }
        }));

        this.addBottomSection(newSection("More apps...", R.drawable.ic_ic_shop_24px, new MaterialSectionListener() {
            @Override
            public void onClick(MaterialSection section) {
                String url = "https://play.google.com/store/apps/dev?id=5600498874720965803";
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            }
        }));

        setDrawerHeaderImage(R.drawable.cover);
        allowArrowAnimation();

        if (instanceCount > 0) {
            finish();
        }

        prefs = getSharedPreferences("org.numixproject.hermes", MODE_PRIVATE);
        if (prefs.getBoolean("firstrun", true)) {
            startIntro();
            prefs.edit().putBoolean("firstrun", false).commit();
        }

    }

    private void startIntro() {
        Intent intent = new Intent(this, IntroActivity.class);
        startActivity(intent);
    }

    public void openServer(int position) {
        fragment.openServer(position);
    }

    public void onCardMoreClicked(int position){
        fragment.onMoreButtonClick(position);
    }

    private void newServerActivity() {

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Intent intent = getIntent();
        overridePendingTransition(0, 0);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        finish();
        overridePendingTransition(0, 0);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            // Find out if delete server
            Intent mIntent = getIntent();
            if (mIntent != null) {
                int serverToDelete = mIntent.getIntExtra("serverId", 0);
                deleteServer(serverToDelete);
            }
        } catch (Exception e){
            // Do nothing
        }

        // Start and connect to service
        Intent intent = new Intent(this, IRCService.class);
        intent.setAction(IRCService.ACTION_BACKGROUND);
        startService(intent);
        bindService(intent, this, 0);
        receiver = new ServerReceiver(this);
        registerReceiver(receiver, new IntentFilter(Broadcast.SERVER_UPDATE));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // inflate from xml
        MenuInflater inflater = new MenuInflater(this);
        inflater.inflate(R.menu.servers, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instanceCount--;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (binder != null && binder.getService() != null) {
            binder.getService().checkServiceStatus();
        }

        unbindService(this);
        unregisterReceiver(receiver);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        binder = (IRCBinder) service;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        binder = null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (resultCode == RESULT_OK) {
            try {
                // Refresh list from database
                adapter.loadServers();
            } catch (Exception e){};
        }
    }

    public void deleteServer(int serverId)
    {
        Database db = new Database(this);
        db.removeServerById(serverId);
        db.close();

        Hermes.getInstance().removeServerById(serverId);
        adapter.loadServers();
    }

    @Override
    public void onStatusUpdate() {
    }
}
