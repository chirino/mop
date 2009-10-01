/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.mop;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;

/**
 * A simple API for injecting a collection of artifacts into a command method as a parameter
 * which can be used to extract either the list of artifact objects or the files.
 *
 * @version $Revision: 1.1 $
 */
public abstract class Artifacts {
    private Set<Artifact> artifacts;
    private List<File> files;

    public List<File> getFiles() throws Exception {
        if (files == null) {
            files = createFiles();
        }
        return files;
    }

    public Set<Artifact> getArtifacts() throws Exception {
        if (artifacts == null) {
            artifacts = createArtifacts();
        }
        return artifacts;
    }

    protected abstract List<File> createFiles() throws Exception;

    protected abstract Set<Artifact> createArtifacts() throws Exception;
}
