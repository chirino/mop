/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.mop;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.fusesource.mop.support.ArtifactId;

/**
 * Some simple tests of command line argument processing
 */
public class CommandLineTest extends TestCase {
    protected MOP mavenRunner;
    protected String groupId = "javax.servlet";
    protected String artifactId = "servlet-api";
    protected String version = "RELEASE";
    protected String className = "SomeClass";
    protected String arg0 = "arg0";
    protected String arg1 = "arg1";
    
    public void testShowHelp() {
        int rc = new MOP().execute(new String[]{"-h"});
        assertEquals(0, rc);
    }

    public void testScope() {
        int rc = new MOP().execute(new String[]{"--scope", "compile", "-l", "target/test-repo", "classpath", "javax.servlet:servlet-api:2.3"});
        assertEquals(0, rc);
    }


    public void testClassPathCommand() {
        int rc = new MOP().execute(new String[]{"-l", "target/test-repo", "classpath", "javax.servlet:servlet-api:2.3"});
        assertEquals(0, rc);
    }

    public void testEchoCommand() {
        int rc = new MOP().execute(new String[]{"-l", "target/test-repo", "echo", "javax.servlet:servlet-api:2.3", className, arg0, arg1});
        assertEquals(0, rc);
    }

    public void testAritfactLookup() {
        // Online version
        int rc = new MOP().execute(new String[]{"-X", "-l", "target/test-repo", "classpath", "commons-logging:commons-logging:1.1.1"});
        assertEquals(0, rc);
        // Offline version
        rc = new MOP().execute(new String[]{"-X", "-l", "target/test-repo", "offline", "classpath", "commons-logging"});
        assertEquals(0, rc);
    }
    
    
    public void testUsingLatestRelease() {
        doRun("-X", "exec", groupId+":"+artifactId, className, arg0, arg1);
    }

    public void testUsingSpecificVersion() {
        version = "5.1.0";
        doRun("-X", "exec", groupId+":"+artifactId+":"+version, className, arg0, arg1);
    }

    public void testSpecifyManyRepos() {
        doRun("-X", "-r", "ops4j=http://repository.ops4j.org/maven2", "-r", "apache=http://repository.apache.org/content/groups/public", "exec", groupId+":"+artifactId, className, arg0, arg1);
    }

    protected void doRun(String... args) {
        mavenRunner = new MOP();
        mavenRunner.setRepository(new MOPRepository() {
            @Override
            public List<File> resolveFiles(List<ArtifactId> artifactIds) throws Exception {
                System.out.println("We would be doing something now :)");
                return new ArrayList<File>();
            }
        });


        int rc = mavenRunner.execute(args);
        assertEquals(0, rc);
        
        assertEquals("mavenRunner.getGroupId()", groupId, mavenRunner.getArtifactIds().get(0).getGroupId());
        assertEquals("mavenRunner.getArtifactId()", artifactId, mavenRunner.getArtifactIds().get(0).getArtifactId());
        assertEquals("mavenRunner.getVersion()", version, mavenRunner.getArtifactIds().get(0).getVersion());
        assertEquals("mavenRunner.getClassName()", className, mavenRunner.getClassName());

        List remainingArgs = mavenRunner.getReminingArgs();
        assertEquals("remainingArgs size", 2, remainingArgs.size());
        assertEquals("remainingArgs(0)", arg0, remainingArgs.get(0));
        assertEquals("remainingArgs(1)", arg1, remainingArgs.get(1));


    }
}
