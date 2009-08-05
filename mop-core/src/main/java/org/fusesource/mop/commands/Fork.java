/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.mop.commands;

import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.fusesource.mop.Option;
import org.fusesource.mop.Command;
import org.fusesource.mop.Lookup;
import org.fusesource.mop.Artifacts;
import org.fusesource.mop.MOP;
import org.fusesource.mop.ProcessRunner;
import org.fusesource.mop.support.ConfiguresMop;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.util.List;
import java.util.LinkedList;

import com.google.common.collect.Lists;

/**
 * @version $Revision: 1.1 $
 */
public class Fork {

    /**
     * Forks a new child JVM and executes the remaining arguments as a child MOP process
     */
    @Command
    public ProcessRunner fork(MOP mop, LinkedList<String> args) throws Exception {
        System.out.println("forking MOP with " + args);

        // TODO we could either use the system classpath
        // or we could discover MOP ourselves and restart that
        String classpath = System.getProperty("java.class.path");
        if (classpath == null || classpath.length() == 0) {
            throw new Exception("no java.class.path system property available!");
        }

        List<String> newArgs = Lists.newArrayList();
        newArgs.add("java");
        newArgs.add("-cp");
        newArgs.add(classpath);
        newArgs.add(MOP.class.getName());
        newArgs.addAll(args);

        return mop.exec(newArgs);
    }



}