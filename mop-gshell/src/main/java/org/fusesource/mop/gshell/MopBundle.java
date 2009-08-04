/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.mop.gshell;

import org.apache.geronimo.gshell.application.plugin.bundle.Bundle;
import org.apache.geronimo.gshell.registry.CommandRegistry;
import org.fusesource.mop.MOP;
import org.fusesource.mop.support.CommandDefinition;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Registers the MOP commands into the GShell registry
 *
 * @version $Revision: 1.1 $
 */
public class MopBundle implements Bundle {
    private final MOP mop;
    private final CommandRegistry commandRegistry;
    private boolean enabled;
    private Set<GShellCommand> gshellCommands = new HashSet<GShellCommand>();

    public MopBundle(MOP mop, CommandRegistry commandRegistry) {
        this.mop = mop;
        this.commandRegistry = commandRegistry;
    }

    public String getName() {
        return "mop";
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void enable() throws Exception {
        if (!enabled) {
            enabled = true;

            gshellCommands.clear();

            Collection<CommandDefinition> commands = mop.getCommands().values();
            for (CommandDefinition command : commands) {
                GShellCommand gshellCommand = new GShellCommand(mop, command);
                gshellCommands.add(gshellCommand);
                commandRegistry.registerCommand(gshellCommand);
            }
        }
    }

    public void disable() throws Exception {
        if (enabled) {
            enabled = false;

            for (GShellCommand gshellCommand : gshellCommands) {
                commandRegistry.removeCommand(gshellCommand);
            }
            gshellCommands.clear();
        }
    }
}
