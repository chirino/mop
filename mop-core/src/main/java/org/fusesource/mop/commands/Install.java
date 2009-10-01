/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.mop.commands;

import java.io.File;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.fusesource.mop.Artifacts;
import org.fusesource.mop.Command;
import org.fusesource.mop.Lookup;
import org.fusesource.mop.MOP;
import org.fusesource.mop.Optional;
import org.fusesource.mop.support.ConfiguresMop;

/**
 * @version $Revision: 1.1 $
 */
public class Install implements ConfiguresMop {
    private static final transient Log LOG = LogFactory.getLog(Install.class);

    @Optional
    private boolean failIfNotExit = false;

    @Lookup
    private ArchiverManager archiverManager;

    @Override
    public String toString() {
        return "Install[archiver: " + archiverManager + " failIfNotExit: " + failIfNotExit + "]";
    }

    public void configure(MOP mop) {
        String defaultType = "tar.gz";
        String osName = System.getProperty("os.name", "NO OS NAME!!");
        if (osName.contains("Windows")) {
            defaultType = "zip";
        }
        LOG.debug("OS name " + osName + " has default type: " + defaultType);
        mop.setDefaultType(defaultType);
        mop.setTransitive(false);
    }

    /**
     * Installs the given artifacts in the given target directory
     */
    @Command
    public void install(Artifacts artifacts, File targetDirectory) throws Exception {
        System.out.println("installing to " + targetDirectory);
        checkTargetDirectory(targetDirectory);

        List<File> files = artifacts.getFiles();
        for (File artifact : files) {
            installFile(artifact, targetDirectory);
        }
    }

    protected void installFile(File source, File destDir) throws Exception {

        try {
            UnArchiver unArchiver = archiverManager.getUnArchiver(source);
            unArchiver.setSourceFile(source);
            unArchiver.setDestDirectory(destDir);
            unArchiver.extract();
        }
        catch (ArchiverException e) {
            throw new Exception("Error unpacking file: " + source + "to: " + destDir, e);
        }
        catch (NoSuchArchiverException e) {
            throw new Exception("Unknown extension: " + source.getPath());
        }
    }

    protected void checkTargetDirectory(File targetDirectory) throws Exception {
        if (failIfNotExit) {
            if (!targetDirectory.exists()) {
                throw new Exception("targetDirectory " + targetDirectory + " does not exist " + targetDirectory);
            }
        } else {
            targetDirectory.mkdirs();
        }
        if (!targetDirectory.isDirectory()) {
            throw new Exception("targetDirectory: " + targetDirectory + " is not a directory");
        }
    }
}
