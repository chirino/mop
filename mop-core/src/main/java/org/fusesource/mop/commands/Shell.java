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
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.mop.Command;
import org.fusesource.mop.MOP;
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