/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.mop;

import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * @version $Revision: 1.1 $
 */
public class Command {
    public static final String ARTIFACTS_VARIABLE = "${artifacts}";
    public static final String ARGS_VARIABLE = "${args}";
    public static final String VERSION_VARIABLE = "${version}";

    private String name;
    private String description;
    private String usage = "";
    private String alias;

    public Command() {
    }

    public Command(String name, String usage, String description) {
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
}
