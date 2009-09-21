/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.mop.commands;

import java.io.File;
import java.util.List;

import org.fusesource.mop.Command;

public class Karaf extends AbstractContainerBase {
        
    @Command
    public void karaf(List<String> params) throws Exception {
        installAndLaunch(params);
    }

    protected String getContainerName() {
        return "karaf";
    }
    
    protected String getArtefactId() {
        return "org.apache.felix.karaf:apache-felix-karaf:";
    }
    
    protected String getPrefix() {
        return "apache-felix-karaf-";
    }
    
    protected String getCommandName() {
        return "karaf";
    }
    
    protected List<String> addArgs(List<String> command) {        
        return command;
    }

    protected File getDeployFolder(File root) {
        return new File(root, "deploy");
    }
}

