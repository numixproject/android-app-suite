package org.numixproject.hermes;

import android.app.Fragment;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SlidingPaneLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.numixproject.hermes.activity.ServersActivity;
import org.numixproject.hermes.adapter.ServerListAdapter;
import org.numixproject.hermes.irc.IRCBinder;
import org.numixproject.hermes.irc.IRCService;
import org.numixproject.hermes.activity.AboutActivity;
import org.numixproject.hermes.activity.AddServerActivity;
import org.numixproject.hermes.activity.SettingsActivity;
import org.numixproject.hermes.model.Server;
import org.numixproject.hermes.model.Status;
import org.numixproject.hermes.receiver.ServerReceiver;

import java.util.ArrayList;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import it.neokree.materialnavigationdrawer.elements.MaterialSection;
import it.neokree.materialnavigationdrawer.elements.listeners.MaterialSectionListener;

public class MainActivity extends MaterialNavigationDrawer implements ServiceConnection {
    private static int instanceCount = 0;
    private IRCBinder binder;
    private ServerReceiver receiver;
    private ServerListAdapter adapter;

    private SlidingUpPanelLayout serverSliding = null;


    @Override
    public void init(Bundle savedInstanceState) {
        allowArrowAnimation();
        MaterialSection home = newSection("Home", new ServersActivity());
        MaterialSection addserver = newSection("Add new Server", new Intent(this, AddServerActivity.class));
        MaterialSection settings = newSection("Settings", R.drawable.ic_ic_settings_24px , new Intent(this, SettingsActivity.class));
        MaterialSection help = newSection("Help", R.drawable.ic_ic_help_24px , new Intent(this, SettingsActivity.class));
        MaterialSection about = newSection("About", R.drawable.ic_ic_info_24px , new Intent(this, SettingsActivity.class));

        addSection(home);
        addSection(addserver);
        addBottomSection(settings);
        addBottomSection(help);
        addBottomSection(about);
        setDrawerHeaderImage(R.drawable.cover);


        this.addSection(newSection("Servers List", new MaterialSectionListener() {
            @Override
            public void onClick(MaterialSection section) {
                FragmentManager fm = getSupportFragmentManager();

//if you added fragment via layout xml
                HomeFragment fragment = (HomeFragment)fm.findFragmentByTag("serverFragment");
                fragment.openServerPane();
            }
        }));





             /*
         * With activity:launchMode = standard, we get duplicated activities
         * depending on the task the app was started in. In order to avoid
         * stacking up of this duplicated activities we keep a count of this
         * root activity and let it finish if it already exists
         *
         * Launching the app via the notification icon creates a new task,
         * and there doesn't seem to be a way around this so this is needed
         */

        if (instanceCount > 0) {
            finish();
        }
        instanceCount++;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        instanceCount--;
    }

    /**
     * Options Menu (Menu Button pressed)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);

        // inflate from xml
        MenuInflater inflater = new MenuInflater(this);
        inflater.inflate(R.menu.servers, menu);

        return super.onCreateOptionsMenu(menu);
    }

    /**
     * On menu item selected
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add:
                startActivityForResult(new Intent(this, AddServerActivity.class), 0);
                break;
            case R.id.about:
                startActivity(new Intent(this, AboutActivity.class));
                break;
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
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
     * On activity result
     */
    @Override
         protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (resultCode == RESULT_OK) {
            // Refresh list from database
            adapter.loadServers();
        }
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

}
