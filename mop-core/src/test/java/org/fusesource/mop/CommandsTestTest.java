/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.mop;

import junit.framework.TestCase;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.mop.support.CommandDefinitions;
import org.fusesource.mop.support.CommandDefinition;

/**
 * @version $Revision: 1.1 $
 */
public class CommandsTestTest extends TestCase {
    private static final transient Log LOG = LogFactory.getLog(CommandsTestTest.class);

    public void testLoadingOfCommands() throws Exception {
        Map<String, CommandDefinition> map = CommandDefinitions.loadCommands(getClass().getClassLoader());
        assertTrue("map should not be empty", !map.isEmpty());

        assertValidCommand(map, "war");
        assertValidCommand(map, "spring");
    }

    private CommandDefinition assertValidCommand(Map<String, CommandDefinition> map, String name) {
        CommandDefinition command = map.get(name);
        assertNotNull("Should have a command called '" + name + "' in the command map: " + map, command);
        LOG.info("Found " + command);

        assertEquals("name", name, command.getName());

        assertNotNullOrdBlank("alias", command.getAlias());
        assertNotNullOrdBlank("description", command.getDescription());
        return command;
    }

    private void assertNotNullOrdBlank(String name, String value) {
        assertNotNull(name + " is null!", value);
        assertTrue(name + " should not be blank!", value.length() > 0);

    }

}
