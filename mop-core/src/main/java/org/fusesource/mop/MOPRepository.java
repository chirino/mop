/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.mop;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.LinkedHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.repository.RepositorySystem;

import org.codehaus.plexus.ContainerConfiguration;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.classworlds.ClassWorld;

import org.fusesource.mop.support.ArtifactId;
import org.fusesource.mop.support.Database;
import org.fusesource.mop.support.Logger;

/**
 * @author chirino
 */
public class MOPRepository {

    private static final transient Log LOG = LogFactory.getLog(MOPRepository.class);
    public static final String MOP_BASE = "mop.base";
    private static final Object lock = new Object();

    private String scope = System.getProperty("mop.scope", "runtime");
    private boolean online = System.getProperty("mop.online", "true").equals("true");
    private boolean transitive = System.getProperty("mop.include.transitive", "true").equals("true");
    private boolean includeOptional = System.getProperty("mop.include.includeOptional", "false").equals("true");
    private String localRepoUpdatePolicy = System.getProperty("mop.repo.local.check", ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS);

    private PlexusContainer container;
    private File localRepo;
    private LinkedHashMap<String, String> remoteRepositories = getDefaultRepositories();


    /**
     * @param artifactIds
     * @return a list of error messages if the uninstall cannot be performed
     * @throws Exception
     */
    public List<String> uninstall(final List<ArtifactId> artifactIds) throws Exception {
        
        final ArrayList<String> errorList = new ArrayList<String>();
        database(false, new DBCallback() {
            public void execute(Database database) throws Exception {
                for (ArtifactId artifactId : artifactIds) {
                    TreeSet<String> deps = database.listDependenants(artifactId.toString());
                    if (deps == null) {
                        errorList.add(artifactId.toString() + ": is not installed.\n");
                    } else if (!deps.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append(artifactId.toString() + ": is used by\n");
                        for (String dep : deps) {
                            sb.append("  * " + dep + "\n");
                        }
                        errorList.add(sb.toString());
                    }
                }

                if (!errorList.isEmpty()) {
                    return;
                }

                for (ArtifactId artifactId : artifactIds) {
                    TreeSet<String> unused = database.uninstall(artifactId.toString());
                    System.out.println("TODO:");
                    for (String dep : unused) {
                        System.out.println(" rm " + dep);
                        // TODO: need to remove these deps from the file system.
                    }
                }

            }
        });
        return errorList;
    }

    /**
     * Provides a listing of all mop installed artifacts. The type arguement can
     * be either "all" or "installed".
     * 
     * @param type
     * @return
     * @throws IllegalArgumentException
     * @throws IOException
     */
    public Set<ArtifactId> list(String type) throws Exception {

        if (type == null) {
            type = "installed";
        }
        final String t = type;
        
        final HashSet<ArtifactId> rc = new HashSet<ArtifactId>();
        database(true, new DBCallback() {
            public void execute(Database database) throws Exception {
                if (t.equals("installed")) {
                    Set<String> list = database.listInstalled();
                    for (String s : list) {
                        ArtifactId id = ArtifactId.strictParse(s);
                        rc.add(id);
                    }
                } else if (t.equals("all")) {
                    Set<String> list = database.listAll();
                    for (String s : list) {
                        ArtifactId id = ArtifactId.strictParse(s);
                        rc.add(id);
                    }
                } else {
                    throw new IllegalArgumentException("all|installed expected");
                }
            }
        });
        return rc;
    }

    // ----------------------------------------------------------------
    // createArtifactClassLoader method variations
    // ----------------------------------------------------------------

    /**
     * Returns a new class loader from the given dependencies
     */
    public URLClassLoader createArtifactClassLoader(ClassLoader parent, String... artifactIds) throws Exception {
        return createArtifactClassLoader(null, parent, toArtifactIds(artifactIds));
    }

    /**
     * Returns a new class loader from the given dependencies
     */
    public URLClassLoader createArtifactClassLoader(ClassLoader parent, ArtifactId... artifactIds) throws Exception {
        return createArtifactClassLoader(null, parent, Arrays.asList(artifactIds));
    }

    /**
     * Returns a new class loader from the given dependencies
     */
    public URLClassLoader createArtifactClassLoader(ClassLoader parent, List<ArtifactId> artifactIds) throws Exception {
        return createArtifactClassLoader(null, parent, artifactIds);
    }


