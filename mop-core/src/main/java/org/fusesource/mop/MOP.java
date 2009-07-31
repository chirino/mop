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
import com.google.common.collect.Sets;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.InvalidRepositoryException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.tools.cli.AbstractCli;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Runs a Java class from an artifact loaded from the local maven repository
 * using optional remote repositories.
 */
public class MOP extends AbstractCli {

    private static final transient Log LOG = LogFactory.getLog(org.fusesource.mop.MOP.class);

    public static final String DEFAULT_VERSION = "RELEASE";
    public static final String DEFAULT_TYPE = "jar";

    private Options options;
    private String scope;
    private String localRepo;
    private String[] remoteRepos;
    private String className;
    private boolean online;

    private ArrayList<ArtifactId> artifactIds;
    private List<String> reminingArgs;
    private Map<String, Command> commands;
    private String defaultVersion = DEFAULT_VERSION;
    private String defaultType = DEFAULT_TYPE;

    public static void main(String[] args) {
        org.fusesource.mop.MOP mavenRunner = new org.fusesource.mop.MOP();
        System.exit(mavenRunner.execute(args));
    }


    public Options buildCliOptions(Options options) {
        this.options = options;
        options.addOption("l", "local", true, "Specifies the local mop repo");
        options.addOption("r", "repo", true, "Specifies a remote maven repo");
        options.addOption("o", "online", true, "Toggle online mode");
        options.addOption("s", "scope", true, "Maven scope of transitive dependencies to include, defaults to 'compile'");
        return options;
    }

    @Override
    public void displayHelp() {
        System.out.println();
        System.out.println("mop: http://mop.fusesource.org/");
        System.out.println();
        System.out.println("mop is a tool for running Java code on the command line");
        System.out.println("using maven repositories to download code and create classpaths");
        System.out.println();

        checkCommandsLoaded();

        HelpFormatter formatter = new HelpFormatter();

        StringBuilder buffer = new StringBuilder();
        for (Command command : commands.values()) {
            buffer.append(String.format("\n mop [options] %-20s %s", command.getName(), removeNewLines(command.getOptions())));
        }
        formatter.printHelp(buffer.toString(), "\nOptions:", options, "\n");

        System.out.println();
        System.out.println("<artifact> is of the format: groupId:artifactId[[:type[:classifier]]:version] [+<artifact>]");
        System.out.println();
        System.out.println("Commands:");

        for (Map.Entry<String, Command> entry : commands.entrySet()) {
            String description = removeNewLines(entry.getValue().getDescription());
            // lets remove any newlines
            System.out.printf("\t%-20s : %s\n", entry.getKey(), description);
        }

        System.out.println();
    }

    /**
     * Removes any newlines in the text so its one big line
     */
    private String removeNewLines(String text) {
        StringBuilder buffer = new StringBuilder();
        BufferedReader reader = new BufferedReader(new StringReader(text));
        boolean first = true;
        while (true) {
            try {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                if (first) {
                    first = false;
                } else {
                    buffer.append(" ");
                }
                buffer.append(line.trim());
            } catch (IOException e) {
                LOG.warn("This should never happen. Caught: " + e, e);
            }
        }
        return buffer.toString();
    }

    public void invokePlexusComponent(CommandLine cli, PlexusContainer container) throws Exception {
        // lets process the options
        Logger.debug = cli.hasOption('X');
        System.out.println("Debug: " + Logger.debug);
        scope = cli.getOptionValue('s', "compile");
        localRepo = cli.getOptionValue('l');
        remoteRepos = cli.getOptionValues('r');

        // now the remaining command line args
        try {
            LinkedList<String> argList = new LinkedList<String>(cli.getArgList());
            processCommandLine(container, argList);
        } catch (UsageException e) {
            displayHelp();
            throw e;
        } catch (Throwable e) {
            System.out.println("Failed: " + e);
            e.printStackTrace();
            Set<Throwable> exceptions = Sets.newHashSet(e);
            for (int i = 0; i < 10; i++) {
                e = e.getCause();
                if (e != null && exceptions.add(e)) {
                    System.out.println("Reason: " + e);
                    e.printStackTrace();
                } else {
                    break;
                }
            }
        }
    }

