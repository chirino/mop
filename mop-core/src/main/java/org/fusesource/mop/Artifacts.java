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
