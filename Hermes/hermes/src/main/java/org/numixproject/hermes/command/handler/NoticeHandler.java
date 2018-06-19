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
package org.numixproject.hermes.command.handler;

import org.numixproject.hermes.R;
import org.numixproject.hermes.command.BaseHandler;
import org.numixproject.hermes.exception.CommandException;
import org.numixproject.hermes.irc.IRCService;
import org.numixproject.hermes.model.Broadcast;
import org.numixproject.hermes.model.Conversation;
import org.numixproject.hermes.model.Message;
import org.numixproject.hermes.model.Server;

import android.content.Context;
import android.content.Intent;

/**
 * Command: /notice <nickname> <message>
 * 
 * Send a notice to an other user
 * 
 * @author Sebastian Kaspari <sebastian@yaaic.org>
 */
public class NoticeHandler extends BaseHandler
{
    /**
     * Execute /notice
     */
    @Override
    public void execute(String[] params, Server server, Conversation conversation, IRCService service) throws CommandException
    {
        if (params.length > 2) {
            String text = BaseHandler.mergeParams(params);

            Message message = new Message(">" + params[1] + "< " + text);
            message.setIcon(R.drawable.info);
            conversation.addMessage(message);

            Intent intent = Broadcast.createConversationIntent(
                Broadcast.CONVERSATION_MESSAGE,
                server.getId(),
                conversation.getName()
            );
            service.sendBroadcast(intent);

            service.getConnection(server.getId()).sendNotice(params[1], text);
        } else {
            throw new CommandException(service.getString(R.string.invalid_number_of_params));
        }
    }

    /**
     * Usage of /notice
     */
    @Override
    public String getUsage()
    {
        return "/notice <nickname> <message>";
    }

    /**
     * Description of /notice
     */
    @Override
    public String getDescription(Context context)
    {
        return context.getString(R.string.command_desc_notice);
    }
}
