package org.numixproject.hermes;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.github.paolorotolo.expandableheightlistview.ExpandableHeightListView;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.melnykov.fab.FloatingActionButton;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.accountswitcher.AccountHeader;
import com.mikepenz.materialdrawer.accountswitcher.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.Nameable;

import org.numixproject.hermes.activity.AddServerActivity;
import org.numixproject.hermes.activity.ConversationActivity;
import org.numixproject.hermes.activity.Gitty;
import org.numixproject.hermes.adapter.ServerListAdapter;
import org.numixproject.hermes.db.Database;
import org.numixproject.hermes.irc.IRCBinder;
import org.numixproject.hermes.activity.SettingsActivity;
import org.numixproject.hermes.irc.IRCService;
import org.numixproject.hermes.listener.ServerListener;
import org.numixproject.hermes.model.Broadcast;
import org.numixproject.hermes.model.Extra;
import org.numixproject.hermes.model.Server;
import org.numixproject.hermes.model.Status;
import org.numixproject.hermes.receiver.ServerReceiver;
import org.numixproject.hermes.utils.iap;

public class MainActivity extends AppCompatActivity implements ServiceConnection, ServerListener, AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {
    private static int instanceCount = 0;
    SharedPreferences prefs = null;
    public static GoogleAnalytics analytics;
    public static Tracker tracker;
    private static final int REQUEST_INVITE = 1;
    private IRCBinder binder;
    private ServerReceiver receiver;
    private ServerListAdapter adapter;
    private ExpandableHeightListView list;
    private String channel;
    private int positionBuffer;
    private FloatingActionButton fab = null;
    String key;
    BillingProcessor bp;

