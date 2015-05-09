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
import org.numixproject.hermes.utils.ExpandableHeightListView;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.speech.RecognizerIntent;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.CardView;
import android.text.InputType;
import android.text.method.TextKeyListener;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.numixproject.hermes.utils.SwipeDismissListViewTouchListener;
import org.numixproject.hermes.utils.TinyDB;

import com.melnykov.fab.FloatingActionButton;

/**
 * The server view with a scrollable list of all channels
 *
 * @author Sebastian Kaspari <sebastian@yaaic.org>
 */
public class ConversationActivity extends ActionBarActivity implements ServiceConnection, ServerListener, ConversationListener
{
    public static final int REQUEST_CODE_SPEECH = 99;

    private static final int REQUEST_CODE_JOIN = 1;
    private static final int REQUEST_CODE_USERS = 2;
    private static final int REQUEST_CODE_USER = 3;
    private static final int REQUEST_CODE_NICK_COMPLETION= 4;

    private int serverId;
    private Server server;
    private IRCBinder binder;
    private ConversationReceiver channelReceiver;
    private ServerReceiver serverReceiver;

    private ViewPager pager;
    private ConversationIndicator indicator;
    private ConversationPagerAdapter pagerAdapter;
    private ArrayList<String> RoomsList;

    LinearLayout conversationLayout;
    ExpandableHeightListView roomsList;

    private Scrollback scrollback;
    private FloatingActionButton fab = null;
    mentionsAdapter roomAdapter;
    private String joinChannelBuffer;
    private int historySize;
    private boolean reconnectDialogActive = false;
    private ArrayList<String> pinnedRooms = new ArrayList<>();
    private TinyDB tinydb;

    private final OnKeyListener inputKeyListener = new OnKeyListener() {
        /**
         * On key pressed (input line)
         */
        @Override
        public boolean onKey(View view, int keyCode, KeyEvent event)
        {
            EditText input = (EditText) view;

            if (event.getAction() != KeyEvent.ACTION_DOWN) {
                return false;
            }

            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                String message = scrollback.goBack();
                if (message != null) {
                    input.setText(message);
                }
                return true;
            }

            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                String message = scrollback.goForward();
                if (message != null) {
                    input.setText(message);
                }
                return true;
            }

            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                sendMessage(input.getText().toString());

                // Workaround for a race condition in EditText
                // Instead of calling input.setText("");
                // See:
                // - https://github.com/pocmo/Yaaic/issues/67
                // - http://code.google.com/p/android/issues/detail?id=17508
                TextKeyListener.clear(input.getText());

