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
 * Test cases for {@link ServiceMix}
 */
public class ServiceMixTest extends TestCase {
    
    private static final MOP SMX3_MOP = new MOP() {
        public String getDefaultVersion() {
            return "3.3.1";
        };
    };
    private static final MOP SMX4_MOP = new MOP() {
        public String getDefaultVersion() {
            return "4.0.0";
        };
    };
    
    /* long running test -- uncomment at your own risk ;)
    public void testCommandLine() throws Exception {
        System.setProperty("mop.base", "target");
        
        MOP mop = new MOP();
        mop.setWorkingDirectory(new File("target"));
        int rc = mop.execute(new String[]{"-l", "target/test-repo", "servicemix:4.0.0", "org.apache.servicemix.examples.bridge:bridge-sa:zip:4.0.0"});
        
        assertEquals(0, rc);
    }*/
    
    public void testGetDeployFolder() throws Exception {
        ServiceMix smx = new ServiceMix();
        File root = new File("root");
        
        smx.configure(SMX3_MOP);
        assertEquals("ServiceMix 3.x uses hotdeploy folder", "hotdeploy", smx.getDeployFolder(root).getName());
        
        smx.configure(SMX4_MOP);
        assertEquals("ServiceMix 4.x uses deploy folder", "deploy", smx.getDeployFolder(root).getName());
    }
    
    public void testGetCommand() throws Exception {
        ServiceMix smx = new ServiceMix();
        File root = new File("root");
        
        smx.configure(SMX3_MOP);
        assertCommand(smx.getCommand(root));
        
        smx.configure(SMX4_MOP);
        List<String> command = smx.getCommand(root);
        assertCommand(command);
        assertEquals("ServiceMix 4 should be started in server mode", "server", command.get(1));
    }

    private void assertCommand(List<String> command) {
        assertTrue("Requires at least two elements", command.size() >= 1);
        String expected = isWindows() ? "bin\\servicemix.bat" : "bin/servicemix";
        assertTrue("Run bin/servicemix", command.get(0).endsWith(expected));
    }

    private boolean isWindows() {
    	String os = System.getProperty("os.name");
        return os != null && os.toLowerCase().contains("windows") ? true : false;
    }
}
