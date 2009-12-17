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
package org.fusesource.mop.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.fusesource.mop.MOP;

/**
 * Test cases for {@link Karaf}
 */
public class KarafTest extends TestCase {
    
    private static final MOP KARAF_MOP = new MOP() {
        public String getDefaultVersion() {
            return "0.9.0-SNAPSHOT";
        };
    };
    
    public void testGetDeployFolder() throws Exception {
        Karaf karaf = new Karaf();
        File root = new File("root");
        
        karaf.configure(KARAF_MOP);
        assertEquals("Karaf uses deploy folder", "deploy", karaf.getDeployFolder(root).getName());
    }
    
    public void testGetCommand() throws Exception {
        Karaf karaf = new Karaf();
        File root = new File("root");
        
        karaf.configure(KARAF_MOP);
        List<String> command = karaf.getCommand(root, new ArrayList<String>());
        assertCommand(command);
        assertEquals("Karaf should be started with no extra args", 1, command.size());
    }
    
    public void testGetCommandWithExtras() throws Exception {
        Karaf karaf = new Karaf();
        File root = new File("root");
        List<String> params = new ArrayList<String>();
        params.add("foobar:1.2.3.4");
        params.add("--environment");
        params.add("VAR1=first_half");
        params.add("VAR2=whole");
        params.add("VAR1=second_half");
        params.add("--commands");
        params.add("some command ; some other command");
        
        karaf.configure(KARAF_MOP);
        List<String> command = karaf.getCommand(root, params);
        assertCommand(command);
        assertEquals("Karaf should be started with no extra args", 1, command.size());
        assertEquals("params should be stripped of secondary commands", 1, params.size());
        assertEquals("params should be stripped of secondary commands", "foobar:1.2.3.4", params.get(0));

        String[] env = karaf.getEnvironment();
        assertNotNull("expected environment", env);
        assertEquals("unexpected environment size", 2, env.length);
        String[] expected = {"VAR1=first_half second_half", "VAR2=whole"};
        assertTrue("unexpected environment settings", 
                   (expected[0].equals(env[0]) && expected[1].equals(env[1])) ||
                   (expected[0].equals(env[1]) && expected[1].equals(env[0])));
        
        String input = karaf.getInput();
        assertEquals("some command ; some other command", input.trim());
        /*
        List<String> secondary = karaf.getSecondaryCommand(root, params);
        assertEquals("unexpected secondary command size", 4, secondary.size());
        assertTrue("unexpected secondary command", secondary.get(0).endsWith("java"));
        assertEquals("unexpected secondary command", "-jar", secondary.get(1));
        assertTrue("unexpected secondary command", secondary.get(2).endsWith("karaf-client.jar"));
        assertEquals("unexpected secondary command", "some command ; some other command ", secondary.get(3));
        */
    }

    private void assertCommand(List<String> command) {
        assertTrue("Requires at least two elements", command.size() >= 1);
        String expected = isWindows() ? "bin\\karaf.bat" : "bin/karaf";
        assertTrue("Run bin/karaf", command.get(0).endsWith(expected));
    }

    private boolean isWindows() {
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase().contains("windows") ? true : false;
    }
}