    /**
     * Returns a new class loader from the given dependencies
     */
    public URLClassLoader createArtifactClassLoader(ArtifactFilter filter, ClassLoader parent, ArtifactId... artifactIds) throws Exception {
        return createArtifactClassLoader(filter, parent, Arrays.asList(artifactIds));
    }

    /**
     * Returns a new class loader from the given dependencies
     */
    public URLClassLoader createArtifactClassLoader(ArtifactFilter filter, ClassLoader parent, List<ArtifactId> artifactIds) throws Exception {
        return createFileClassLoader(parent, resolveFiles(filter, artifactIds));
    }

    // ----------------------------------------------------------------
    // copy method variations
    // ----------------------------------------------------------------
    public void copy(File targetDir, String... artifactIds) throws Exception {
        copy(null, targetDir, toArtifactIds(artifactIds));
    }

    public void copy(File targetDir, ArtifactId... artifactIds) throws Exception {
        copy(null, targetDir, Arrays.asList(artifactIds));
    }

    public void copy(File targetDir, List<ArtifactId> artifactIds) throws Exception {
        copy(null, targetDir, artifactIds);
    }

    public void copy(ArtifactFilter filter, File targetDir, ArtifactId... artifactIds) throws Exception {
        copy(filter, targetDir, Arrays.asList(artifactIds));
    }

    public void copy(ArtifactFilter filter, File targetDir, List<ArtifactId> artifactIds) throws Exception {
        List<File> dependencies = resolveFiles(artifactIds);
        if (!targetDir.isDirectory()) {
            throw new IOException("target is not a directroy: " + targetDir);
        }

        for (File dependency : dependencies) {
            Logger.debug("copying: " + dependency + " to " + targetDir);
            FileInputStream is = new FileInputStream(dependency);
            try {
                FileOutputStream os = new FileOutputStream(new File(targetDir, dependency.getName()));
                try {
                    copy(is, os);
                } finally {
                    try {
                        os.close();
                    } catch (IOException ignore) {
                    }
                }
            } finally {
                try {
                    is.close();
                } catch (IOException ignore) {
                }
            }

        }
    }

    // ----------------------------------------------------------------
    // classpath method variations
    // ----------------------------------------------------------------

    public String classpath(String... artifactIds) throws Exception {
        return classpath(null, toArtifactIds(artifactIds));
    }

    public String classpath(ArtifactId... artifactIds) throws Exception {
        return classpath(null, Arrays.asList(artifactIds));
    }
    
    public String classpath(List<ArtifactId> artifactIds) throws Exception {
        return classpath(null, artifactIds);
    }

    public String classpath(ArtifactFilter filter, ArtifactId... artifactIds) throws Exception {
        return classpath(filter, Arrays.asList(artifactIds));
    }

    public String classpath(ArtifactFilter filter, List<ArtifactId> artifactIds) throws Exception {
        return classpathFiles(resolveFiles(filter, artifactIds));
    }

    // ----------------------------------------------------------------
    // resolveFiles method variations
    // ----------------------------------------------------------------

    public List<File> resolveFiles(String... artifactIds) throws Exception {
        return resolveFiles(null, toArtifactIds(artifactIds));
    }

    public List<File> resolveFiles(ArtifactId... artifactIds) throws Exception {
        return resolveFiles(null, Arrays.asList(artifactIds));
    }

    public List<File> resolveFiles(List<ArtifactId> artifactIds) throws Exception {
        return resolveFiles(null, artifactIds);
    }

    public List<File> resolveFiles(ArtifactFilter filter, ArtifactId... artifactIds) throws Exception {
        return resolveFiles(filter, Arrays.asList(artifactIds));
    }

    public List<File> resolveFiles(ArtifactFilter filter, List<ArtifactId> artifactIds) throws Exception {
        Set<Artifact> artifacts = resolveArtifacts(filter, artifactIds);
        ArrayList<File> files = new ArrayList<File>(artifacts.size());
        for (Artifact a : artifacts) {
            files.add(a.getFile());
        }
        return files;
    }
    // ----------------------------------------------------------------
    // resolveArtifacts method variations
    // ----------------------------------------------------------------

