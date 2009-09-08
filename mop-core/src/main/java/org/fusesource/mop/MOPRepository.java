/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.mop;

import com.google.common.base.Nullable;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
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
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.*;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.fusesource.mop.support.ArtifactId;
import org.fusesource.mop.support.Database;
import org.fusesource.mop.support.Logger;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * @author chirino
 */
public class MOPRepository {

    private static final transient Log LOG = LogFactory.getLog(MOPRepository.class);
    public static final String MOP_BASE = "mop.base";
    private static final Object lock = new Object();

    private PlexusContainer container;
    private String scope = "compile";
    private File localRepo;
    private String[] remoteRepos;
    private boolean online = true;
    private boolean transitive = true;
    private boolean alwaysCheckUserLocalRepo = false;

    private HashMap<String, String> remoteRepositories = getDefaultRepositories();

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
                        ArtifactId id = new ArtifactId();
                        id.strictParse(s);
                        rc.add(id);
                    }
                } else if (t.equals("all")) {
                    Set<String> list = database.listAll();
                    for (String s : list) {
                        ArtifactId id = new ArtifactId();
                        id.strictParse(s);
                        rc.add(id);
                    }
                } else {
                    throw new IllegalArgumentException("all|installed expected");
                }
            }
        });
        return rc;
    }

    /**
     * Returns a new class loader from the given dependencies
     */
    public URLClassLoader createArtifactClassLoader(ClassLoader parent, List<ArtifactId> artifactIds) throws Exception {
        return createFileClassLoader(parent, resolveFiles(artifactIds));
    }

    /**
     * Returns a new class loader from the given dependencies
     */
    public URLClassLoader createArtifactClassLoader(ClassLoader parent, ArtifactId... artifactIds) throws Exception {
        return createFileClassLoader(parent, resolveFiles(artifactIds));
    }

    /**
     * } Returns a new class loader from the given dependencies
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

    public void copy(List<ArtifactId> artifactIds, File targetDir) throws Exception {
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

    private static void copy(InputStream is, OutputStream os) throws IOException {
        byte buffer[] = new byte[1024 * 4];
        int c;
        while ((c = is.read(buffer)) > 0) {
            os.write(buffer, 0, c);
        }
    }

    public String classpath(List<ArtifactId> artifactIds) throws Exception {
        List<File> files = resolveFiles(artifactIds);
        return classpathFiles(files);
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

    public List<File> resolveFiles(ArtifactId... artifactIds) throws Exception {
        return resolveFiles(Arrays.asList(artifactIds));
    }

    public List<File> resolveFiles(List<ArtifactId> artifactIds) throws Exception {
        return resolveFiles(artifactIds, Predicates.<Artifact> alwaysTrue());
    }

    public List<File> resolveFiles(List<ArtifactId> artifactIds, Predicate<Artifact> filter) throws Exception {
        Set<Artifact> artifacts = resolveArtifacts(artifactIds);

        Predicate<Artifact> matchingArtifacts = Predicates.and(filter, new Predicate<Artifact>() {
            public boolean apply(@Nullable Artifact artifact) {
                String artifactScope = artifact.getScope();
                return matchesScope(scope, artifactScope);
            }
        });

        List<File> files = new ArrayList<File>();
        for (Artifact a : artifacts) {
            String artifactScope = a.getScope();
            if (matchingArtifacts.apply(a)) {
                File file = a.getFile();
                files.add(file);
                Logger.debug("    depends on: " + a.getGroupId() + " / " + a.getArtifactId() + " / " + a.getVersion() + " scope: " + artifactScope + " file: " + file);
            } else {
                Logger.debug("    not in scope: " + a.getGroupId() + " / " + a.getArtifactId() + " / " + a.getVersion() + " scope: " + artifactScope);
            }

        }
        return files;
    }

    public Set<Artifact> resolveArtifacts(List<ArtifactId> artifactIds) throws Exception {
        LinkedHashSet<Artifact> artifacts = new LinkedHashSet<Artifact>();
        for (ArtifactId id : artifactIds) {
            artifacts.addAll(resolveArtifacts(id));
        }
        return artifacts;
    }

    public Set<Artifact> resolveArtifacts(ArtifactId... artifactIds) throws Exception {
        LinkedHashSet<Artifact> artifacts = new LinkedHashSet<Artifact>();
        for (ArtifactId id : artifactIds) {
            artifacts.addAll(resolveArtifacts(id));
        }
        return artifacts;
    }


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

    // Implementation methods
    //-------------------------------------------------------------------------

    private Set<Artifact> resolveArtifacts(final ArtifactId id) throws Exception, InvalidRepositoryException {
        Logger.debug("Resolving artifact " + id);

        RepositorySystem repositorySystem = (RepositorySystem) getContainer().lookup(RepositorySystem.class);
        List<ArtifactRepository> remoteRepoList = new ArrayList<ArtifactRepository>();
        if (online) {
            addDefaultRemoteRepos(repositorySystem, remoteRepoList);
            if (remoteRepos != null) {
                int counter = 1;
                ArtifactRepositoryPolicy repositoryPolicy = new ArtifactRepositoryPolicy();
                DefaultRepositoryLayout layout = new DefaultRepositoryLayout();
                for (String remoteRepo : remoteRepos) {
                    String repoid = "repo" + (counter++);
                    Logger.debug("Adding repository with id: " + id + " url: " + remoteRepo);
                    ArtifactRepository repo = repositorySystem.createArtifactRepository(repoid, remoteRepo, layout, repositoryPolicy, repositoryPolicy);
                    remoteRepoList.add(repo);
                }
            }
            remoteRepoList.add(repositorySystem.createDefaultRemoteRepository());
        }

        ArtifactRepository localRepository = createLocalRepository(repositorySystem, "mop.local", getLocalRepo().getAbsolutePath(), false);

        // If group id is not set.. we can still look it up in the db
        // of installed artifacs.
        if (id.getGroupId() == null) {
            database(true, new DBCallback() {
                public void execute(Database database) throws Exception {

                    // Makes groupId optional.. we look it up in the database.
                    if (id.getGroupId() == null) {
                        Map<String, Set<String>> rc = database.groupByGroupId(database.findByArtifactId(id.getArtifactId()));
                        if (rc.isEmpty()) {
                            throw new Exception("Please qualify a group id: No local artifacts match: "+id);
                        }
                        if (rc.size() > 1) {
                            System.out.println("Local artifacts that match:");
                            for (String s : rc.keySet()) {
                                System.out.println("   " + s + ":" + id.getArtifactId());
                            }
                            throw new Exception("Please qualify a group id: Multiple local artifacts match: "+id);
                        }
                        id.setGroupId(rc.keySet().iterator().next());

                        // We could propably figure out the classifier/type/version too..
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
        ArtifactResolutionRequest request = new ArtifactResolutionRequest()
                .setArtifact(artifact)
                .setResolveRoot(true)
                .setResolveTransitively(isTransitive())
                .setLocalRepository(localRepository)
                .setRemoteRepositories(remoteRepoList);

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
        return rc;

    }


    /**
     * Adds some default remote repositories
     * 
     * @param repositorySystem
     * @param remoteRepoList
     */
    protected void addDefaultRemoteRepos(RepositorySystem repositorySystem, List<ArtifactRepository> remoteRepoList) {

        String mavenRepositoryDir = System.getProperty("user.home", ".") + "/.m2/repository";
        remoteRepoList.add(createLocalRepository(repositorySystem, "local", mavenRepositoryDir, true));

        DefaultRepositoryLayout layout = new DefaultRepositoryLayout();
        ArtifactRepositoryPolicy repositoryPolicy = new ArtifactRepositoryPolicy();
        repositoryPolicy.setUpdatePolicy(ArtifactRepositoryPolicy.UPDATE_POLICY_NEVER);


        for (Map.Entry<String, String> entry : remoteRepositories.entrySet()) {
            remoteRepoList.add(repositorySystem.createArtifactRepository(entry.getKey(), entry.getValue(), layout, repositoryPolicy, repositoryPolicy));
        }
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
        if (alwaysCheckUserLocalRepo) {
            repositoryPolicy = new ArtifactRepositoryPolicy();
            repositoryPolicy.setUpdatePolicy(ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS);
        }
        repositoryPolicy.setChecksumPolicy(ArtifactRepositoryPolicy.CHECKSUM_POLICY_IGNORE);
        localRepository[0] = repositorySystem.createArtifactRepository(id, "file://" + path, layout, repositoryPolicy, repositoryPolicy);
        ArtifactRepository repository = localRepository[0];
        return repository;
    }

    /**
     * Returns true if the given artifactScope matches the current scope setting
     * (which defaults to 'compile') to choose the exact dependencies to add to
     * the classpath
     * 
     * @param scope
     * @param artifactScope
     * @return
     */
    protected boolean matchesScope(String scope, String artifactScope) {
        // TODO is there a special Maven way to test this???
        return artifactScope == null || artifactScope.equals(scope) || artifactScope.equals("compile") || artifactScope.equals("provided");
    }

    private HashMap<String, String> getDefaultRepositories() {
        HashMap<String, String> rc = new HashMap<String, String>();
        rc.put("fusesource.m2", "http://repo.fusesource.com/maven2");
        rc.put("fusesource.m2-snapshot", "http://repo.fusesource.com/maven2-snapshot");

        // TODO we can remove these when we get consolidation of forge repos?
        rc.put("cloudmix.snapshot", "http://cloudmix.fusesource.org/repo/snapshot");
        rc.put("cloudmix.release", "http://cloudmix.fusesource.org/repo/release");
        rc.put("mop.snapshot", "http://mop.fusesource.org/repo/snapshot");
        rc.put("mop.release", "http://mop.fusesource.org/repo/release");
        return rc;
    }


    // Properties
    //-------------------------------------------------------------------------

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
        return alwaysCheckUserLocalRepo;
    }

    public void setAlwaysCheckUserLocalRepo(boolean alwaysCheckUserLocalRepo) {
        this.alwaysCheckUserLocalRepo = alwaysCheckUserLocalRepo;
    }

    public String[] getRemoteRepos() {
        return remoteRepos;
    }

    public void setRemoteRepos(String[] remoteRepos) {
        this.remoteRepos = remoteRepos;
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

    public HashMap<String, String> getRemoteRepositories() {
        return remoteRepositories;
    }

    public void setRemoteRepositories(HashMap<String, String> remoteRepositories) {
        this.remoteRepositories = remoteRepositories;
    }
}