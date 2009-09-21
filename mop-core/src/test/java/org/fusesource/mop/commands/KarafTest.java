/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.mop.commands;

import java.io.File;
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
        Karaf smx = new Karaf();
        File root = new File("root");
        
        smx.configure(KARAF_MOP);
        assertEquals("Karaf uses deploy folder", "deploy", smx.getDeployFolder(root).getName());
    }
    
    public void testGetCommand() throws Exception {
        Karaf smx = new Karaf();
        File root = new File("root");
        
        smx.configure(KARAF_MOP);
        List<String> command = smx.getCommand(root);
        assertCommand(command);
        assertEquals("Karaf should be started with no extra args", 1, command.size());
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
