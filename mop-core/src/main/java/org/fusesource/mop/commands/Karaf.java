/**
 *  Copyright (C) 2009 Progress Software, Inc. All rights reserved.
 *  http://fusesource.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
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
        extractEnvironment(params);
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