    private Drawer result = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.home_fragment);

        analytics = GoogleAnalytics.getInstance(this);
        analytics.setLocalDispatchPeriod(1800);

        tracker = analytics.newTracker("UA-63953479-1");
        tracker.enableExceptionReporting(true);
        tracker.enableAdvertisingIdCollection(true);
        tracker.enableAutoActivityTracking(true);

        key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA5B4Oomgmm2D8XVSxh1DIFGtU3p1N2w6Xi2ZO7MoeZRAhvVjk3B8MfrOatlO9HfozRGhEkCkq0MfstB4Cjci3dsnYZieNmHOVYIFBWERqdwfdtnUIfI554xFsAC3Ah7PTP3MwKE7qTT1VLTTHxxsE7GH4sLtvLwrAzsVrLK+dgQk+e9bDJMvhhEPBgabRFaTvKaTtSzB/BBwrCa5mv0pte6WfrNbugFjiAJC43b7NNY2PV9UA8mukiBNZ9mPrK5fZeSEfcVqenyqbvZZG+P+O/cohAHbIEzPMuAS1EBf0VBsZtm3fjQ45PgCvEB7Ye3ucfR9BQ9ADjDwdqivExvXndQIDAQAB";
        iap inAppPayments = new iap();
        bp = inAppPayments.getBilling(this, key);
        bp.loadOwnedPurchasesFromGoogle();

        // Handle Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        AccountHeader headerResult = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.cover)
                .build();

        result = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar)
                .withTranslucentStatusBar(true)
                .withActionBarDrawerToggle(true)
                .withAccountHeader(headerResult)
                .withSelectedItem(0)
                .addDrawerItems(
                        new PrimaryDrawerItem().withName("Connect to...").withIcon(R.drawable.ic_ic_swap_horiz_24px),
                        new PrimaryDrawerItem().withName("Settings").withIcon(R.drawable.ic_ic_settings_24px)
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                                    @Override
                                    public boolean onItemClick(AdapterView<?> parent, View view, int position, long id, IDrawerItem drawerItem) {
                                        if (drawerItem instanceof Nameable) {
                                            switch (((Nameable) drawerItem).getName()) {
                                                case "Settings": {
                                                    Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                                                    startActivity(intent);
                                                    break;
                                                }
                                                case "Contact us": {
                                                    try {
                                                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("mailto:" + "team@numixproject.org"));
                                                        startActivity(intent);
                                                    } catch (Exception e) {
                                                        Toast.makeText(MainActivity.this, "A mail client is required.", Toast.LENGTH_SHORT).show();
                                                    }
                                                    break;
                                                }
                                                case "Remove ads": {
                                                    removeAds();
                                                    break;
                                                }

                                                case "Send feedback": {
                                                    Intent intent = new Intent(MainActivity.this, Gitty.class);
                                                    startActivity(intent);
                                                    break;
                                                }

                                                case "More Apps": {
                                                    String url = "https://play.google.com/store/apps/dev?id=5600498874720965803";
                                                    Intent i = new Intent(Intent.ACTION_VIEW);
                                                    i.setData(Uri.parse(url));
                                                    startActivity(i);
                                                    break;
                                                }
                                            }
                                        }
                                        return false;
                                    }
                                }).build();
        if (!inAppPayments.isPurchased()) {
            result.addItem(new PrimaryDrawerItem().withName("Remove ads").withIcon(R.drawable.ic_ic_dnd_on_24px));
        }
        result.addItem(new PrimaryDrawerItem().withName("Send feedback").withIcon(R.drawable.ic_edit_black_18dp));
        result.addItem(new PrimaryDrawerItem().withName("Contact us").withIcon(R.drawable.ic_ic_mail_24px));
        result.addItem(new PrimaryDrawerItem().withName("More Apps").withIcon(R.drawable.ic_ic_shop_24px));


        adapter = new ServerListAdapter();

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                newAddServerActivity(v);
            }
        });

        list = (ExpandableHeightListView) findViewById(R.id.list);
        list.setAdapter(adapter);
        list.setOnItemClickListener(this);
        list.setOnItemLongClickListener(this);
        list.setExpanded(true);

        fab.attachToListView(list);

        LinearLayout reportBugLayout = (LinearLayout) findViewById(R.id.reportLayout);

        reportBugLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Start Gitty Reporter
                    Intent intent = new Intent(MainActivity.this, Gitty.class);
                    startActivity(intent);
                }
            });

        if (instanceCount > 0) {
            finish();
        }

        prefs = getSharedPreferences("org.numixproject.hermes", MODE_PRIVATE);
        if (prefs.getBoolean("firstrun", true)) {
            startIntro();
            prefs.edit().putBoolean("firstrun", false).commit();
        }

        if (adapter.isServerNull()){
            reportBugLayout.setVisibility(View.GONE);
        }
    }

    private void newAddServerActivity(View v){
        Intent intent = new Intent(this, AddServerActivity.class);
        startActivity(intent);
    }

    public void removeAds() {
        bp.purchase(this, "remove_ads");
    }

    public BillingProcessor getIAP() {
        return bp;
    }

    private void startIntro() {
        Intent intent = new Intent(this, IntroActivity.class);
        startActivity(intent);
    }

    /**
     * Service connected to Activity
     */
    @Override
    public void onServiceConnected(ComponentName name, IBinder service)
    {
        binder = (IRCBinder) service;
    }

    /**
     * On server selected
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Server server = adapter.getItem(position);

        if (server == null) {
            // "Add server" was selected
            startActivityForResult(new Intent(this, AddServerActivity.class), 0);
            return;
        }

        Intent intent = new Intent(this, ConversationActivity.class);
        finish();

        if (server.getStatus() == Status.DISCONNECTED && !server.mayReconnect()) {
            server.setStatus(Status.PRE_CONNECTING);
            intent.putExtra("connect", true);
        }

        intent.putExtra("serverId", server.getId());
        startActivity(intent);
    }

    // same of OnItemClick. But opens new room too.
    public void openServer(int position) {
        Server server = adapter.getItem(position);

        if (server == null) {
            // "Add server" was selected
            startActivityForResult(new Intent(this, AddServerActivity.class), 0);
            return;
        }

        Intent intent = new Intent(this, ConversationActivity.class);

        if (server.getStatus() == Status.DISCONNECTED && !server.mayReconnect()) {
            server.setStatus(Status.PRE_CONNECTING);
            intent.putExtra("connect", true);
        }

        intent.putExtra("serverId", server.getId());
        startActivity(intent);
        finish();
    }

    /**
     * On activity result
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            try {
                // Refresh list from database
                adapter.loadServers();
            } catch (Exception e){
                e.printStackTrace();
            }
        }

        switch (requestCode) {
            case 1:
                channel = data.getExtras().getString("channel");
                Intent intent = new Intent(this, ConversationActivity.class);

                // send position in intent.putExtra
                final Server server = adapter.getItem(positionBuffer);

                intent.putExtra("serverId", server.getId());
                // Intent wants to open another room
                intent.putExtra("NewRoom", 1);
                intent.putExtra("channel", channel);

                // starts ConversationActivity
                startActivity(intent);
                finish();
                break;
        }
    }

    /**
     * On long click
     */
    @Override
    public boolean onItemLongClick(AdapterView<?> l, View v, int position, long id)
    {
        final Server server = adapter.getItem(position);

        if (server == null) {
            // "Add server" view selected
            return true;
        }

        final CharSequence[] items = {
                getString(R.string.connect),
                getString(R.string.disconnect),
                getString(R.string.edit),
                getString(R.string.delete)
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(server.getTitle());
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                switch (item) {
                    case 0: // Connect
                        if (server.getStatus() == Status.DISCONNECTED) {
                            binder.connect(server);
                            server.setStatus(Status.CONNECTING);
                            adapter.notifyDataSetChanged();
                        }
                        break;
                    case 1: // Disconnect
                        server.clearConversations();
                        server.setStatus(Status.DISCONNECTED);
                        server.setMayReconnect(false);
                        binder.getService().getConnection(server.getId()).quitServer();
                        break;
                    case 2: // Edit
                        editServer(server.getId());
                        break;
                    case 3: // Delete
                        binder.getService().getConnection(server.getId()).quitServer();
                        deleteServer(server.getId());
                        break;
                }
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
        return true;
    }

    /**
     * Start activity to edit server with given id
     *
     * @param serverId The id of the server
     */
    private void editServer(int serverId)
    {
        Server server = Hermes.getInstance().getServerById(serverId);

        if (server.getStatus() != Status.DISCONNECTED) {
            Toast.makeText(this, getResources().getString(R.string.disconnect_before_editing), Toast.LENGTH_SHORT).show();
        }
        else {
            Intent intent = new Intent(this, AddServerActivity.class);
            intent.putExtra(Extra.SERVER, serverId);
            startActivityForResult(intent, 0);
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
        result.setSelectionByIdentifier(0);
        try {
            // Find out if delete server
            Intent mIntent = getIntent();
            if (mIntent != null) {
                int serverToDelete = mIntent.getIntExtra("serverId", 0);
                deleteServer(serverToDelete);
            }
        } catch (Exception e){
            e.printStackTrace();
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
        if (bp != null)
            bp.release();
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
    public void onBackPressed() {
        //handle the back press :D close the drawer first and if the drawer is closed close the activity
        if (result != null && result.isDrawerOpen()) {
            result.closeDrawer();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        binder = null;
    }

    @Override
    public void onStatusUpdate() {
        adapter.loadServers();

        if (adapter.getCount() > 2) {
            // Hide background if there are servers in the list
            list.setBackgroundDrawable(null);
        }
    }
}
