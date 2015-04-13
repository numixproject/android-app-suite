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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.numixproject.hermes.MainActivity;
import org.numixproject.hermes.R;
import org.numixproject.hermes.Hermes;
import org.numixproject.hermes.db.Database;
import org.numixproject.hermes.exception.ValidationException;
import org.numixproject.hermes.model.Authentication;
import org.numixproject.hermes.model.Extra;
import org.numixproject.hermes.model.Identity;
import org.numixproject.hermes.model.Server;
import org.numixproject.hermes.model.Status;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Add a new server to the list
 *
 * @author Sebastian Kaspari <sebastian@yaaic.org>
 */
public class AddServerActivity extends ActionBarActivity implements OnClickListener {

    private static final int REQUEST_CODE_CHANNELS       = 1;
    private static final int REQUEST_CODE_COMMANDS       = 2;
    private static final int REQUEST_CODE_AUTHENTICATION = 4;

    private Server server;
    private Authentication authentication;
    private ArrayList<String> aliases;
    private ArrayList<String> channels;
    private ArrayList<String> commands;

    private CheckBox nickservCheckbox;
    private CheckBox saslCheckbox;
    private TextView saslUsernameLabel;
    private EditText saslUsernameEditText;
    private TextView saslPasswordLabel;
    private EditText saslPasswordEditText;

    /**
     * On create
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.serveradd);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        authentication = new Authentication();
        aliases = new ArrayList<String>();
        channels = new ArrayList<String>();
        commands = new ArrayList<String>();

        ((Button) findViewById(R.id.add)).setOnClickListener(this);
        ((Button) findViewById(R.id.cancel)).setOnClickListener(this);
        ((Button) findViewById(R.id.channels)).setOnClickListener(this);
        ((Button) findViewById(R.id.commands)).setOnClickListener(this);

        Spinner spinner = (Spinner) findViewById(R.id.charset);
        String[] charsets = getResources().getStringArray(R.array.charsets);
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, charsets);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey(Extra.SERVER)) {
            setTitle(R.string.edit_server_label);

            // Request to edit an existing server
            Database db = new Database(this);
            this.server = db.getServerById(extras.getInt(Extra.SERVER));
            aliases.addAll(server.getIdentity().getAliases());
            this.channels = db.getChannelsByServerId(server.getId());
            this.commands = db.getCommandsByServerId(server.getId());
            this.authentication = server.getAuthentication();
            db.close();

            // Set server values
            ((EditText) findViewById(R.id.title)).setText(server.getTitle());
            ((EditText) findViewById(R.id.host)).setText(server.getHost());
            ((EditText) findViewById(R.id.port)).setText(String.valueOf(server.getPort()));
            ((EditText) findViewById(R.id.password)).setText(server.getPassword());

            ((EditText) findViewById(R.id.nickname)).setText(server.getIdentity().getNickname());
            ((EditText) findViewById(R.id.ident)).setText(server.getIdentity().getIdent());
            ((EditText) findViewById(R.id.realname)).setText(server.getIdentity().getRealName());
            ((CheckBox) findViewById(R.id.useSSL)).setChecked(server.useSSL());

            // Select charset
            if (server.getCharset() != null) {
                for (int i = 0; i < charsets.length; i++) {
                    if (server.getCharset().equals(charsets[i])) {
                        spinner.setSelection(i);
                        break;
                    }
                }
            }
        }

        // Disable suggestions for host name
        if (android.os.Build.VERSION.SDK_INT >= 5) {
            EditText serverHostname = (EditText) findViewById(R.id.host);
            serverHostname.setInputType(0x80000);
        }

        Uri uri = getIntent().getData();
        if (uri != null && uri.getScheme().equals("irc")) {
            // handling an irc:// uri

            ((EditText) findViewById(R.id.host)).setText(uri.getHost());
            if (uri.getPort() != -1) {
                ((EditText) findViewById(R.id.port)).setText(String.valueOf(uri.getPort()));
            }
            if (uri.getPath() != null) {
                channels.add(uri.getPath().replace('/', '#'));
            }
            if (uri.getQuery() != null) {
                ((EditText) findViewById(R.id.password)).setText(String.valueOf(uri.getQuery()));
            }
        }

        // Autentication
        nickservCheckbox = (CheckBox) findViewById(R.id.nickserv_checkbox);
        saslCheckbox = (CheckBox) findViewById(R.id.sasl_checkbox);
        saslUsernameEditText = (EditText) findViewById(R.id.sasl_username);
        saslPasswordEditText = (EditText) findViewById(R.id.sasl_password);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; go home
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * On activity result
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (resultCode != RESULT_OK) {
            return; // ignore everything else
        }

        switch (requestCode) {
            case REQUEST_CODE_CHANNELS:
                channels = data.getExtras().getStringArrayList(Extra.CHANNELS);
                break;

            case REQUEST_CODE_COMMANDS:
                commands = data.getExtras().getStringArrayList(Extra.COMMANDS);
                break;

        }
    }

    /**
     * On click add server or cancel activity
     */
    @Override
    public void onClick(View v)
    {
        switch (v.getId()) {
            case R.id.channels:
                Intent channelIntent = new Intent(this, AddChannelActivity.class);
                channelIntent.putExtra(Extra.CHANNELS, channels);
                startActivityForResult(channelIntent, REQUEST_CODE_CHANNELS);
                break;

            case R.id.commands:
                Intent commandsIntent = new Intent(this, AddCommandsActivity.class);
                commandsIntent.putExtra(Extra.COMMANDS, commands);
                startActivityForResult(commandsIntent, REQUEST_CODE_COMMANDS);
                break;

            case R.id.add:
                save();
                break;

            case R.id.cancel:
                setResult(RESULT_CANCELED);
                finish();
                break;
        }
    }

