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

import java.util.LinkedList;

import org.fusesource.mop.Command;
import org.fusesource.mop.MOP;
import org.fusesource.mop.Optional;

/**
 * @version $Revision: 1.1 $
 */
public class CloudMixAgent {

    @Optional
    private String port = "0";

    /**
     * Starts a CloudMix agent
     */
    @Command(name="cloudmix-agent")
    public void cloudmixAgent(MOP mop, 
                              @Optional String url, 
                              @Optional String profile, 
                              @Optional String workDir) throws Exception {
        mop.setSystemProperty("agent.controller.uri", url);
        mop.setSystemProperty("agent.profile", profile);
        mop.setSystemProperty("agent.workdir", workDir);

        System.out.println("System properties: " + mop.getSystemProperties());
        LinkedList<String> commands = new LinkedList<String>();
        commands.add("war");

        // TODO how to extract the version of this command???
        System.out.println("Version: " + mop.getDefaultVersion());
        commands.add("org.fusesource.cloudmix:org.fusesource.cloudmix.agent.mop.web:" 
                     + mop.getDefaultVersion());

        // lets default the port
        commands.add("--port");
        commands.add(port);

        mop.executeCommand(commands);
    }


}