                return true;
            }

            // Nick completion
            if (keyCode == KeyEvent.KEYCODE_SEARCH) {
                doNickCompletion(input);
                return true;
            }

            return false;
        }
    };

    /**
     * On create
     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        tinydb = new TinyDB(getApplicationContext());
        loadPinnedItems();
        serverId = getIntent().getExtras().getInt("serverId");
        server = Hermes.getInstance().getServerById(serverId);
        server.setAutoJoinChannels(pinnedRooms);

        Settings settings = new Settings(this);

        // Finish activity if server does not exist anymore - See #55
        if (server == null) {
            this.finish();
        }

        try {
        setTitle(server.getTitle());}
        catch (Exception e){}

        setContentView(R.layout.conversations);

        boolean isLandscape = (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);

        EditText input = (EditText) findViewById(R.id.input);
        input.setOnKeyListener(inputKeyListener);

        pager = (ViewPager) findViewById(R.id.pager);

        pagerAdapter = new ConversationPagerAdapter(this, server);
        pager.setAdapter(pagerAdapter);

        final float density = getResources().getDisplayMetrics().density;

        indicator = (ConversationIndicator) findViewById(R.id.titleIndicator);
        indicator.setServer(server);
        indicator.setViewPager(pager);
        indicator.setFooterColor(Color.parseColor("#d1d1d1"));
        indicator.setFooterLineHeight(1);
        indicator.setPadding(10, 10, 10, 10);
        indicator.setFooterIndicatorStyle(IndicatorStyle.Underline);
        indicator.setFooterIndicatorHeight(2 * density);
        indicator.setSelectedColor(0xFF222222);
        indicator.setSelectedBold(false);
        indicator.setBackgroundColor(Color.parseColor("#fff5f5f5"));

        historySize = settings.getHistorySize();

        if (server.getStatus() == Status.PRE_CONNECTING) {
            server.clearConversations();
            pagerAdapter.clearConversations();
            server.getConversation(ServerInfo.DEFAULT_NAME).setHistorySize(historySize);
        }

        float fontSize = settings.getFontSize();
        indicator.setTextSize(TypedValue.COMPLEX_UNIT_SP, 35);

        input.setTypeface(Typeface.SANS_SERIF);

        // Optimization : cache field lookups
        Collection<Conversation> mConversations = server.getConversations();

        for (Conversation conversation : mConversations) {
            // Only scroll to new conversation if it was selected before
            if (conversation.getStatus() == Conversation.STATUS_SELECTED) {
                onNewConversation(conversation.getName());
            } else {
                createNewConversation(conversation.getName());
            }
        }

        input.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSoftKeyboard(v);
            }

        });

        int setInputTypeFlags = 0;

        setInputTypeFlags |= InputType.TYPE_TEXT_FLAG_AUTO_CORRECT;

        if (settings.autoCapSentences()) {
            setInputTypeFlags |= InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
        }

        if (isLandscape && settings.imeExtract()) {
            setInputTypeFlags |= InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE;
        }

        if (!settings.imeExtract()) {
            input.setImeOptions(input.getImeOptions() | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        }

        input.setInputType(input.getInputType() | setInputTypeFlags);

        // Create a new scrollback history
        scrollback = new Scrollback();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Adapter section

        conversationLayout = (LinearLayout) findViewById(R.id.conversationFragment);
        conversationLayout.setVisibility(LinearLayout.GONE);

        roomsList = (ExpandableHeightListView) findViewById(R.id.roomsActivityList);
        roomsList.setExpanded(true);

        SwipeDismissListViewTouchListener touchListener =
                new SwipeDismissListViewTouchListener(
                        roomsList,
                        new SwipeDismissListViewTouchListener.DismissCallbacks() {
                            @Override
                            public boolean canDismiss(int position) {
                                return true;
                            }

                            @Override
                            public void onDismiss(ListView listView, int[] reverseSortedPositions) {
                                for (int position : reverseSortedPositions) {
                                    roomAdapter.remove(position);
                                }
                                roomAdapter.notifyDataSetChanged();
                            }
                        });
        roomsList.setOnTouchListener(touchListener);
        // Setting this scroll listener is required to ensure that during ListView scrolling,
        // we don't look for swipes.
        roomsList.setOnScrollListener(touchListener.makeScrollListener());

        RoomsList = new ArrayList<String>();
        ArrayList<Integer> MentionsList = new ArrayList<Integer>();

        ArrayList<String> channels = new ArrayList<String>();
        ArrayList<String> query = new ArrayList<String>();

        channels = server.getCurrentChannelNames();
        query = server.getCurrentQueryNames();

        for (int i = 0; i < channels.size(); i++) {
            try {
                Conversation conversation = server.getConversation(channels.get(i));
                int Mentions = conversation.getNewMentions();

                RoomsList.add(channels.get(i));
                MentionsList.add(Mentions);

            } catch (Exception E) {
                // Do nothing
            }

            // FAB section
            fab = (FloatingActionButton) findViewById(R.id.room_fab);
            fab.setOnTouchListener(new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event){
                if(event.getAction() == MotionEvent.ACTION_UP){
                    joinRoom(v);
                    return true;
                }
                return false;
            }
            });
        }

        roomAdapter = new mentionsAdapter(RoomsList, MentionsList);
        roomsList.setAdapter(roomAdapter);
        roomsList.setEmptyView(findViewById(R.id.roomsActivityList_empty));

        // Handle click on adapter
        roomsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // Set conversation VISIBLE
                conversationLayout.setVisibility(LinearLayout.VISIBLE);
                invalidateOptionsMenu();

                int pagerPosition;
                String name;

                // Find channel name from TextView
                TextView roomName = (TextView) view.findViewById(R.id.room_name);
                name = roomName.getText().toString();

                // Find room's position in pager
                pagerPosition = pagerAdapter.getPositionByName(name);

                // Set position in pager
                pager.setCurrentItem(pagerPosition, true);
            }
        });

        // Click on Others
        CardView otherCard = (CardView) findViewById(R.id.card_view_other);
        otherCard.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                // Set conversation VISIBLE
                conversationLayout.setVisibility(LinearLayout.VISIBLE);
                invalidateOptionsMenu();

                // Set position in pager
                pager.setCurrentItem(0);
            }
        });
     }

    /**
     * On resume
     */
    @Override
    public void onResume()
    {
        super.onResume();
        loadPinnedItems();
        // register the receivers as early as possible, otherwise we may loose a broadcast message
        channelReceiver = new ConversationReceiver(server.getId(), this);
        registerReceiver(channelReceiver, new IntentFilter(Broadcast.CONVERSATION_MESSAGE));
        registerReceiver(channelReceiver, new IntentFilter(Broadcast.CONVERSATION_NEW));
        registerReceiver(channelReceiver, new IntentFilter(Broadcast.CONVERSATION_REMOVE));
        registerReceiver(channelReceiver, new IntentFilter(Broadcast.CONVERSATION_TOPIC));

        serverReceiver = new ServerReceiver(this);
        registerReceiver(serverReceiver, new IntentFilter(Broadcast.SERVER_UPDATE));

        // Start service
        Intent intent = new Intent(this, IRCService.class);
        intent.setAction(IRCService.ACTION_FOREGROUND);
        startService(intent);
        bindService(intent, this, 0);

        if (!server.isConnected()) {
            ((EditText) findViewById(R.id.input)).setEnabled(false);
        } else {
            ((EditText) findViewById(R.id.input)).setEnabled(true);
        }

        // Optimization - cache field lookup
        Collection<Conversation> mConversations = server.getConversations();
        MessageListAdapter mAdapter;

        // Fill view with messages that have been buffered while paused
        for (Conversation conversation : mConversations) {
            String name = conversation.getName();
            mAdapter = pagerAdapter.getItemAdapter(name);

            if (mAdapter != null) {
                mAdapter.addBulkMessages(conversation.getBuffer());
                conversation.clearBuffer();
            } else {
                // Was conversation created while we were paused?
                if (pagerAdapter.getPositionByName(name) == -1) {
                    onNewConversation(name);
                }
            }

            // Clear new message notifications for the selected conversation
            if (conversation.getStatus() == Conversation.STATUS_SELECTED && conversation.getNewMentions() > 0) {
                Intent ackIntent = new Intent(this, IRCService.class);
                ackIntent.setAction(IRCService.ACTION_ACK_NEW_MENTIONS);
                ackIntent.putExtra(IRCService.EXTRA_ACK_SERVERID, serverId);
                ackIntent.putExtra(IRCService.EXTRA_ACK_CONVTITLE, name);
                startService(ackIntent);
            }

                // Check if intentExtra is null or empty
            if (getIntent().getStringExtra("joinChannel") != null && !getIntent().getStringExtra("joinChannel").isEmpty()){
                // if not open the new room dialog
                if (getIntent().getStringExtra("joinChannel").equals("joinChannel")) {
                    getIntent().removeExtra("joinChannel");
                    startActivityForResult(new Intent(this, JoinActivity.class), 1);
                }
            }
        }

        // Remove views for conversations that ended while we were paused
        int numViews = pagerAdapter.getCount();
        if (numViews > mConversations.size()) {
            for (int i = 0; i < numViews; ++i) {
                if (!mConversations.contains(pagerAdapter.getItem(i))) {
                    pagerAdapter.removeConversation(i--);
                    --numViews;
                }
            }
        }

        // Join channel that has been selected in JoinActivity (onActivityResult())
        if (joinChannelBuffer != null) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        binder.getService().getConnection(serverId).joinChannel(joinChannelBuffer);
                        joinChannelBuffer = null;
                    } catch (Exception E) {
                        // Do nothing
                    }
                }
            }.start();
            refreshActivity();
        }

        server.setIsForeground(true);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        savePinnedItems();
    }

    @Override
    public void onBackPressed() {
        if (conversationLayout.getVisibility() == LinearLayout.GONE) {
            finish();
        } else {
            conversationLayout.setVisibility(LinearLayout.GONE);
            refreshActivity();
            invalidateOptionsMenu();
        }
    }

    public void joinRoom(View v) {
        startActivityForResult(new Intent(this, JoinActivity.class), REQUEST_CODE_JOIN);
    }

    /**
     * On Pause
     */
    @Override
    public void onPause()
    {
        super.onPause();
        savePinnedItems();

        server.setIsForeground(false);

        if (binder != null && binder.getService() != null) {
            binder.getService().checkServiceStatus();
        }

        unbindService(this);
        unregisterReceiver(channelReceiver);
        unregisterReceiver(serverReceiver);
    }

    /**
     * On service connected
     */
    @Override
    public void onServiceConnected(ComponentName name, IBinder service)
    {
        this.binder = (IRCBinder) service;

        // connect to irc server if connect has been requested
        if (server.getStatus() == Status.PRE_CONNECTING && getIntent().hasExtra("connect")) {
            server.setStatus(Status.CONNECTING);
            binder.connect(server);
        } else {
            onStatusUpdate();
        }
    }

    /**
     * On service disconnected
     */
    @Override
    public void onServiceDisconnected(ComponentName name)
    {
        this.binder = null;
    }

    /**
     * On options menu requested
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        menu.clear();

        // Check the menu to display (Server or Conversation)
        if (conversationLayout.getVisibility() == LinearLayout.GONE) {
            // inflate Server options from xml
            MenuInflater inflater = new MenuInflater(this);
            inflater.inflate(R.menu.room_activity, menu);
        } else {
            // inflate Conversation options from xml
            MenuInflater inflater = new MenuInflater(this);
            inflater.inflate(R.menu.conversations, menu);
        }


        return super.onPrepareOptionsMenu(menu);
    }

    private void editServer()
    {
        Server server = getServer();

        if (server.getStatus() != Status.DISCONNECTED) {
            Toast.makeText(this, getResources().getString(R.string.disconnect_before_editing), Toast.LENGTH_SHORT).show();
        }
        else {
            Intent intent = new Intent(this, AddServerActivity.class);
            intent.putExtra(Extra.SERVER, serverId);
            startActivityForResult(intent, 0);
        }
    }

    private void refreshActivity() {
        Intent intent = getIntent();
        overridePendingTransition(0, 0);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        finish();

        overridePendingTransition(0, 0);
        startActivity(intent);
    }

    /**
     * On menu item selected
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case  android.R.id.home:
                if (conversationLayout.getVisibility() == LinearLayout.VISIBLE) {
                    conversationLayout.setVisibility(LinearLayout.GONE);
                    invalidateOptionsMenu();
                    refreshActivity();
                } else {
                    finish();
                }
                break;
            case R.id.refresh:
                refreshActivity();
                break;
            case R.id.edit: // Edit
                editServer();
                break;
            case R.id.delete: // Delete
                Intent deleteServer = new Intent(this, MainActivity.class);
                deleteServer.putExtra("serverId", serverId);
                startActivity(deleteServer);
                break;

            case R.id.disconnect:
                server.setStatus(Status.DISCONNECTED);
                server.setMayReconnect(false);
                binder.getService().getConnection(serverId).quitServer();
                server.clearConversations();
                setResult(RESULT_OK);
                break;

            case R.id.close:
                Conversation conversationToClose = pagerAdapter.getItem(pager.getCurrentItem());
                // Make sure we part a channel when closing the channel conversation
                if (conversationToClose.getType() == Conversation.TYPE_CHANNEL) {
                    binder.getService().getConnection(serverId).partChannel(conversationToClose.getName());
                }
                else if (conversationToClose.getType() == Conversation.TYPE_QUERY) {
                    server.removeConversation(conversationToClose.getName());
                    onRemoveConversation(conversationToClose.getName());
                } else {
                    Toast.makeText(this, getResources().getString(R.string.close_server_window), Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.join:
                startActivityForResult(new Intent(this, JoinActivity.class), REQUEST_CODE_JOIN);
                break;

            case R.id.users:
                Conversation conversationForUserList = pagerAdapter.getItem(pager.getCurrentItem());
                if (conversationForUserList.getType() == Conversation.TYPE_CHANNEL) {
                    Intent intent = new Intent(this, UsersActivity.class);
                    intent.putExtra(
                            Extra.USERS,
                            binder.getService().getConnection(server.getId()).getUsersAsStringArray(
                                    conversationForUserList.getName()
                            )
                    );
                    startActivityForResult(intent, REQUEST_CODE_USERS);
                } else {
                    Toast.makeText(this, getResources().getString(R.string.only_usable_from_channel), Toast.LENGTH_SHORT).show();
                }
                break;
        }

        return true;
    }

    /**
     * Get server object assigned to this activity
     *
     * @return the server object
     */
    public Server getServer()
    {
        return server;
    }

    /**
     * On conversation message
     */
    @Override
    public void onConversationMessage(String target)
    {
        Conversation conversation = server.getConversation(target);

        if (conversation == null) {
            // In an early state it can happen that the conversation object
            // is not created yet.
            return;
        }

        MessageListAdapter adapter = pagerAdapter.getItemAdapter(target);

        while(conversation.hasBufferedMessages()) {
            Message message = conversation.pollBufferedMessage();

            if (adapter != null && message != null) {
                if (message.hasSender()) {
                    Log.i("ConversationActivity", pagerAdapter.getPageTitle(pager.getCurrentItem()));
                    Log.i("ConversationActivity", message.getSender());
                    try {
                        // Check if sender is Op
                        if (binder.getService().getConnection(serverId).getUser(target, message.getSender()).isOp()) {
                            // If yes, display an appropriate icon
                            message.setIcon(R.drawable.ic_ic_sms_failed_24px);
                            adapter.addMessageCard(message);
                        } else if (binder.getService().getConnection(serverId).getUser(target, message.getSender()).hasVoice()){
                            message.setIcon(R.drawable.ic_ic_mic_24px);
                            adapter.addMessageCard(message);
                        } else {
                            adapter.addMessageCard(message);
                        }
                    } catch (Exception E){
                        // Do nothing
                    }
                } else {
                    adapter.addMessage(message);
                }

                int status;

                switch (message.getType())
                {
                    case Message.TYPE_MISC:
                        status = Conversation.STATUS_MISC;
                        break;

                    default:
                        status = Conversation.STATUS_MESSAGE;
                        break;
                }
                conversation.setStatus(status);
            }
        }

        indicator.updateStateColors();
    }

    /**
     * On new conversation
     */
    @Override
    public void onNewConversation(String target)
    {
        createNewConversation(target);

        pager.setCurrentItem(pagerAdapter.getCount() - 1);
    }

    /**
     * Create a new conversation in the pager adapter for the
     * given target conversation.
     *
     * @param target
     */
    public void createNewConversation(String target)
    {
        pagerAdapter.addConversation(server.getConversation(target));
    }

    /**
     * On conversation remove
     */
    @Override
    public void onRemoveConversation(String target)
    {
        int position = pagerAdapter.getPositionByName(target);

        if (position != -1) {
            pagerAdapter.removeConversation(position);
        }
    }

    /**
     * On topic change
     */
    @Override
    public void onTopicChanged(String target)
    {
        // No implementation
    }

    /**
     * On server status update
     */
    @Override
    public void onStatusUpdate()
    {
        EditText input = (EditText) findViewById(R.id.input);

        if (server.isConnected()) {
            input.setEnabled(true);
        } else {
            input.setEnabled(false);

            if (server.getStatus() == Status.CONNECTING) {
                return;
            }

            // Service is not connected or initialized yet - See #54
            if (binder == null || binder.getService() == null || binder.getService().getSettings() == null) {
                return;
            }

            if (!binder.getService().getSettings().isReconnectEnabled() && !reconnectDialogActive) {
                reconnectDialogActive = true;
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(getResources().getString(R.string.reconnect_after_disconnect, server.getTitle()))
                        .setCancelable(false)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                if (!server.isDisconnected()) {
                                    reconnectDialogActive = false;
                                    return;
                                }

                                binder.getService().getConnection(server.getId()).setAutojoinChannels(
                                        server.getCurrentChannelNames()
                                );
                                server.setStatus(Status.CONNECTING);
                                binder.connect(server);
                                reconnectDialogActive = false;
                            }
                        })
                        .setNegativeButton(getString(R.string.negative_button), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                server.setMayReconnect(false);
                                reconnectDialogActive = false;
                                dialog.cancel();
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
            }
        }
    }

    /**
     * On activity result
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (resultCode != RESULT_OK) {
            // ignore other result codes
            return;
        }

        switch (requestCode) {
            case REQUEST_CODE_SPEECH:
                ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (matches.size() > 0) {
                    ((EditText) findViewById(R.id.input)).setText(matches.get(0));
                }
                break;
            case REQUEST_CODE_JOIN:
                joinChannelBuffer = data.getExtras().getString("channel");
                break;
            case REQUEST_CODE_USERS:
                Intent intent = new Intent(this, UserActivity.class);
                intent.putExtra(Extra.USER, data.getStringExtra(Extra.USER));
                startActivityForResult(intent, REQUEST_CODE_USER);
                break;
            case REQUEST_CODE_NICK_COMPLETION:
                insertNickCompletion((EditText) findViewById(R.id.input), data.getExtras().getString(Extra.USER));
                break;
            case REQUEST_CODE_USER:
                final int actionId = data.getExtras().getInt(Extra.ACTION);
                final String nickname = data.getExtras().getString(Extra.USER);
                final IRCConnection connection = binder.getService().getConnection(server.getId());
                final String conversation = server.getSelectedConversation();
                final Handler handler = new Handler();

                // XXX: Implement me - The action should be handled after onResume()
                //                     to catch the broadcasts... now we just wait a second
                // Yes .. that's very ugly - we need some kind of queue that is handled after onResume()

                new Thread() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            // Do nothing
                        }

                        String nicknameWithoutPrefix = nickname;

                        while (
                                nicknameWithoutPrefix.startsWith("@") ||
                                        nicknameWithoutPrefix.startsWith("+") ||
                                        nicknameWithoutPrefix.startsWith(".") ||
                                        nicknameWithoutPrefix.startsWith("%")
                                ) {
                            // Strip prefix(es) now
                            nicknameWithoutPrefix = nicknameWithoutPrefix.substring(1);
                        }

                        switch (actionId) {
                            case User.ACTION_REPLY:
                                final String replyText = nicknameWithoutPrefix + ": ";
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        EditText input = (EditText) findViewById(R.id.input);
                                        input.setText(replyText);
                                        input.setSelection(replyText.length());
                                    }
                                });
                                break;
                            case User.ACTION_QUERY:
                                Conversation query = server.getConversation(nicknameWithoutPrefix);
                                if (query == null) {
                                    // Open a query if there's none yet
                                    query = new Query(nicknameWithoutPrefix);
                                    query.setHistorySize(binder.getService().getSettings().getHistorySize());
                                    server.addConversation(query);

                                    Intent intent = Broadcast.createConversationIntent(
                                            Broadcast.CONVERSATION_NEW,
                                            server.getId(),
                                            nicknameWithoutPrefix
                                    );
                                    binder.getService().sendBroadcast(intent);
                                }
                                break;
                            case User.ACTION_OP:
                                connection.op(conversation, nicknameWithoutPrefix);
                                break;
                            case User.ACTION_DEOP:
                                connection.deOp(conversation, nicknameWithoutPrefix);
                                break;
                            case User.ACTION_VOICE:
                                connection.voice(conversation, nicknameWithoutPrefix);
                                break;
                            case User.ACTION_DEVOICE:
                                connection.deVoice(conversation, nicknameWithoutPrefix);
                                break;
                            case User.ACTION_KICK:
                                connection.kick(conversation, nicknameWithoutPrefix);
                                break;
                            case User.ACTION_BAN:
                                connection.ban(conversation, nicknameWithoutPrefix + "!*@*");
                                break;
                        }
                    }
                }.start();

                break;
        }
    }

    /**
     * Send a message in this conversation
     *
     * @param text The text of the message
     */
    private void sendMessage(String text) {
        if (text.equals("")) {
            // ignore empty messages
            return;
        }

        if (!server.isConnected()) {
            Message message = new Message(getString(R.string.message_not_connected));
            message.setColor(Message.COLOR_RED);
            message.setIcon(R.drawable.error);
            server.getConversation(server.getSelectedConversation()).addMessage(message);
            onConversationMessage(server.getSelectedConversation());
        }

        scrollback.addMessage(text);

        Conversation conversation = pagerAdapter.getItem(pager.getCurrentItem());

        if (conversation != null) {
            if (!text.trim().startsWith("/")) {
                if (conversation.getType() != Conversation.TYPE_SERVER) {
                    String nickname = binder.getService().getConnection(serverId).getNick();
                    conversation.addMessage(new Message(" " + nickname + " - " + text));
                    // conversation.addMessage(new Message(text, nickname));
                    binder.getService().getConnection(serverId).sendMessage(conversation.getName(), text);
                } else {
                    Message message = new Message(getString(R.string.chat_only_form_channel));
                    message.setColor(Message.COLOR_YELLOW);
                    message.setIcon(R.drawable.warning);
                    conversation.addMessage(message);
                }
                onConversationMessage(conversation.getName());
            } else {
                CommandParser.getInstance().parse(text, server, conversation, binder.getService());
            }
        }
    }

    /**
     * Complete a nick in the input line
     */
    private void doNickCompletion(EditText input) {
        String text = input.getText().toString();

        if (text.length() <= 0) {
            return;
        }

        String[] tokens = text.split("[\\s,.-]+");

        if (tokens.length <= 0) {
            return;
        }

        String word = tokens[tokens.length - 1].toLowerCase();
        tokens[tokens.length - 1] = null;

        int begin   = input.getSelectionStart();
        int end     = input.getSelectionEnd();
        int cursor  = Math.min(begin, end);
        int sel_end = Math.max(begin, end);

        boolean in_selection = (cursor != sel_end);

        if (in_selection) {
            word = text.substring(cursor, sel_end);
        } else {
            // use the word at the curent cursor position
            while (true) {
                cursor -= 1;
                if (cursor <= 0 || text.charAt(cursor) == ' ') {
                    break;
                }
            }

            if (cursor < 0) {
                cursor = 0;
            }

            if (text.charAt(cursor) == ' ') {
                cursor += 1;
            }

            sel_end = text.indexOf(' ', cursor);

            if (sel_end == -1) {
                sel_end = text.length();
            }

            word = text.substring(cursor, sel_end);
        }
        // Log.d("Yaaic", "Trying to complete nick: " + word);

        Conversation conversationForUserList = pagerAdapter.getItem(pager.getCurrentItem());

        String[] users = null;

        if (conversationForUserList.getType() == Conversation.TYPE_CHANNEL) {
            users = binder.getService().getConnection(server.getId()).getUsersAsStringArray(
                    conversationForUserList.getName()
            );
        }

        // go through users and add matches
        if (users != null) {
            List<Integer> result = new ArrayList<Integer>();

            for (int i = 0; i < users.length; i++) {
                String nick = removeStatusChar(users[i].toLowerCase());
                if (nick.startsWith(word.toLowerCase())) {
                    result.add(Integer.valueOf(i));
                }
            }

            if (result.size() == 1) {
                input.setSelection(cursor, sel_end);
                insertNickCompletion(input, users[result.get(0).intValue()]);
            } else if (result.size() > 0) {
                Intent intent  = new Intent(this, UsersActivity.class);
                String[] extra = new String[result.size()];
                int i = 0;

                for (Integer n : result) {
                    extra[i++] = users[n.intValue()];
                }

                input.setSelection(cursor, sel_end);
                intent.putExtra(Extra.USERS, extra);
                startActivityForResult(intent, REQUEST_CODE_NICK_COMPLETION);
            }
        }
    }

    /**
     * Insert a given nick completion into the input line
     *
     * @param input The input line widget, with the incomplete nick selected
     * @param nick The completed nick
     */
    private void insertNickCompletion(EditText input, String nick) {
        int start = input.getSelectionStart();
        int end  = input.getSelectionEnd();
        nick = removeStatusChar(nick);

        if (start == 0) {
            nick += ":";
        }

        nick += " ";
        input.getText().replace(start, end, nick, 0, nick.length());
        // put cursor after inserted text
        input.setSelection(start + nick.length());
        input.clearComposingText();
        input.post(new Runnable() {
            @Override
            public void run() {
                // make the softkeyboard come up again (only if no hw keyboard is attached)
                EditText input = (EditText) findViewById(R.id.input);
                openSoftKeyboard(input);
            }
        });

        input.requestFocus();
    }

    /**
     * Open the soft keyboard (helper function)
     */
    private void openSoftKeyboard(View view) {
        ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);    }

    /**
     * Remove the status char off the front of a nick if one is present
     *
     * @param nick
     * @return nick without statuschar
     */
    private String removeStatusChar(String nick)
    {
        /* Discard status characters */
        if (nick.startsWith("@") || nick.startsWith("+")
                || nick.startsWith("%")) {
            nick = nick.substring(1);
        }
        return nick;
    }

    private void savePinnedItems(){
        tinydb.putListString("pinned", pinnedRooms);
    }

    private void loadPinnedItems(){
        pinnedRooms = tinydb.getListString("pinned");
    }

    // Adapter for Room List
    class mentionsAdapter extends BaseAdapter {
        ArrayList<String> Room;
        ArrayList<Integer> Mentions;

        mentionsAdapter() {
            Room = null;
            Mentions = null;
        }

        public mentionsAdapter(ArrayList<String> text, ArrayList<Integer> text1) {
            Room = text;
            Mentions = text1;
        }

        public void remove(int position) {

            int pagerPosition;

            // Find room's position in pager
            pagerPosition = pagerAdapter.getPositionByName(Room.get(position));

            Conversation conversationToClose = pagerAdapter.getItem(pagerPosition);
            binder.getService().getConnection(serverId).partChannel(conversationToClose.getName());

            pinnedRooms.remove(Room.get(position));
            Room.remove(position);
            Mentions.remove(position);
            savePinnedItems();
        }

        public int getCount() {
            // TODO Auto-generated method stub
            return Room.size();
        }

        public Object getItem(int arg0) {
            // TODO Auto-generated method stub
            return null;
        }

        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return position;
        }

        public int getPosition(String roomName){
            int position = pagerAdapter.getPositionByName(roomName);
            // Because first page is Server log
            return position-1;
        }

        public View getViewByPosition(int pos, ListView listView) {
            final int firstListItemPosition = listView.getFirstVisiblePosition();
            final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

            if (pos < firstListItemPosition || pos > lastListItemPosition ) {
                return listView.getAdapter().getView(pos, null, listView);
            } else {
                final int childIndex = pos - firstListItemPosition;
                return listView.getChildAt(childIndex);
            }
        }

        public View getView(final int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View row;
            row = inflater.inflate(R.layout.rooms_activity_item, parent, false);
            final TextView room, mentions;
            room = (TextView) row.findViewById(R.id.room_name);
            mentions = (TextView) row.findViewById(R.id.mentions_number);
            room.setText(Room.get(position));

            try {
                mentions.setText("" + Mentions.get(position));
            } catch (Exception E) {
                // Do nothing
            }

            final ImageView star = (ImageView) row.findViewById(R.id.star);

            if (RoomsList.size() != 0) {
                int i;
                for (i = 0; i < pinnedRooms.size(); i++) {
                    loadPinnedItems();
                    String roomName = pinnedRooms.get(i);
                    if (roomName.equals(Room.get(position)))
                        star.setImageResource(R.drawable.ic_ic_star_rate_yellow_24px);
                }
            }

            // ---- Pinned rooms section ----
            try {
                // Handle clicks on star.
                star.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String pinnedRoomName = Room.get(position);
                        // Check if room is already pinned
                        if (pinnedRooms.contains(pinnedRoomName)) {
                            pinnedRooms.remove(pinnedRoomName);
                            star.setImageResource(R.drawable.ic_ic_star_rate_24px);
                            savePinnedItems();
                        } else {
                            pinnedRooms.add(pinnedRoomName);
                            star.setImageResource(R.drawable.ic_ic_star_rate_yellow_24px);
                            savePinnedItems();
                        }
                    }
                });
            } catch (Exception e) {
                // Do nothing
            }

            return (row);
        }
    }
}