    protected void processCommandLine(PlexusContainer container, LinkedList<String> argList) throws Exception {
        // lets reset values in case we chain things together...
        defaultVersion = DEFAULT_VERSION;
        defaultType = DEFAULT_TYPE;

        if (argList.isEmpty()) {
            displayHelp();
            return;
        }
        String command = argList.removeFirst();
        if (command.equals("exec")) {
            execCommand(container, argList);
        } else if (command.equals("execjar")) {
            execJarCommand(container, argList);
        } else if (command.equals("jar")) {
            jarCommand(container, argList);
        } else if (command.equals("run")) {
            runCommand(container, argList);
        } else if (command.equals("echo")) {
            echoCommand(container, argList);
        } else if (command.equals("classpath")) {
            classpathCommand(container, argList);
        } else if (command.equals("copy")) {
            copyCommand(container, argList);
        } else if (command.equals("war")) {
            warCommand(container, argList);
        } else {
            tryDiscoverCommand(container, command, argList);
        }
    }

    protected void tryDiscoverCommand(PlexusContainer container, String commandText, LinkedList<String> argList) throws Exception {
        checkCommandsLoaded();

        defaultVersion = DEFAULT_VERSION;
        Command command = commands.get(commandText);
        if (command == null) {
            // if we have used a colon then extract the version argument
            int idx = commandText.lastIndexOf(':');
            if (idx > 1) {
                defaultVersion = commandText.substring(idx + 1);
                commandText = commandText.substring(0, idx);
                command = commands.get(commandText);
            }
        }
        if (command == null) {
            throw new UsageException("Unknown command '" + commandText + "'");
        }
        // lets run the command!
        LinkedList<String> artifacts = new LinkedList<String>();
        LinkedList<String> args = new LinkedList<String>();
        splitArgumentList(argList, artifacts, args);


        boolean addedArgs = false;
        LinkedList<String> newArguments = new LinkedList<String>();
        for (String arg : command.getAliasArguments()) {
            arg = replaceVariables(arg);
            if (arg.equals(Command.ARTIFACTS_VARIABLE)) {
                newArguments.addAll(artifacts);
            } else if (arg.equals(Command.ARGS_VARIABLE)) {
                newArguments.addAll(args);
                addedArgs = true;
            } else {
                newArguments.add(arg);
            }
        }
        if (!addedArgs) {
            newArguments.addAll(args);
        }

        LOG.info("About to execute: " + newArguments);
        processCommandLine(container, newArguments);

    }

    private void execCommand(PlexusContainer container, LinkedList<String> argList) throws Exception {
        assertNotEmpty(argList);
        artifactIds = parseArtifactList(argList);
        assertNotEmpty(argList);
        className = argList.removeFirst();
        reminingArgs = argList;

        List<File> dependencies = resolveFiles(container);

        execClass(dependencies);
    }

    private void execJarCommand(PlexusContainer container, LinkedList<String> argList) throws Exception {
        assertNotEmpty(argList);
        artifactIds = parseArtifactList(argList);
        assertNotEmpty(argList);
        reminingArgs = argList;

        List<File> dependencies = resolveFiles(container);
        setClassNameFromExecutableJar(dependencies);

        execClass(dependencies);
    }

    protected void execClass(List<File> dependencies) {
        ArrayList<String> commandLine = new ArrayList<String>();
        commandLine.add("java");
        commandLine.add("-cp");
        commandLine.add("\"" + classpath(dependencies) + "\"");
        commandLine.add(className);
        commandLine.addAll(reminingArgs);

        Logger.debug("execing: " + commandLine);
    }

    private void jarCommand(PlexusContainer container, LinkedList<String> argList) throws Exception {
        assertNotEmpty(argList);
        artifactIds = parseArtifactList(argList);
        reminingArgs = argList;

        List<File> dependencies = resolveFiles(container);
        setClassNameFromExecutableJar(dependencies);

        runClass(dependencies);
    }

