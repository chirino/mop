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

public class ServiceMix extends AbstractContainerBase {
        
    @Command
    public void servicemix(List<String> params) throws Exception {
        installAndLaunch(params);
    }

    protected String getContainerName() {
        return "ServiceMix";
    }
    
    protected String getArtefactId() {
        return "org.apache.servicemix:apache-servicemix:";
    }
    
    protected String getPrefix() {
        return "apache-servicemix-";
    }
    
    protected String getCommandName() {
        return "servicemix";
    }
    
    protected List<String> addArgs(List<String> command) {        
        if (!version.startsWith("3")) {
            command.add("server");
        }
        return command;
    }

    protected File getDeployFolder(File root) {
        if (version.startsWith("3")) {
            return new File(root, "hotdeploy");
        } else {
            return new File(root, "deploy");
        }
    }

}
