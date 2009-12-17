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
        assertCommand(smx.getCommand(root, new ArrayList<String>()));
        
        smx.configure(SMX4_MOP);
        List<String> command = smx.getCommand(root, new ArrayList<String>());
        assertCommand(command);
        assertEquals("ServiceMix 4 should be started in server mode", "server", command.get(1));
    }
    
    public void testGetCommandWithExtras() throws Exception {
        ServiceMix smx = new ServiceMix();
        File root = new File("root");
        List<String> params = new ArrayList<String>();
        params.add("foobar:1.2.3.4");
        params.add("--environment");
        params.add("VAR1=first_half");
        params.add("VAR2=whole");
        params.add("VAR1=second_half");
        params.add("--commands");
        params.add("some command ; some other command");
        
        smx.configure(SMX4_MOP);
        List<String> command = smx.getCommand(root, params);
        assertCommand(command);
        assertEquals("SMX should be started with no extra args", 2, command.size());
        assertEquals("params should be stripped of secondary commands", 1, params.size());
        assertEquals("params should be stripped of secondary commands", "foobar:1.2.3.4", params.get(0));
        
        assertNull(smx.getInput());

        String[] env = smx.getEnvironment();
        assertNotNull("expected environment", env);
        assertEquals("unexpected environment size", 2, env.length);
        String[] expected = {"VAR1=first_half second_half", "VAR2=whole"};
        assertTrue("unexpected environment settings", 
                   (expected[0].equals(env[0]) && expected[1].equals(env[1])) ||
                   (expected[0].equals(env[1]) && expected[1].equals(env[0])));
        
        List<String> secondary = smx.getSecondaryCommand(root, params);
        assertEquals("unexpected secondary command size", 4, secondary.size());
        assertTrue("unexpected secondary command", 
                   secondary.get(0).endsWith(smx.isWindows() ? "java.exe" : "java"));
        assertEquals("unexpected secondary command", "-jar", secondary.get(1));
        assertTrue("unexpected secondary command", secondary.get(2).endsWith("karaf-client.jar"));
        assertEquals("unexpected secondary command", "some command ; some other command ", secondary.get(3));
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