    protected void warCommand(PlexusContainer container, LinkedList<String> argList) throws Exception {
        assertNotEmpty(argList);
        defaultType = "war";
        artifactIds = parseArtifactList(argList);
        reminingArgs = argList;

        // lets default the artiact to WAR and then find all the files and pass them in as a command line argumnet
        List<File> files = resolveFiles(container, new Predicate<Artifact>() {
            public boolean apply(@Nullable Artifact artifact) {
                String type = artifact.getType();
                System.out.println("artifact: " + artifact + " has type: " + type);
                return type != null && type.equals("war");
            }
        });

        LOG.debug("Running war with files: " + files);


        LinkedList<String> newArgs = new LinkedList<String>();
        newArgs.add("jar");
        newArgs.add("org.mortbay.jetty:jetty-runner:LATEST");
        newArgs.addAll(argList);
        for (File file : files) {
            newArgs.add(file.toString());
        }

        LOG.debug("About to run: " + newArgs);
        processCommandLine(container, newArgs);
    }


    protected void setClassNameFromExecutableJar(List<File> dependencies) throws Exception, UsageException {
        // now lets figure out the className from the manifest
        // lets assume that the first file in the dependency list is usually the one we want to execute
        for (File file : dependencies) {
            JarFile jar = new JarFile(file);
            Manifest manifest = jar.getManifest();
            if (manifest != null) {
                Attributes attributes = manifest.getMainAttributes();
                if (attributes != null) {
                    //debug("file " + file + " has main attributes: " + new HashMap(attributes));
                    className = attributes.getValue(Attributes.Name.MAIN_CLASS);
                    if (className != null && className.length() > 0) {
                        className = className.trim();
                        if (className.length() > 0) {
                            break;
                        }
                    }
                } else {
                    Logger.debug("file " + file + " has no manifest main attributes: " + attributes);
                }
            } else {
                Logger.debug("file " + file + " has no manifest");
            }
        }
        if (className == null) {
            throw new Exception("No Main-Class attribute could be found in the dependent jars!");
        }
    }

    private void runCommand(PlexusContainer container, LinkedList<String> argList) throws Exception, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        assertNotEmpty(argList);
        artifactIds = parseArtifactList(argList);
        assertNotEmpty(argList);
        className = argList.removeFirst();
        reminingArgs = argList;

