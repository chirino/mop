/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.mop.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.fusesource.mop.Command;

public class Karaf extends AbstractContainerBase {
        
    private String secondaryArgs = "";
    
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
        for (int i = 0 ; i < params.size() ; i++) {
            String param = params.get(i);
            if ("-c".equals(param) || "--commands".equals(param)) {
                int remaining = params.size() - (i + 1);
                params.remove(i);
                for (int j = 0 ; j < remaining ; j++) {
                    secondaryArgs += params.get(i);
                    secondaryArgs += " ";
                    params.remove(i);
                }
            }
        }
        return command;
    }

    protected File getDeployFolder(File root) {
        return new File(root, "deploy");
    }

    @Override
   protected List<String> getSecondaryCommand(File root, List<String> params) {
        List<String> commands = null;
        if (!"".equals(secondaryArgs)) {
            commands = new ArrayList<String>();
            commands.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
            commands.add("-jar");
            commands.add(root + File.separator + "lib" + File.separator + "karaf-client.jar");
            commands.add(secondaryArgs);
        }
        return commands;
    }
}