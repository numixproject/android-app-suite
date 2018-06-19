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
package org.numixproject.hermes.adapter;

import org.numixproject.hermes.R;
import org.numixproject.hermes.model.User;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Adapter for user action lists
 * 
 * @author Sebastian Kaspari <sebastian@yaaic.org>
 */
public class UserActionListAdapter extends BaseAdapter
{
    /**
     * Action IDs
     */
    private final int[] actions = {
        User.ACTION_WHOIS,
        User.ACTION_REPLY,
        User.ACTION_QUERY,
        User.ACTION_OP,
        User.ACTION_DEOP,
        User.ACTION_VOICE,
        User.ACTION_DEVOICE,
        User.ACTION_KICK,
        User.ACTION_BAN
    };

    /**
     * Labels for actions
     */
    private final int[] labels = {
        R.string.user_action_whois,
        R.string.user_action_reply,
        R.string.user_action_query,
        R.string.user_action_op,
        R.string.user_action_deop,
        R.string.user_action_voice,
        R.string.user_action_devoice,
        R.string.user_action_kick,
        R.string.user_action_ban,
    };

    /**
     * Icons for actions
     */
    private final int[] icons = {
        R.drawable.ic_info_black_24dp,
        R.drawable.ic_reply_black_24dp,
        R.drawable.ic_message_black_24dp,
        R.drawable.ic_call_made_black_24dp,
        R.drawable.ic_call_received_black_24dp,
        R.drawable.ic_mic_black_24dp,
        R.drawable.ic_mic_off_black_24dp,
        R.drawable.ic_remove_circle_outline_black_24dp,
        R.drawable.ic_remove_circle_black_24dp
    };

    /**
     * Get number of actions
     */
    @Override
    public int getCount()
    {
        return actions.length;
    }

    /**
     * Get object for given position
     */
    @Override
    public Object getItem(int position)
    {
        return null;
    }

    /**
     * Get item id for given position
     */
    @Override
    public long getItemId(int position)
    {
        return actions[position];
    }

    /**
     * Get view for given position
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.actionitem, null);
        }

        TextView textView  = (TextView)  convertView.findViewById(R.id.label);
        ImageView iconView = (ImageView) convertView.findViewById(R.id.icon);

        textView.setText(labels[position]);
        iconView.setImageResource(icons[position]);

        return convertView;
    }
}