        List<File> dependencies = resolveFiles(container);
        runClass(dependencies);
    }

    protected void runClass(List<File> dependencies) throws Exception {
        URLClassLoader classLoader = createClassLoader(dependencies);
        Thread.currentThread().setContextClassLoader(classLoader);

        Logger.debug("Attempting to load class: " + className);
        Class<?> aClass = classLoader.loadClass(className);
        Method method = aClass.getMethod("main", String[].class);
        String[] commandLineArgs = reminingArgs.toArray(new String[reminingArgs.size()]);
        Object[] methodArgs = {commandLineArgs};
        method.invoke(null, methodArgs);
    }

    /**
     * Returns a new class loader from the given dependencies
     */
    protected URLClassLoader createClassLoader(List<File> dependencies) throws MalformedURLException {
        List<URL> urls = new ArrayList<URL>();
        for (File file : dependencies) {
            urls.add(file.toURL());
        }

        URL[] urlArray = urls.toArray(new URL[urls.size()]);
        ClassLoader rootClassLoader = Object.class.getClassLoader();
        URLClassLoader classLoader = new URLClassLoader(urlArray, rootClassLoader);
        return classLoader;
    }

    private void copyCommand(PlexusContainer container, LinkedList<String> argList) throws Exception {
        assertNotEmpty(argList);
        artifactIds = parseArtifactList(argList);
        assertNotEmpty(argList);
        File targetDir = new File(argList.removeFirst());
        List<File> dependencies = resolveFiles(container);
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

    private void classpathCommand(PlexusContainer container, LinkedList<String> argList) throws Exception {
        artifactIds = parseArtifactList(argList);
        List<File> dependencies = resolveFiles(container);
        String classpath = classpath(dependencies);
        System.out.println(classpath);
    }

    private void echoCommand(PlexusContainer container, LinkedList<String> argList) throws Exception {
        artifactIds = parseArtifactList(argList);
        List<File> dependencies = resolveFiles(container);
        String classpath = classpath(dependencies);
        System.out.print("java -cp \"" + classpath + "\"");
        for (String arg : argList) {
            System.out.print(" \"" + arg + "\"");
        }
        System.out.println();
    }

    protected String classpath(List<File> files) {
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
        String classPath = buffer.toString();
        return classPath;
    }


    private ArrayList<ArtifactId> parseArtifactList(LinkedList<String> values) throws UsageException {
        ArrayList<ArtifactId> rc = new ArrayList<ArtifactId>();
        assertNotEmpty(values);
        String value = values.removeFirst();
        ArtifactId id = new ArtifactId();
        if (!id.parse(value, defaultVersion, defaultType)) {
            throw new UsageException("");
        }
        rc.add(id);

        while (!values.isEmpty() && isAnotherArtifactId(values.getFirst())) {
            value = values.removeFirst().substring(1);
            id = new ArtifactId();
            if (!id.parse(value, defaultVersion, defaultType)) {
                throw new UsageException("");
            }
            rc.add(id);
        }

        return rc;

    }

    static private class UsageException extends Exception {
        public UsageException(String message) {
            super(message);
        }
    }

    private void assertNotEmpty(LinkedList<String> args) throws UsageException {
        if (args.isEmpty()) {
            throw new UsageException("Empty argument list");
        }
    }

    // Implementation methods
    //-------------------------------------------------------------------------
    protected List<File> resolveFiles(PlexusContainer container) throws Exception {
        return resolveFiles(container, Predicates.<Artifact>alwaysTrue());
    }

    protected List<File> resolveFiles(PlexusContainer container, Predicate<Artifact> filter) throws Exception {
        LinkedHashSet<Artifact> artifacts = resolveArtifacts(container);

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

    private LinkedHashSet<Artifact> resolveArtifacts(PlexusContainer container) throws Exception {
        LinkedHashSet<Artifact> artifacts = new LinkedHashSet<Artifact>();
        for (ArtifactId id : artifactIds) {
            artifacts.addAll(resolveArtifacts(container, id));
        }
        return artifacts;
    }

    private Set<Artifact> resolveArtifacts(PlexusContainer container, ArtifactId id) throws Exception, InvalidRepositoryException {
        Logger.debug("Loading artifact " + id);

        RepositorySystem repositorySystem = (RepositorySystem) container.lookup(RepositorySystem.class);

        //Artifact artifact = repositorySystem.createArtifact(id.getGroupId(), id.getArtifactId(), id.getVersion(), id.getType());
        Artifact artifact = repositorySystem.createArtifactWithClassifier(id.getGroupId(), id.getArtifactId(), id.getVersion(), id.getType(), id.getClassifier());

        Logger.debug("Found artifact: " + artifact);

        ArtifactRepository localRepository = (localRepo != null)
                ? repositorySystem.createLocalRepository(new File(localRepo))
                : repositorySystem.createDefaultLocalRepository();

        List<ArtifactRepository> remoteRepoList = new ArrayList<ArtifactRepository>();
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

        ArtifactResolutionRequest request = new ArtifactResolutionRequest()
                .setArtifact(artifact)
                .setResolveRoot(true)
                .setResolveTransitively(true)
                .setLocalRepository(localRepository)
                .setRemoteRepostories(remoteRepoList);

        ArtifactResolutionResult result = repositorySystem.resolve(request);

        List<Artifact> list = result.getMissingArtifacts();
        if (!list.isEmpty()) {
            throw new Exception("The following artifacts could not be downloaded: " + list);
        }
        return result.getArtifacts();
    }

    /**
     * Adds some default remote repositories
     */
    protected void addDefaultRemoteRepos(RepositorySystem repositorySystem, List<ArtifactRepository> remoteRepoList) {
        ArtifactRepositoryPolicy repositoryPolicy = new ArtifactRepositoryPolicy();
        DefaultRepositoryLayout layout = new DefaultRepositoryLayout();

        remoteRepoList.add(repositorySystem.createArtifactRepository("fusesource.m2", "http://repo.fusesource.com/maven2", layout, repositoryPolicy, repositoryPolicy));
        remoteRepoList.add(repositorySystem.createArtifactRepository("fusesource.m2-snapshot", "http://repo.fusesource.com/maven2-snapshot", layout, repositoryPolicy, repositoryPolicy));
    }


    /**
     * Returns true if the given artifactScope matches the current scope setting (which defaults to 'compile') to choose
     * the exact dependencies to add to the classpath
     */
    protected boolean matchesScope(String scope, String artifactScope) {
        // TODO is there a special Maven way to test this???
        return artifactScope == null || artifactScope.equals(scope) || artifactScope.equals("compile") || artifactScope.equals("provided");
    }


    void checkCommandsLoaded() {
        if (commands == null) {
            commands = Commands.loadCommands(getClass().getClassLoader());

            registerDefaultCommands();
        }
    }

    protected void registerDefaultCommands() {
        registerDefaultCommand("jar", "uses an embedded class loader to run the Main class from the executable jar");
        registerDefaultCommand("execjar", "spawns a separate process to run the Main class from the executable jar in a new JVM");

        registerDefaultCommand("run", "<artifact(s)> <className> [<args(s)>]", "uses an embedded class loader to run the class's main() method");
        registerDefaultCommand("exec", "<artifact(s)> <className> [<args(s)>]", "spawns a separate process to run the class's main() method in a new JVM");

        registerDefaultCommand("echo", "<artifact(s)> [<className>] [<arg(s)>]", "displays the command line to set the classpath and run the class's main() method");
        registerDefaultCommand("classpath", "<artifact(s)>", "displays the classpath for the artifact");

        registerDefaultCommand("copy", "<artifact(s)> targetDirectory", "copies all the jars into the given directory");
        registerDefaultCommand("war", "runs the given (typically war) archetypes in the jetty servlet engine via jetty-runner");
    }


    protected void registerDefaultCommand(String name, String description) {
        registerDefaultCommand(name, "<artifact(s)> [<args(s)>]", description);
    }

    protected void registerDefaultCommand(String name, String options, String description) {
        commands.put(name, new Command(name, options, description));
    }

    private String replaceVariables(String arg) {
        return arg.replaceAll("\\$\\{version\\}", defaultVersion);
    }

    /**
     * Lets split the argument list into the artifact(s) strings then the remaining arguments
     */
    private void splitArgumentList(LinkedList<String> argList, LinkedList<String> artifacts, LinkedList<String> remainingArgs) {
        if (argList.isEmpty()) {
            return;
        }
        artifacts.add(argList.removeFirst());
        while (!argList.isEmpty()) {
            String arg = argList.removeFirst();
            if (isAnotherArtifactId(arg)) {
                artifacts.add(arg);
            } else {
                remainingArgs.add(arg);
                remainingArgs.addAll(argList);
                break;
            }
        }

    }

    /**
     * Returns true if this is an additional artifact string; typically if it begins with +
     */
    protected boolean isAnotherArtifactId(String arg) {
        return arg.startsWith("+");
    }

    // Properties
    //-------------------------------------------------------------------------
    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getLocalRepo() {
        return localRepo;
    }

    public void setLocalRepo(String localRepo) {
        this.localRepo = localRepo;
    }

    public Options getOptions() {
        return options;
    }

    public void setOptions(Options options) {
        this.options = options;
    }

    public List<String> getReminingArgs() {
        return reminingArgs;
    }

    public void setReminingArgs(List<String> reminingArgs) {
        this.reminingArgs = reminingArgs;
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

    public ArrayList<ArtifactId> getArtifactIds() {
        return artifactIds;
    }

    public void setArtifactIds(ArrayList<ArtifactId> artifactIds) {
        this.artifactIds = artifactIds;
    }
}