    public Set<Artifact> resolveArtifacts(String... artifactIds) throws Exception {
        return resolveArtifacts(null, toArtifactIds(artifactIds));
    }

    public Set<Artifact> resolveArtifacts(ArtifactId... artifactIds) throws Exception {
        return resolveArtifacts(null, Arrays.asList(artifactIds));
    }

    public Set<Artifact> resolveArtifacts(List<ArtifactId> artifactIds) throws Exception {
        return resolveArtifacts(null, artifactIds);
    }

    public Set<Artifact> resolveArtifacts(ArtifactFilter filter, ArtifactId... artifactIds) throws Exception {
        return resolveArtifacts(filter, Arrays.asList(artifactIds));
    }

    public Set<Artifact> resolveArtifacts(ArtifactFilter filter, List<ArtifactId> artifactIds) throws Exception {
        LinkedHashSet<Artifact> artifacts = new LinkedHashSet<Artifact>();
        for (ArtifactId id : artifactIds) {
            artifacts.addAll(resolveArtifacts(filter, id));
        }
        return artifacts;
    }

    public Set<Artifact> resolveArtifacts(ArtifactFilter filter, final ArtifactId id) throws Exception, InvalidRepositoryException {
        Logger.debug("Resolving artifact " + id);

        RepositorySystem repositorySystem = (RepositorySystem) getContainer().lookup(RepositorySystem.class);

        List<ArtifactRepository> remoteRepositories = createRemoteRepositories(repositorySystem);
        ArtifactRepository localRepository = createLocalRepository(repositorySystem, "mop.local", getLocalRepo().getAbsolutePath(), false);

        // If group id is not set.. we can still look it up in the db
        // of installed artifacs.
        if (id.getGroupId() == null) {
            database(true, new DBCallback() {
                public void execute(Database database) throws Exception {

                    // Makes groupId includeOptional.. we look it up in the database.
                    if (id.getGroupId() == null) {
                        Map<String, Set<String>> rc = database.groupByGroupId(database.findByArtifactId(id.getArtifactId()));
                        if (rc.isEmpty()) {
                            throw new Exception("Please qualify a group id: No local artifacts match: "+id.getArtifactId());
                        }
                        if (rc.size() > 1) {
                            System.out.println("Local artifacts that match:");
                            for (String s : rc.keySet()) {
                                System.out.println("   " + s + ":" + id.getArtifactId());
                            }
                            throw new Exception("Please qualify a group id: Multiple local artifacts match: "+id);
                        }
                        id.setGroupId(rc.keySet().iterator().next());
                        Logger.debug("Resolving artifact " + id);
                    }

                }
            });
        }

        if (online) {

            // Keep track that we are trying an install..
            // If an install dies midway.. the repo will have partlly installed dependencies...
            // we may want to continue the install??
            // database.beginInstall(id.toString());

        }

        Artifact artifact = repositorySystem.createArtifactWithClassifier(id.getGroupId(), id.getArtifactId(), id.getVersion(), id.getType(), id.getClassifier());

        // Setup the filters which will constrain the resulting dependencies..
        List<ArtifactFilter> constraints = new ArrayList<ArtifactFilter>();
        constraints.add(new ScopeArtifactFilter(scope));
        if( !includeOptional) {
            constraints.add(new ArtifactFilter() {
                public boolean include(Artifact artifact) {
                    return !artifact.isOptional();
                }
            });
        }
        if( filter!=null ) {
            constraints.add(filter);
        }

        ArtifactFilter filters = new AndArtifactFilter(constraints);

        ArtifactResolutionRequest request = new ArtifactResolutionRequest()
                .setArtifact(artifact)
                .setResolveRoot(true)
                .setResolveTransitively(isTransitive())
                .setLocalRepository(localRepository)
                .setRemoteRepositories(remoteRepositories)
                .setOffline(!online)
                .setCollectionFilter(filters);

        ArtifactResolutionResult result = repositorySystem.resolve(request);

        List<Artifact> list = result.getMissingArtifacts();
        if (!list.isEmpty()) {
            throw new Exception("The following artifacts could not be downloaded: " + list);
        }

        Set<Artifact> rc = result.getArtifacts();
        if (online) {
            // Have the DB index the installed the artifacts.
            final LinkedHashSet<String> installed = new LinkedHashSet<String>();
            for (Artifact a : rc) {
                installed.add(a.getId());
            }
            database(false, new DBCallback() {
                public void execute(Database database) throws Exception {
                    database.install(installed);
                }
            });
        }

        if( Logger.isDebug() ) {
            Logger.debug("  Resolved: "+id);
            for (Artifact a : rc) {
                Logger.debug("    depends on: " + a.getId() + ", scope: " + a.getScope() +", optional: "+a.isOptional()+ ", file: " + a.getFile());
            }
        }

        return rc;

    }

