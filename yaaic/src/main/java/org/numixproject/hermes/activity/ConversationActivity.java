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
import java.util.Collection;
import java.util.List;

import org.numixproject.hermes.ConversationFragment;
import org.numixproject.hermes.HomeFragment;
import org.numixproject.hermes.MainActivity;
import org.numixproject.hermes.R;
import org.numixproject.hermes.Hermes;
import org.numixproject.hermes.adapter.ConversationPagerAdapter;
import org.numixproject.hermes.adapter.MessageListAdapter;
import org.numixproject.hermes.command.CommandParser;
import org.numixproject.hermes.indicator.ConversationIndicator;
import org.numixproject.hermes.indicator.ConversationTitlePageIndicator.IndicatorStyle;
import org.numixproject.hermes.irc.IRCBinder;
import org.numixproject.hermes.irc.IRCConnection;
import org.numixproject.hermes.irc.IRCService;
import org.numixproject.hermes.listener.ConversationListener;
import org.numixproject.hermes.listener.ServerListener;
import org.numixproject.hermes.listener.SpeechClickListener;
import org.numixproject.hermes.model.Broadcast;
import org.numixproject.hermes.model.Conversation;
import org.numixproject.hermes.model.Extra;
import org.numixproject.hermes.model.Message;
import org.numixproject.hermes.model.Query;
import org.numixproject.hermes.model.Scrollback;
import org.numixproject.hermes.model.Server;
import org.numixproject.hermes.model.ServerInfo;
import org.numixproject.hermes.model.Settings;
import org.numixproject.hermes.model.Status;
import org.numixproject.hermes.model.User;
import org.numixproject.hermes.receiver.ConversationReceiver;
import org.numixproject.hermes.receiver.ServerReceiver;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.speech.RecognizerIntent;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.text.method.TextKeyListener;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import it.neokree.materialnavigationdrawer.elements.MaterialSection;
import it.neokree.materialnavigationdrawer.elements.listeners.MaterialSectionListener;


/**
 * The server view with a scrollable list of all channels
 *
 * @author Sebastian Kaspari <sebastian@yaaic.org>
 */
public class ConversationActivity extends MaterialNavigationDrawer {

    private ConversationFragment fragment = null;

    @Override
    public void init(Bundle savedInstanceState) {
        MaterialSection home = newSection("Chat", R.drawable.ic_ic_chat_24px, new ConversationFragment());
        MaterialSection addserver = newSection("Add new server", R.drawable.ic_ic_add_24px, new Intent(this, AddServerActivity.class));
        MaterialSection notifications = newSection("Snooze Notifications", R.drawable.ic_ic_notifications_off_24px, new Intent(this, SettingsActivity.class));
        MaterialSection pro = newSection("Unlock all features", R.drawable.ic_ic_vpn_key_24px,  new Intent(this, SettingsActivity.class));
        MaterialSection settings = newSection("Settings", R.drawable.ic_ic_settings_24px , new Intent(this, SettingsActivity.class));
        MaterialSection help = newSection("Help", R.drawable.ic_ic_help_24px , new Intent(this, SettingsActivity.class));
        MaterialSection about = newSection("About", R.drawable.ic_ic_info_24px , new Intent(this, AboutActivity.class));
        getSupportActionBar().setElevation(3);
        addSection(home);

        this.addSubheader("Servers");

        fragment = (ConversationFragment)home.getTargetFragment();

        // Intent to open MainActivity on "Connect to" click in sidebar.
        final Intent intent = new Intent(this, MainActivity.class);

        this.addSection(newSection("Connect to ...", R.drawable.ic_ic_swap_horiz_24px, new MaterialSectionListener() {
            @Override
            public void onClick(MaterialSection section) {
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra("OpenPane", 1);
                startActivity(intent);
            }
        }));


        addSection(addserver);


        this.addSection(newSection("Disconnect all", R.drawable.ic_ic_close_24px, new MaterialSectionListener() {
            @Override
            public void onClick(MaterialSection section) {
                ArrayList<Server> mServers = Hermes.getInstance().getServersAsArrayList();
                for (Server server : mServers) {
                    if (MainActivity.binder.getService().hasConnection(server.getId())) {
                        server.setStatus(Status.DISCONNECTED);
                        server.setMayReconnect(false);
                        MainActivity.binder.getService().getConnection(server.getId()).quitServer();
                    }
                }
                // ugly
                MainActivity.binder.getService().stopForegroundCompat(R.string.app_name);            }
        }));
        addBottomSection(notifications);
        addBottomSection(pro);
        addBottomSection(settings);
        addBottomSection(help);
        addBottomSection(about);
        setDrawerHeaderImage(R.drawable.cover);
        allowArrowAnimation();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    @Override
    public void onResume()
    {
        super.onResume();

        // Check if opened by new Room button in ServerCard
        Intent intentConversation = getIntent();
        int value = -1;
        if (null != intentConversation) {
            value = intentConversation.getIntExtra("NewRoom", -1);
        }
        if (-1 != value) {
            String channel;
            // take channel name from putExtras
            channel = getIntent().getExtras().getString("channel");
            joinNewRoom(channel);
        }
    }

    // Do method join new Room in ConversationFragment
    public void joinNewRoom(String channel){
        fragment.joinNewChannel(channel);
    }

}
