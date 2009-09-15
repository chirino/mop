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
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.mop.Command;
import org.fusesource.mop.MOP;
import org.fusesource.mop.ProcessRunner;
import org.fusesource.mop.support.ConfiguresMop;

/**
 * Allows running arbitrary commands and shell scripts through MOP
 */
public class Shell implements ConfiguresMop {
    
    private static final transient Log LOG = LogFactory.getLog(Shell.class);
    private MOP mop;

    /**
     * Forks a new child process by running an external command
     */
    @Command
    public void shell(File command, List<String> arguments) throws Exception {
        LOG.info("Running external shell script " + command);

        List<String> commands = new LinkedList<String>();
        commands.add(command.toString());
        commands.addAll(arguments);
        mop.exec(commands);
    }

    public void configure(MOP mop) {
        this.mop = mop;
        
    }
}