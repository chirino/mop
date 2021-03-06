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
package org.fusesource.mop.support;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.mop.MOP;

/**
 * @version $Revision: 1.1 $
 */
public class CommandDefinition {
    public static final String ARTIFACTS_VARIABLE = "${artifacts}";
    public static final String ARGS_VARIABLE = "${args}";
    public static final String VERSION_VARIABLE = "${version}";
    
    private static final transient Log LOG = LogFactory.getLog(CommandDefinition.class);


    private String name;
    private String description;
    private String usage = "";
    private String alias;
    private MOP mop;

    public CommandDefinition() {
    }

    public CommandDefinition(String name, String usage, String description) {
        this.name = name;
        this.usage = usage;
        this.description = description;
    }

    @Override
    public String toString() {
        return "Command[name: " + name + " alias: " + alias + " usage: " + usage + " description: " + description + "]";

    }

    /**
     * Returns a list of the alias command line arguments
     */
    public List<String> getAliasArguments() {
        List<String> answer = new ArrayList<String>();
        if (alias != null) {
            StringTokenizer iter = new StringTokenizer(alias);
            while (iter.hasMoreTokens()) {
                String arg = iter.nextToken();
                answer.add(arg);
            }
        }
        return answer;
    }
    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getUsage() {
        return usage;
    }

    public void setUsage(String usage) {
        this.usage = usage;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void executeCommand(MOP mop, LinkedList<String> argList) throws Exception {
        this.mop = mop;

        // lets run the command!
        LinkedList<String> artifacts = new LinkedList<String>();
        LinkedList<String> args = new LinkedList<String>();
        splitArgumentList(argList, artifacts, args);


        boolean addedArgs = false;
        LinkedList<String> newArguments = new LinkedList<String>();
        for (String arg : getAliasArguments()) {
            arg = replaceVariables(arg);
            if (arg.equals(ARTIFACTS_VARIABLE)) {
                newArguments.addAll(artifacts);
            } else if (arg.equals(ARGS_VARIABLE)) {
                newArguments.addAll(args);
                addedArgs = true;
            } else {
                newArguments.add(arg);
            }
        }
        if (!addedArgs) {
            newArguments.addAll(args);
        }

        LOG.info("About to execute: " + newArguments);
        mop.executeCommand(newArguments);
    }



    private String replaceVariables( String arg) {
        return arg.replaceAll("\\$\\{version\\}", mop.getDefaultVersion());
    }

    /**
     * Lets split the argument list into the artifact(s) strings then the remaining arguments
     */
    protected void splitArgumentList(LinkedList<String> argList, LinkedList<String> artifacts, LinkedList<String> remainingArgs) {
        if (argList.isEmpty()) {
            return;
        }
        artifacts.add(argList.removeFirst());
        while (!argList.isEmpty()) {
            String arg = argList.removeFirst();
            if (mop.isAnotherArtifactId(arg)) {
                artifacts.add(arg);
            } else {
                remainingArgs.add(arg);
                remainingArgs.addAll(argList);
                break;
            }
        }

    }

}
