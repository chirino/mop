/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.mop.commands;

import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.fusesource.mop.Command;
import org.fusesource.mop.MOP;

/**
 * @version $Revision: 1.1 $
 */
public class RepoConfig {
    private static final transient Log LOG = LogFactory.getLog(RepoConfig.class);
       
    /**
     * Set the default repository update policy to always.
     */
    @Command
    public void update(MOP mop, LinkedList<String> args) throws Exception {
        LOG.info("Setting repository update policy to " + ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS);

        mop.setOnline(true);
        mop.getRepository().setUpdatePolicy(ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS);
        
        executeRemaining(mop,args);
    }
    
    /**
     * Set the MOP to offline mode.
     */
    @Command
    public void offline(MOP mop, LinkedList<String> args) throws Exception {
        LOG.info("Setting mop to offline mode");

        mop.setOnline(false);
        executeRemaining(mop,args);
    }
    
    /**
     * Set the MOP to online mode.
     */
    @Command
    public void online(MOP mop, LinkedList<String> args) throws Exception {
        LOG.info("Setting mop to online mode");

        mop.setOnline(true);
        executeRemaining(mop,args);
    }
    
    /**
     * Purges the mop repository
     */
    @Command
    public void purge(MOP mop, LinkedList<String> args) throws Exception {
        LOG.info("Puriging Repository");

        mop.purgeRepository();
        executeRemaining(mop,args);
    }
    
    @Command
    public void includeOptional(MOP mop, LinkedList<String> args) throws Exception {
        LOG.info("Including optional dependencies");
        mop.getRepository().setIncludeOptional(true);
        executeRemaining(mop,args);
    }
    
    @Command
    public void excludeOptional(MOP mop, LinkedList<String> args) throws Exception {
        LOG.info("Excluding optional dependencies");
        mop.getRepository().setIncludeOptional(false);
        executeRemaining(mop,args);
    }

    
    private final void executeRemaining(MOP mop, LinkedList<String> args) throws Exception
    {
        if(!args.isEmpty())
        {
            mop.executeCommand(args);
        }
    }
    
    
}