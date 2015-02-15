package org.numixproject.hermes;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.numixproject.hermes.activity.AboutActivity;
import org.numixproject.hermes.activity.AddServerActivity;
import org.numixproject.hermes.activity.ConversationActivity;
import org.numixproject.hermes.activity.SettingsActivity;
import org.numixproject.hermes.adapter.ServerListAdapter;
import org.numixproject.hermes.db.Database;
import org.numixproject.hermes.irc.IRCBinder;
import org.numixproject.hermes.irc.IRCService;
import org.numixproject.hermes.listener.ServerListener;
import org.numixproject.hermes.model.Broadcast;
import org.numixproject.hermes.model.Extra;
import org.numixproject.hermes.model.Server;
import org.numixproject.hermes.model.Status;
import org.numixproject.hermes.receiver.ServerReceiver;

import java.util.ArrayList;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;


public class HomeFragment extends Fragment implements ServiceConnection, ServerListener, AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {

    private SlidingUpPanelLayout serverSliding = null;
    private IRCBinder binder;
    private ServerReceiver receiver;
    private ServerListAdapter adapter;
    private ListView list;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        FrameLayout llLayout = (FrameLayout) inflater.inflate(R.layout.server_sliding_fragment, container, false);

        // Inflate the layout for this fragment
        llLayout.findViewById(R.id.home_mainFragment);

        serverSliding = (SlidingUpPanelLayout) llLayout.findViewById(R.id.sliding_layout);
        serverSliding.setEnableDragViewTouchEvents(true);

        adapter = new ServerListAdapter();


        list = (ListView) llLayout.findViewById(android.R.id.list);
        list.setAdapter(adapter);
        list.setOnItemClickListener(this);
        list.setOnItemLongClickListener(this);

        return llLayout;
    }

    public void openServerPane() {
        serverSliding.expandPanel();
    }

    /**
     * On Destroy
     */
    @Override
    public void onDestroy()
    {
        super.onDestroy();
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