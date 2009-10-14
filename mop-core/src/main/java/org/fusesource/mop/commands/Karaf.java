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
        StringBuffer sb = new StringBuffer();
        sb.append("org").append(".").append("apache").append(".").append("felix");
        sb.append(".").append("karaf").append(":apache-felix-karaf:");
        return sb.toString();
        //return "org.apache.felix.karaf:apache-felix-karaf:";
    }
    
    protected String getPrefix() {
        return "apache-felix-karaf-";
    }
    
    protected String getCommandName() {
        return "karaf";
    }
    
    protected List<String> processArgs(List<String> command, List<String> params) {
        extractSecondaryCommands(params);
        return command;
    }
    
    protected String getInput() {
        return "".equals(secondaryArgs) ? null : secondaryArgs + "\n";
    }

    protected File getDeployFolder(File root) {
        return new File(root, "deploy");
    }
    
    @Override
    protected List<String> getSecondaryCommand(File root, List<String> params) {
        return null;
    }
}