    // ----------------------------------------------------------------
    // Helper implementation methods
    // ----------------------------------------------------------------

    static interface DBCallback {
        void execute(Database database) throws Exception;
    }

    private void database(boolean readOnly, DBCallback c) throws Exception {
        Database database = new Database();
        database.setDirectroy(new File(getLocalRepo(), ".index"));
        database.open(readOnly);
        try {
            if( readOnly ) {
                synchronized (lock) {
                    c.execute(database);
                }
            } else {
                c.execute(database);
            }
        } finally {
            database.close();
        }
    }

    static private List<ArtifactId> toArtifactIds(String... artifactIds) {
        return toArtifactIds(Arrays.asList(artifactIds));
    }
    
    static private List<ArtifactId> toArtifactIds(List<String> artifactIds) {
        ArrayList<ArtifactId> rc = new ArrayList<ArtifactId>(artifactIds.size());
        for (String id : artifactIds) {
            rc.add(ArtifactId.parse(id));
        }
        return rc;
    }


    private static void copy(InputStream is, OutputStream os) throws IOException {
        byte buffer[] = new byte[1024 * 4];
        int c;
        while ((c = is.read(buffer)) > 0) {
            os.write(buffer, 0, c);
        }
    }

    /**
     * Returns a new class loader from the given dependencies
     */
    public static URLClassLoader createFileClassLoader(ClassLoader parent, List<File> dependencies) throws MalformedURLException {
        List<URL> urls = new ArrayList<URL>();
        for (File file : dependencies) {
            urls.add(file.toURL());
        }

        URL[] urlArray = urls.toArray(new URL[urls.size()]);
        if (parent == null) {
            parent = Object.class.getClassLoader();
        }
        return new URLClassLoader(urlArray, parent);
    }

    static String classpathFiles(List<File> files) {
        StringBuilder buffer = new StringBuilder();
        boolean first = true;
        for (File file : files) {
            if (first) {
                first = false;
            } else {
                buffer.append(File.pathSeparator);
            }
            buffer.append(file);
        }
        return buffer.toString();
    }

    /**
     * Adds some default remote repositories
     * 
     * @param repositorySystem
     */
    protected List<ArtifactRepository> createRemoteRepositories(RepositorySystem repositorySystem) {

        List<ArtifactRepository> rc = new ArrayList<ArtifactRepository>();

        String mavenRepositoryDir = System.getProperty("user.home", ".") + "/.m2/repository";
        rc.add(createLocalRepository(repositorySystem, RepositorySystem.DEFAULT_LOCAL_REPO_ID, mavenRepositoryDir, true));

        DefaultRepositoryLayout layout = new DefaultRepositoryLayout();
        ArtifactRepositoryPolicy repositoryPolicy = new ArtifactRepositoryPolicy();

        if( online ) {
            repositoryPolicy.setUpdatePolicy(ArtifactRepositoryPolicy.UPDATE_POLICY_DAILY);
        } else {
            repositoryPolicy.setUpdatePolicy(ArtifactRepositoryPolicy.UPDATE_POLICY_NEVER);
        }

        for (Map.Entry<String, String> entry : remoteRepositories.entrySet()) {
            rc.add(repositorySystem.createArtifactRepository(entry.getKey(), entry.getValue(), layout, repositoryPolicy, repositoryPolicy));
        }

        return rc;
    }

