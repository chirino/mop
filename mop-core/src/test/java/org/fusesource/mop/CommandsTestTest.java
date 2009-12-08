/**
 *  Copyright (C) 2009 Progress Software, Inc. All rights reserved.
 *  http://fusesource.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.fusesource.mop;

import java.util.Map;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.mop.support.CommandDefinition;
import org.fusesource.mop.support.CommandDefinitions;

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
