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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.mop.Command;
import org.fusesource.mop.MOP;
import org.fusesource.mop.support.ArtifactId;

/**
 * @version $Revision: 1.1 $
 */
public class Fork {
    private static final transient Log LOG = LogFactory.getLog(Fork.class);

    /**
     * Forks a new child JVM and executes the remaining arguments as a child MOP
     * process
     */
    @Command
    public void fork(MOP mop, LinkedList<String> args) throws Exception {
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
         * if (version == null) { version = "RELEASE"; }
         */
        String classpath;
        if (version != null) {
            ArtifactId mopArtifactId = mop.parseArtifactId("org.fusesource.mop:mop-core:" + version);
            mop.setTransitive(false);
            mop.setArtifactIds(Arrays.asList(mopArtifactId));
            classpath = mop.classpath();
        } else {
            classpath = System.getProperty("java.class.path");
            if (classpath == null || classpath.length() == 0) {
                throw new Exception("no java.class.path system property available!");
            }
        }
        if (isWindows() && classpath.contains(" ")) {
            classpath = "\"" + classpath + "\"";
        }

        List<String> newArgs = new ArrayList<String>();
        String javaExe = "java";
        if (isWindows()) {
            javaExe += ".exe";
        }
        newArgs.add(javaExe);

        //Propagate repository props to the forked process:
        for (Entry<String, String> entry : mop.getRepository().getRepositorySystemProps().entrySet()) {
            mop.setSystemProperty(entry.getKey(), entry.getValue());
        }

        mop.addSystemProperties(newArgs);
        newArgs.add("-D" + MOP.MOP_WORKING_DIR_SYSPROPERTY + "=" + mop.getWorkingDirectory().getAbsolutePath());

        newArgs.add("-cp");
        newArgs.add(classpath);
        newArgs.add(MOP.class.getName());
        newArgs.addAll(args);

        LOG.debug("About to execute: " + newArgs);
        mop.exec(newArgs);
    }

    private boolean isWindows() {
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase().contains("windows") ? true : false;
    }

}