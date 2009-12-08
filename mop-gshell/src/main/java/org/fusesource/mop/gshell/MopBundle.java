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
