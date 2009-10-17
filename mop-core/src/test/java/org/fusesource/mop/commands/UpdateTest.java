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

import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.fusesource.mop.MOP;
import org.fusesource.mop.MOPRepository;

/** 
 * UpdateTest
 * <p>
 * Description:
 * </p>
 * @author cmacnaug
 * @version 1.0
 */
public class UpdateTest extends TestCase {

    
    public void testUpdateCommand() throws Exception
    {
                
        System.setProperty(MOPRepository.MOP_BASE, System.getProperty("basedir", ".") + File.separator + "target" + File.separator + "test-data" + File.separator + getClass().getSimpleName());
        MOP mop = new MOP();
        mop.setOnline(false);
        mop.setUpdatePolicy(ArtifactRepositoryPolicy.UPDATE_POLICY_NEVER);
        
        
        LinkedList<String> command = new LinkedList<String>();
        command.addLast("update");
        command.addLast("classpath");
        command.addLast("org.fusesource.mop:mop-core:1.0-SNAPSHOT");
        
        mop.executeCommand(command);
        
        assertEquals(true, mop.isOnline());
        assertEquals(ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS, mop.getRepository().getUpdatePolicy());
    }
}
