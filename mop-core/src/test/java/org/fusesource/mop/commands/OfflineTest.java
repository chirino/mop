/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.mop.commands;

import java.io.File;
import java.util.LinkedList;

import junit.framework.TestCase;

import org.fusesource.mop.MOP;
import org.fusesource.mop.MOPRepository;

/**
 * PurgeTest
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class OfflineTest extends TestCase {

    public void testOfflineCommand() throws Exception {

        System.setProperty(MOPRepository.MOP_BASE, System.getProperty("basedir", ".") + File.separator + "target" + File.separator + "test-data" + File.separator + getClass().getSimpleName());
        MOP mop = new MOP();
        mop.setArtifactIds(mop.parseArtifactId("org.fusesource.mop:mop-core:1.0-SNAPSHOT"));
        assertNotNull(mop.classpath());
        
        LinkedList<String> command = new LinkedList<String>();
        command.addLast("purge");
        command.addLast("offline");
        command.addLast("classpath");
        command.addLast("org.fusesource.mop:mop-core:1.0-SNAPSHOT");
        
        Exception expected = null;
        try {
            mop.executeCommand(command);
        } catch (Exception e) {
            System.out.println("Got expected exception resolving artifact: " + e.getMessage());
            e.printStackTrace();
            expected = e;
        }
        assertNotNull(expected);

        command = new LinkedList<String>();
        command.addLast("online");
        command.addLast("classpath");
        command.addLast("org.fusesource.mop:mop-core:1.0-SNAPSHOT");
        
        mop.executeCommand(command);
    }
}
