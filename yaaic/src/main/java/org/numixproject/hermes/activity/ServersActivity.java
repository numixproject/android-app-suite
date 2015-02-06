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

import java.util.ArrayList;

import org.numixproject.hermes.HomeFragment;
import org.numixproject.hermes.R;
import org.numixproject.hermes.Hermes;
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
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;

/**
 * List of servers
 *
 * @author Sebastian Kaspari <sebastian@yaaic.org>
 */
public class ServersActivity extends Fragment implements ServiceConnection, ServerListener, OnItemClickListener, OnItemLongClickListener {
    private IRCBinder binder;
    private ServerReceiver receiver;
    private ServerListAdapter adapter;
    private ListView list;
    private static int instanceCount = 0;
    private SlidingUpPanelLayout serverSliding = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FragmentActivity faActivity  = (FragmentActivity)    super.getActivity();
        FrameLayout llLayout    = (FrameLayout)    inflater.inflate(R.layout.servers, container, false);

        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.home_fragment, new HomeFragment(), "serverFragment");
        ft.commit();

        HomeFragment fragB = (HomeFragment) getFragmentManager().findFragmentByTag("serverFragment");

        View V = inflater.inflate(R.layout.server_sliding_fragment, container, false);

        adapter = new ServerListAdapter();

        list = (ListView) V.findViewById(android.R.id.list);
        list.setAdapter(adapter);
        list.setOnItemClickListener(this);
        list.setOnItemLongClickListener(this);

        llLayout.findViewById(R.id.mainLayout);

        serverSliding = (SlidingUpPanelLayout) llLayout.findViewById(R.id.sliding_layout);
        serverSliding.setEnableDragViewTouchEvents(true);

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
        instanceCount--;
    }

    /**
     * On resume
     */
    @Override
    public void onResume()
    {
        super.onResume();

        ((MaterialNavigationDrawer)this.getActivity()).changeToolbarColor(Color.rgb(255,152,0),
                Color.rgb(251,140,0)
        );

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
     */
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