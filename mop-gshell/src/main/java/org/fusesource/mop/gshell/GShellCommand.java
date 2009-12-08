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
package org.fusesource.mop.gshell;

import org.apache.geronimo.gshell.command.CommandAction;
import org.apache.geronimo.gshell.command.CommandContext;
import org.apache.geronimo.gshell.wisdom.command.CommandSupport;
import org.apache.geronimo.gshell.wisdom.command.CommandMessageSource;
import org.apache.geronimo.gshell.wisdom.command.ConfigurableCommandCompleter;
import org.apache.geronimo.gshell.wisdom.command.MessageSourceCommandDocumenter;
import org.apache.geronimo.gshell.wisdom.registry.CommandLocationImpl;
import org.fusesource.mop.MOP;
import org.fusesource.mop.support.CommandDefinition;

import java.util.LinkedList;

/**
 * Adapts MOP commands to running inside GShell
 *
 * @version $Revision: 1.1 $
 */
public class GShellCommand extends CommandSupport {

    public GShellCommand(final MOP mop, final CommandDefinition definition) {
        setAction(new CommandAction() {
            public Object execute(CommandContext commandContext) throws Exception {
                Object[] arguments = commandContext.getArguments();
                LinkedList<String> argList = new LinkedList<String>();
                for (Object argument : arguments) {
                    if (argument != null) {
                        argList.add(argument.toString());
                    }
                }
                definition.executeCommand(mop, argList);
                return CommandAction.Result.SUCCESS;
            }
        });

        setMessages(new CommandMessageSource());
        setCompleter(new ConfigurableCommandCompleter());

        // TODO make real location?
        setLocation(new CommandLocationImpl("mop"));
        setDocumenter(new MessageSourceCommandDocumenter());
    }

}