    /**
     * Try to save server.
     */
    private void save() {
        try {
            validateServer();
            validateIdentity();
            if (server == null) {
                addServer();
            } else {
                updateServer();
            }
            setResult(RESULT_OK);
            finish();
        } catch(ValidationException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        if (nickservCheckbox.isChecked()) {
            authentication.setNickservPassword(saslPasswordEditText.toString());
        } else if (saslCheckbox.isChecked()) {
            authentication.setSaslUsername(saslUsernameEditText.toString());
            authentication.setSaslPassword(saslPasswordEditText.toString());
        }
    }

    /**
     * Add server to database
     */
    private void addServer()
    {
        Database db = new Database(this);

        Identity identity = getIdentityFromView();
        long identityId = db.addIdentity(
                identity.getNickname(),
                identity.getIdent(),
                identity.getRealName(),
                identity.getAliases()
        );

        Server server = getServerFromView();
        server.setAuthentication(authentication);

        long serverId = db.addServer(server, (int) identityId);

        db.setChannels((int) serverId, channels);
        db.setCommands((int) serverId, commands);

        db.close();

        server.setId((int) serverId);
        server.setIdentity(identity);
        server.setAutoJoinChannels(channels);
        server.setConnectCommands(commands);

        Hermes.getInstance().addServer(server);
    }

    /**
     * Update server
     */
    private void updateServer()
    {
        Database db = new Database(this);

        int serverId = this.server.getId();
        int identityId = db.getIdentityIdByServerId(serverId);

        Server server = getServerFromView();
        server.setAuthentication(authentication);
        db.updateServer(serverId, server, identityId);

        Identity identity = getIdentityFromView();
        db.updateIdentity(
            identityId,
            identity.getNickname(),
            identity.getIdent(),
            identity.getRealName(),
            identity.getAliases()
            );

        db.setChannels(serverId, channels);
        db.setCommands(serverId, commands);

        db.close();

        server.setId(this.server.getId());
        server.setIdentity(identity);
        server.setAutoJoinChannels(channels);
        server.setConnectCommands(commands);

        Hermes.getInstance().updateServer(server);
    }

    /**
     * Populate a server object from the data in the view
     *
     * @return The server object
     */
    private Server getServerFromView()
    {
        String title = ((EditText) findViewById(R.id.title)).getText().toString().trim();
        String host = ((EditText) findViewById(R.id.host)).getText().toString().trim();
        int port = Integer.parseInt(((EditText) findViewById(R.id.port)).getText().toString().trim());
        String password = ((EditText) findViewById(R.id.password)).getText().toString().trim();
        String charset = ((Spinner) findViewById(R.id.charset)).getSelectedItem().toString();
        Boolean useSSL = ((CheckBox) findViewById(R.id.useSSL)).isChecked();

        // not in use yet
        //boolean autoConnect = ((CheckBox) findViewById(R.id.autoconnect)).isChecked();

        Server server = new Server();
        server.setHost(host);
        server.setPort(port);
        server.setPassword(password);
        server.setTitle(title);
        server.setCharset(charset);
        server.setUseSSL(useSSL);
        server.setStatus(Status.DISCONNECTED);

        return server;
    }

    /**
     * Populate an identity object from the data in the view
     *
     * @return The identity object
     */
    private Identity getIdentityFromView()
    {
        String nickname = ((EditText) findViewById(R.id.nickname)).getText().toString();
        String ident = ((EditText) findViewById(R.id.ident)).getText().toString().trim();
        String realname = ((EditText) findViewById(R.id.realname)).getText().toString().trim();

        String[] floatStrings = nickname.split(",");

        String[] nick = new String[floatStrings.length];
        for (int i=0; i<nick.length; i++) {
            nick[i] = String.valueOf(floatStrings[i]);
        }

        Identity identity = new Identity();
        identity.setNickname(nick[0]);
        identity.setIdent(ident);
        identity.setRealName(realname);

        // convert String[] to ArrayList<String>
        List<String> aliasesList = Arrays.asList(nick);
        identity.setAliases(aliasesList);

        return identity;
    }

    /**
     * Validate the input for a server
     *
     * @throws ValidationException
     */
    private void validateServer() throws ValidationException
    {
        String title = ((EditText) findViewById(R.id.title)).getText().toString();
        String host = ((EditText) findViewById(R.id.host)).getText().toString();
        String port = ((EditText) findViewById(R.id.port)).getText().toString();
        String charset = ((Spinner) findViewById(R.id.charset)).getSelectedItem().toString();

        if (title.trim().equals("")) {
            throw new ValidationException(getResources().getString(R.string.validation_blank_title));
        }

        if (host.trim().equals("")) {
            // XXX: We should use some better host validation
            throw new ValidationException(getResources().getString(R.string.validation_blank_host));
        }

        try {
            Integer.parseInt(port);
        } catch (NumberFormatException e) {
            throw new ValidationException(getResources().getString(R.string.validation_invalid_port));
        }

        try {
            "".getBytes(charset);
        }
        catch (UnsupportedEncodingException e) {
            throw new ValidationException(getResources().getString(R.string.validation_unsupported_charset));
        }

        Database db = new Database(this);
        if (db.isTitleUsed(title) && (server == null || !server.getTitle().equals(title))) {
            db.close();
            throw new ValidationException(getResources().getString(R.string.validation_title_used));
        }
        db.close();
    }

    /**
     * Validate the input for a identity
     *
     * @throws ValidationException
     */
    private void validateIdentity() throws ValidationException
    {
        String nickname = ((EditText) findViewById(R.id.nickname)).getText().toString();
        String ident = ((EditText) findViewById(R.id.ident)).getText().toString();
        String realname = ((EditText) findViewById(R.id.realname)).getText().toString();

        if (nickname.trim().equals("")) {
            throw new ValidationException(getResources().getString(R.string.validation_blank_nickname));
        }

        if (ident.trim().equals("")) {
            throw new ValidationException(getResources().getString(R.string.validation_blank_ident));
        }

        if (realname.trim().equals("")) {
            throw new ValidationException(getResources().getString(R.string.validation_blank_realname));
        }

        // RFC 1459:  <nick> ::= <letter> { <letter> | <number> | <special> }
        // <special>    ::= '-' | '[' | ']' | '\' | '`' | '^' | '{' | '}'
        // Chars that are not in RFC 1459 but are supported too:
        // | and _
        //Pattern nickPattern = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9^\\-`\\[\\]{}|_\\\\]*$");
        //if (!nickPattern.matcher(nickname).matches()) {
        //    throw new ValidationException(getResources().getString(R.string.validation_invalid_nickname));
        //}

        // We currently only allow chars, numbers and some special chars for ident
        Pattern identPattern = Pattern.compile("^[a-zA-Z0-9\\[\\]\\-_/]+$");
        if (!identPattern.matcher(ident).matches()) {
            throw new ValidationException(getResources().getString(R.string.validation_invalid_ident));
        }
    }
}
