package org.numixproject.hermes;

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
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.anjlab.android.iab.v3.BillingProcessor;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.appinvite.AppInviteInvitation;
import com.melnykov.fab.FloatingActionButton;
import org.numixproject.hermes.activity.AddServerActivity;
import org.numixproject.hermes.activity.ConversationActivity;
import org.numixproject.hermes.adapter.ServerListAdapter;
import org.numixproject.hermes.utils.iap;
import org.numixproject.hermes.db.Database;
import org.numixproject.hermes.irc.IRCBinder;
import org.numixproject.hermes.irc.IRCService;
import org.numixproject.hermes.listener.ServerListener;
import org.numixproject.hermes.model.Broadcast;
import org.numixproject.hermes.model.Extra;
import org.numixproject.hermes.model.Server;
import org.numixproject.hermes.model.Status;
import org.numixproject.hermes.receiver.ServerReceiver;
import com.github.paolorotolo.expandableheightlistview.ExpandableHeightListView;


public class HomeFragment extends Fragment implements ServiceConnection, ServerListener, AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA5B4Oomgmm2D8XVSxh1DIFGtU3p1N2w6Xi2ZO7MoeZRAhvVjk3B8MfrOatlO9HfozRGhEkCkq0MfstB4Cjci3dsnYZieNmHOVYIFBWERqdwfdtnUIfI554xFsAC3Ah7PTP3MwKE7qTT1VLTTHxxsE7GH4sLtvLwrAzsVrLK+dgQk+e9bDJMvhhEPBgabRFaTvKaTtSzB/BBwrCa5mv0pte6WfrNbugFjiAJC43b7NNY2PV9UA8mukiBNZ9mPrK5fZeSEfcVqenyqbvZZG+P+O/cohAHbIEzPMuAS1EBf0VBsZtm3fjQ45PgCvEB7Ye3ucfR9BQ9ADjDwdqivExvXndQIDAQAB";

        iap inAppPayments = new iap();
        bp = inAppPayments.getBilling(super.getActivity(), key);
        bp.loadOwnedPurchasesFromGoogle();

        FrameLayout llLayout = (FrameLayout) inflater.inflate(R.layout.home_fragment, container, false);

        // Inflate the layout for this fragment
        llLayout.findViewById(R.id.home_mainFragment);
        adapter = new ServerListAdapter();

        fab = (FloatingActionButton) llLayout.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                newAddServerActivity(v);
            }
        });

        list = (ExpandableHeightListView) llLayout.findViewById(R.id.list);
        list.setAdapter(adapter);
        list.setOnItemClickListener(this);
        list.setOnItemLongClickListener(this);
        list.setExpanded(true);

        fab.attachToListView(list);

        LinearLayout inviteLayout = (LinearLayout) llLayout.findViewById(R.id.inviteButton);

        if (isGooglePlayInstalled(getActivity().getApplicationContext())) {
            inviteLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onInviteClicked();
                }
            });
        } else {
            inviteLayout.setVisibility(LinearLayout.GONE);
        }

        return llLayout;
    }


    public static boolean isGooglePlayInstalled(Context context) {
        PackageManager pm = context.getPackageManager();
        boolean app_installed = false;
        try
        {
            PackageInfo info = pm.getPackageInfo("com.android.vending", PackageManager.GET_ACTIVITIES);
            String label = (String) info.applicationInfo.loadLabel(pm);
            app_installed = (label != null && !label.equals("Market"));
        }
        catch (PackageManager.NameNotFoundException e)
        {
            app_installed = false;
        }
        return app_installed;
    }

    private void onInviteClicked() {
        Intent intent = new AppInviteInvitation.IntentBuilder("Try Hermes app")
                .setMessage("Hey, Numix Hermes IRC app for Android is really cool.\nWant to try it?")
                .setDeepLink(Uri.parse("https://play.google.com/store/apps/details?id=org.numixproject.hermes"))
                .build();
        startActivityForResult(intent, REQUEST_INVITE);
    }

    private void newAddServerActivity(View v){
        Intent intent = new Intent(super.getActivity(), AddServerActivity.class);
        startActivity(intent);
    }

    public void iap() {
        bp.purchase(super.getActivity(), "remove_ads");
    }

    public BillingProcessor getIAP() {
        return bp;
    }

    /**
     * On Destroy
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bp != null) {
            bp.release();
        }
    }

    /**
     * On resume
     */
    @Override
    public void onResume()
    {
        super.onResume();

        // Start and connect to service
        Intent intent = new Intent(super.getActivity(), IRCService.class);
        intent.setAction(IRCService.ACTION_BACKGROUND);
        super.getActivity().startService(intent);
        super.getActivity().bindService(intent, this, 0);

        receiver = new ServerReceiver(this);
        super.getActivity().registerReceiver(receiver, new IntentFilter(Broadcast.SERVER_UPDATE));

        adapter.loadServers();
    }

    /**
     * On pause
     */
    @Override
    public void onPause()
    {
        super.onPause();

        if (binder != null && binder.getService() != null) {
            binder.getService().checkServiceStatus();
        }

        super.getActivity().unbindService(this);
        super.getActivity().unregisterReceiver(receiver);
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
     * Service disconnected from Activity
     */
    @Override
    public void onServiceDisconnected(ComponentName name)
    {
        binder = null;
    }

    public boolean onMoreButtonClick(int position){
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

        AlertDialog.Builder builder = new AlertDialog.Builder(super.getActivity());
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
    };

    /**
     * On server selected
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Server server = adapter.getItem(position);

        if (server == null) {
            // "Add server" was selected
            startActivityForResult(new Intent(super.getActivity(), AddServerActivity.class), 0);
            return;
        }

        Intent intent = new Intent(super.getActivity(), ConversationActivity.class);

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
            startActivityForResult(new Intent(super.getActivity(), AddServerActivity.class), 0);
            return;
        }

        Intent intent = new Intent(super.getActivity(), ConversationActivity.class);

        if (server.getStatus() == Status.DISCONNECTED && !server.mayReconnect()) {
            server.setStatus(Status.PRE_CONNECTING);
            intent.putExtra("connect", true);
        }

        intent.putExtra("serverId", server.getId());
        startActivity(intent);
    }

    /**
     * On activity result
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
            super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != super.getActivity().RESULT_OK) {
            // ignore other result codes
            return;
        }

        switch (requestCode) {
            case 1:
                channel = data.getExtras().getString("channel");
                Intent intent = new Intent(super.getActivity(), ConversationActivity.class);
                // send position in intent.putExtra
                final Server server = adapter.getItem(positionBuffer);

                intent.putExtra("serverId", server.getId());
                // Intent wants to open another room
                intent.putExtra("NewRoom", 1);
                intent.putExtra("channel", channel);

                // starts ConversationActivity
                startActivity(intent);
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

        AlertDialog.Builder builder = new AlertDialog.Builder(super.getActivity());
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
            Toast.makeText(super.getActivity(), getResources().getString(R.string.disconnect_before_editing), Toast.LENGTH_SHORT).show();
        }
        else {
            Intent intent = new Intent(super.getActivity(), AddServerActivity.class);
            intent.putExtra(Extra.SERVER, serverId);
            startActivityForResult(intent, 0);
        }
    }

    /**
     * On menu item selected

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add:
                startActivityForResult(new Intent(super.getActivity(), AddServerActivity.class), 0);
                break;
            case R.id.about:
                startActivity(new Intent(super.getActivity(), AboutActivity.class));
                break;
            case R.id.settings:
                startActivity(new Intent(super.getActivity(), SettingsActivity.class));
                break;
            case R.id.disconnect_all:
                ArrayList<Server> mServers = Hermes.getInstance().getServersAsArrayList();
                for (Server server : mServers) {
                    if (binder.getService().hasConnection(server.getId())) {
                        server.setStatus(Status.DISCONNECTED);
                        server.setMayReconnect(false);
                        binder.getService().getConnection(server.getId()).quitServer();
                    }
                }
                // ugly
                binder.getService().stopForegroundCompat(R.string.app_name);
        }

        return super.onOptionsItemSelected(item);
    }
/*

    /**
     * Delete server
     *
     * @param serverId
     */
    public void deleteServer(int serverId)
    {
        Database db = new Database(super.getActivity());
        db.removeServerById(serverId);
        db.close();

        Hermes.getInstance().removeServerById(serverId);
        adapter.loadServers();
    }

    /**
     * On server status update
     */
    @Override
    public void onStatusUpdate()
    {
        adapter.loadServers();

        if (adapter.getCount() > 2) {
            // Hide background if there are servers in the list
            list.setBackgroundDrawable(null);
        }
    }
}