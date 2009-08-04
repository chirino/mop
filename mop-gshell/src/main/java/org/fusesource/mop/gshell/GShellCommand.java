/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
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
