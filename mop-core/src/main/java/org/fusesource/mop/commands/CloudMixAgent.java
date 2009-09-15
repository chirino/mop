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
import org.fusesource.mop.Command;
import org.fusesource.mop.MOP;
import org.fusesource.mop.Optional;
import org.fusesource.mop.ProcessRunner;

/**
 * @version $Revision: 1.1 $
 */
public class CloudMixAgent {
    private static final transient Log LOG = LogFactory.getLog(CloudMixAgent.class);

    @Optional
    private String port = "0";

    /**
     * Starts a CloudMix agent
     */
    @Command
    public void cloudmixAgent(MOP mop, @Optional String url, @Optional String profile, @Optional String workDir) throws Exception {
        mop.setSystemProperty("agent.controller.uri", url);
        mop.setSystemProperty("agent.profile", profile);
        mop.setSystemProperty("agent.workdir", workDir);

        System.out.println("System properties: " + mop.getSystemProperties());
        LinkedList<String> commands = new LinkedList<String>();
        commands.add("war");

        // TODO how to extract the version of this command???
        System.out.println("Version: " + mop.getDefaultVersion());
        commands.add("org.fusesource.cloudmix:org.fusesource.cloudmix.agent.mop.web:" + mop.getDefaultVersion());

        // lets default the port
        commands.add("--port");
        commands.add(port);

        mop.executeCommand(commands);
    }


}