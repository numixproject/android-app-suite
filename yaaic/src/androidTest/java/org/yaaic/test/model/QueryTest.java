/*
Yaaic - Yet Another Android IRC Client

Copyright 2009-2010 Sebastian Kaspari

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
package org.numixproject.hermes.test.model;

import org.numixproject.hermes.model.Query;
import org.numixproject.hermes.model.Conversation;

import junit.framework.TestCase;

/**
 * Test case for org.numixproject.hermes.model.Query
 * 
 * @author Sebastian Kaspari <sebastian@yaaic.org>
 */
public class QueryTest extends TestCase
{
	private Query query;
	
	@Override
	protected void setUp()
	{
		this.query = new Query("pocmo");
	}

	public void testGetType()
	{
		assertEquals(Conversation.TYPE_QUERY, query.getType());
	}
	
	public void testInheritance()
	{
		assertTrue(query instanceof Conversation);
	}
}
