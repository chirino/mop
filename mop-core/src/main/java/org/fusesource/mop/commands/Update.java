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
public class Update {
    private static final transient Log LOG = LogFactory.getLog(Fork.class);

    /**
     * Set the default repository update policy to always.
     */
    @Command
    public void update(MOP mop, LinkedList<String> args) throws Exception {
        LOG.info("Setting repository update policy to " + ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS);

        mop.setOnline(true);
        mop.getRepository().setUpdatePolicy(ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS);
        
        mop.executeCommand(args);
    }
}
