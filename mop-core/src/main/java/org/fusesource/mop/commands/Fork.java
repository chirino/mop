/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.mop.commands;

import com.google.common.collect.Lists;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.mop.Command;
import org.fusesource.mop.MOP;
import org.fusesource.mop.ProcessRunner;
import org.fusesource.mop.support.ArtifactId;

import java.util.LinkedList;
import java.util.List;

/**
 * @version $Revision: 1.1 $
 */
public class Fork {
    private static final transient Log LOG = LogFactory.getLog(Fork.class);

    /**
     * Forks a new child JVM and executes the remaining arguments as a child MOP process
     */
    @Command
    public ProcessRunner fork(MOP mop, LinkedList<String> args) throws Exception {
        LOG.info("forking MOP with " + args);

        // TODO we could try find a mop jar on the URL class loader?

        Package aPackage = Package.getPackage("org.fusesource.mop");
        String version = aPackage.getImplementationVersion();
        if (version == null) {
            version = aPackage.getSpecificationVersion();
        }
        LOG.debug("mop package version: " + version);

        // TODO LATEST/RELEASE don't tend to work?
/*
        if (version == null) {
            version = "RELEASE";
        }
*/
        String classpath;
        if (version != null) {
            ArtifactId mopArtifactId = mop.parseArtifactId("org.fusesource.mop:mop-core:" + version);
            mop.setTransitive(false);
            mop.setArtifactIds(Lists.newArrayList(mopArtifactId));
            classpath = mop.classpath();
        } else {
            classpath = System.getProperty("java.class.path");
            if (classpath == null || classpath.length() == 0) {
                throw new Exception("no java.class.path system property available!");
            }
        }

        List<String> newArgs = Lists.newArrayList();
        newArgs.add("java");
        mop.addSystemProperties(newArgs);
        newArgs.add("-cp");
        newArgs.add(classpath);
        newArgs.add(MOP.class.getName());
        newArgs.addAll(args);

        LOG.debug("About to execute: " + newArgs);
        return mop.exec(newArgs);
    }


}