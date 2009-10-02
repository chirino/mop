/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
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
        params.add("--commands");
        params.add("some command ; some other command");
        
        karaf.configure(KARAF_MOP);
        List<String> command = karaf.getCommand(root, params);
        assertCommand(command);
        assertEquals("Karaf should be started with no extra args", 1, command.size());
        assertEquals("params should be stripped of secondary commands", 1, params.size());
        assertEquals("params should be stripped of secondary commands", "foobar:1.2.3.4", params.get(0));
        
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
