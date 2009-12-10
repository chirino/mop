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

import static org.fusesource.mop.support.Logger.debug;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.fusesource.mop.Artifacts;
import org.fusesource.mop.Command;
import org.fusesource.mop.MOP;

/**
 * @version $Revision: 1.1 $
 */
public class RepoConfig {
       
    /**
     * Set the default repository update policy to always.
     */
    @Command
    public void update(MOP mop, LinkedList<String> args) throws Exception {
        debug("Setting repository update policy to " + ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS);

        mop.setOnline(true);
        mop.getRepository().setUpdatePolicy(ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS);
        
        executeRemaining(mop,args);
    }
    
    /**
     * Set the MOP to offline mode.
     */
    @Command
    public void offline(MOP mop, LinkedList<String> args) throws Exception {
        debug("Setting mop to offline mode");

        mop.setOnline(false);
        executeRemaining(mop,args);
    }
    
    /**
     * Set the MOP to online mode.
     */
    @Command
    public void online(MOP mop, LinkedList<String> args) throws Exception {
        debug("Setting mop to online mode");

        mop.setOnline(true);
        executeRemaining(mop,args);
    }
    
    /**
     * Purges the mop repository
     */
    @Command
    public void purge(MOP mop, LinkedList<String> args) throws Exception {
        debug("Puriging Repository");

        mop.purgeRepository();
        executeRemaining(mop,args);
    }
    
    @Command(name="include-optional")
    public void includeOptional(MOP mop, LinkedList<String> args) throws Exception {
        debug("Including optional dependencies");
        mop.getRepository().setIncludeOptional(true);
        executeRemaining(mop,args);
    }
    
    @Command(name="exclude-optional")
    public void excludeOptional(MOP mop, LinkedList<String> args) throws Exception {
        debug("Excluding optional dependencies");
        mop.getRepository().setIncludeOptional(false);
        executeRemaining(mop,args);
    }

    /**
     * Gets the specified artifacts and stores them in the mop repository.
     */
    @Command
    public void get(MOP mop,  Artifacts artifacts) throws Exception {
        List<File> files = artifacts.getFiles();
        for (File artifact : files) {
            System.out.println(artifact);
        }
    }

    private final void executeRemaining(MOP mop, LinkedList<String> args) throws Exception
    {
        if(!args.isEmpty())
        {
            mop.executeCommand(args);
        }
    }
    
    
    
    
}