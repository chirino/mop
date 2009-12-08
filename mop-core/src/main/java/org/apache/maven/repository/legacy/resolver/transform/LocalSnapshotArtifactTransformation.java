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
package org.apache.maven.repository.legacy.resolver.transform;

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.DefaultRepositoryRequest;
import org.apache.maven.artifact.repository.RepositoryRequest;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.SnapshotArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;

/**
 * SnapshotArtifactTransformation
 * <p>
 * This transformation is here because the lates version of artifacts found in
 * our local .m2 repo may have the local flag set in their versioning which
 * causes Maven to skip copying it to the mop repo.
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
@SuppressWarnings("deprecation")
public class LocalSnapshotArtifactTransformation implements ArtifactTransformation {

    private String localRepoId;

    public void setLocalRepoId(String localRepoId) {
        this.localRepoId = localRepoId;
    }

    public void transformForResolve(Artifact artifact, RepositoryRequest request) throws ArtifactResolutionException, ArtifactNotFoundException {
        if (localRepoId == null) {
            return;
        }

        // Only select snapshots that are unresolved (eg 1.0-SNAPSHOT, not 1.0-20050607.123456)
        if (artifact.isSnapshot() && artifact.getBaseVersion().equals(artifact.getVersion())) {
            for (ArtifactMetadata m : artifact.getMetadataList()) {

                if (m instanceof SnapshotArtifactRepositoryMetadata) {
                    SnapshotArtifactRepositoryMetadata snapshotMetadata = (SnapshotArtifactRepositoryMetadata) m;
                    
                    if(snapshotMetadata.getRepository() == null)
                    {
                        continue;
                    }
                    
                    if (localRepoId.equals(snapshotMetadata.getRepository().getId())) {

                        Metadata metadata = snapshotMetadata.getMetadata();

                        if (metadata != null) {
                            Versioning versioning = metadata.getVersioning();

                            if (versioning != null) {
                                Snapshot snapshot = versioning.getSnapshot();

                                if (snapshot != null && snapshot.isLocalCopy()) {
                                    snapshot.setLocalCopy(false);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.maven.repository.legacy.resolver.transform.ArtifactTransformation
     * #transformForResolve(org.apache.maven.artifact.Artifact, java.util.List,
     * org.apache.maven.artifact.repository.ArtifactRepository)
     */
    public void transformForResolve(Artifact artifact, List<ArtifactRepository> remoteRepositories, ArtifactRepository localRepository) throws ArtifactResolutionException, ArtifactNotFoundException {
        RepositoryRequest request = new DefaultRepositoryRequest();
        request.setLocalRepository(localRepository);
        request.setRemoteRepositories(remoteRepositories);
        transformForResolve(artifact, request);
    }

    public void transformForInstall(Artifact artifact, ArtifactRepository localRepository) {
        // metadata is added via addPluginArtifactMetadata
    }

    public void transformForDeployment(Artifact artifact, ArtifactRepository remoteRepository, ArtifactRepository localRepository) {
        // metadata is added via addPluginArtifactMetadata
    }

}
