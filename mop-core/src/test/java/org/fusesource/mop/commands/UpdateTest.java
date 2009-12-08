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
