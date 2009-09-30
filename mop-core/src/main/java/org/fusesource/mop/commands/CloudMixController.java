/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.mop.commands;

import java.util.LinkedList;

import org.fusesource.mop.Command;
import org.fusesource.mop.MOP;
import org.fusesource.mop.Optional;

/**
 * @version $Revision: 1.1 $
 */
public class CloudMixController {

    @Optional
    private String port = "8181";

    /**
     * Starts a CloudMix agent
     */
    @Command
    public void cloudmixController(MOP mop, @Optional String port) throws Exception {

        if (port != null) {
            this.port = port;
        }

        System.out.println("Cloudlaunch controller port is " + this.port);
        LinkedList<String> commands = new LinkedList<String>();
        commands.add("war");

        // TODO how to extract the version of this command???
        System.out.println("Version: " + mop.getDefaultVersion());
        commands.add("org.fusesource.cloudmix:org.fusesource.cloudmix.controller.webapp:" + mop.getDefaultVersion());

        // lets default the port
        commands.add("--port");
        commands.add(this.port);

        mop.executeCommand(commands);
    }

}