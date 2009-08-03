/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.mop.support;

import org.codehaus.plexus.PlexusContainer;
import org.fusesource.mop.MOP;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.LinkedList;

/**
 * @version $Revision: 1.1 $
 */
public class CommandDefinition {
    private static final transient Log LOG = LogFactory.getLog(CommandDefinition.class);

    public static final String ARTIFACTS_VARIABLE = "${artifacts}";
    public static final String ARGS_VARIABLE = "${args}";
    public static final String VERSION_VARIABLE = "${version}";

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

        PlexusContainer container = mop.getContainer();
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
        mop.processCommandLine(newArguments);
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
