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

public class ServiceMix extends AbstractContainerBase {

    @Command
    public void servicemix(List<String> params) throws Exception {
        installAndLaunch(params);
    }

    protected String getContainerName() {
        return "ServiceMix";
    }
    
    protected String getArtefactId() {
        // fool MOP into not shading with leading org.fusesource.mop
        StringBuffer sb = new StringBuffer();
        sb.append("org").append(".").append("apache").append(".");
        sb.append("servicemix").append(":apache-servicemix:");
        return sb.toString();
    }
    
    protected String getPrefix() {
        return "apache-servicemix-";
    }
    
    protected String getCommandName() {
        return "servicemix";
    }
    
    protected List<String> processArgs(List<String> command, List<String> params) {
        if (!version.startsWith("3")) {
            command.add("server");
        }
        extractSecondaryCommands(params);
        return command;
    }

    protected String getInput() {
        return null;
    }

    protected File getDeployFolder(File root) {
        if (version.startsWith("3")) {
            return new File(root, "hotdeploy");
        } else {
            return new File(root, "deploy");
        }
    }

    @Override
    protected List<String> getSecondaryCommand(File root, List<String> params) {    
        List<String> commands = null;
        if (!"".equals(secondaryArgs)) {
            commands = new ArrayList<String>();
            commands.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + (isWindows() ? "java.exe" : "java"));
            commands.add("-jar");
            commands.add(root + File.separator + "lib" + File.separator + "karaf-client.jar");
            if (version.startsWith("4.1") || version.startsWith("4.2")) {
                commands.add("-r");
                commands.add("10");
                commands.add("-d");
                commands.add("5");
            }
            commands.add(secondaryArgs);
        }
        return commands;
    }
}