    private ArtifactRepository createLocalRepository(RepositorySystem repositorySystem, String id, String path, boolean asRemote) {
        // This hack needed since the local repo is being accessed as a remote repo.
        final ArtifactRepository localRepository[] = new ArtifactRepository[1];
        DefaultRepositoryLayout layout = null;
        if (asRemote) {
            layout = new DefaultRepositoryLayout() {
                @Override
                public String pathOfRemoteRepositoryMetadata(ArtifactMetadata metadata) {
                    return super.pathOfLocalRepositoryMetadata(metadata, localRepository[0]);
                }
            };
        }

        //Always check local repo for updates:
        ArtifactRepositoryPolicy repositoryPolicy = new ArtifactRepositoryPolicy();
        if( online ) {
            repositoryPolicy.setUpdatePolicy(localRepoUpdatePolicy);
        } else {
            repositoryPolicy.setUpdatePolicy(ArtifactRepositoryPolicy.UPDATE_POLICY_NEVER);
        }
        repositoryPolicy.setChecksumPolicy(ArtifactRepositoryPolicy.CHECKSUM_POLICY_IGNORE);
        localRepository[0] = repositorySystem.createArtifactRepository(id, "file://" + path, layout, repositoryPolicy, repositoryPolicy);
        ArtifactRepository repository = localRepository[0];
        return repository;
    }


    private static LinkedHashMap<String, String> getDefaultRepositories() {
        LinkedHashMap<String, String> rc = new LinkedHashMap<String, String>();

        // We could propably load a property file from the base dir to get this list.
        rc.put(RepositorySystem.DEFAULT_REMOTE_REPO_ID, RepositorySystem.DEFAULT_REMOTE_REPO_URL);
        rc.put("fusesource.m2", "http://repo.fusesource.com/maven2");
        rc.put("fusesource.m2-snapshot", "http://repo.fusesource.com/maven2-snapshot");

        // TODO we can remove these when we get consolidation of forge repos?
        rc.put("cloudmix.snapshot", "http://cloudmix.fusesource.org/repo/snapshot");
        rc.put("cloudmix.release", "http://cloudmix.fusesource.org/repo/release");
        rc.put("mop.snapshot", "http://mop.fusesource.org/repo/snapshot");
        rc.put("mop.release", "http://mop.fusesource.org/repo/release");

        return rc;
    }


    // ----------------------------------------------------------------
    // Properties
    // ----------------------------------------------------------------

    public PlexusContainer getContainer() {
        if (container == null) {
            try {
                ClassWorld classWorld = new ClassWorld("plexus.core", Thread.currentThread().getContextClassLoader());
                ContainerConfiguration configuration = new DefaultContainerConfiguration().setClassWorld(classWorld);
                container = new DefaultPlexusContainer(configuration);
            } catch (PlexusContainerException e) {
                throw new RuntimeException(e);
            }
        }
        return container;
    }

    public void setContainer(PlexusContainer container) {
        this.container = container;
    }

    public File getLocalRepo() {
        if (localRepo == null) {
            if (System.getProperty(MOP_BASE) != null) {
                localRepo = new File(System.getProperty(MOP_BASE) + File.separator + "repository");
            } else {
                localRepo = new File(System.getProperty("user.home", "."), ".mop" + File.separator + "repository");
                String warnDir = localRepo.toString();
                try {
                    warnDir = localRepo.getCanonicalPath();
                } catch (Exception e) {
                }
                LOG.warn("No "+MOP_BASE+" system property defined so setting local repo to: " + warnDir);
            }
        }
        return localRepo;
    }

    public void setLocalRepo(File localRepo) {
        this.localRepo = localRepo;
    }

    public boolean isAlwaysCheckUserLocalRepo() {
        return ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS.equals(localRepoUpdatePolicy);
    }

    public void setAlwaysCheckUserLocalRepo(boolean alwaysCheckUserLocalRepo) {
        if( alwaysCheckUserLocalRepo ) {
            localRepoUpdatePolicy = ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS;
        } else {
            localRepoUpdatePolicy = ArtifactRepositoryPolicy.UPDATE_POLICY_NEVER;
        }
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public boolean isTransitive() {
        return transitive;
    }

    public void setTransitive(boolean transitive) {
        this.transitive = transitive;
    }

    public LinkedHashMap<String, String> getRemoteRepositories() {
        return remoteRepositories;
    }

    public void setRemoteRepositories(LinkedHashMap<String, String> remoteRepositories) {
        this.remoteRepositories = remoteRepositories;
    }

    public boolean isIncludeOptional() {
        return includeOptional;
    }

    public void setIncludeOptional(boolean includeOptional) {
        this.includeOptional = includeOptional;
    }
}