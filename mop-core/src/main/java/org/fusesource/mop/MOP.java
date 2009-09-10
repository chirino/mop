/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.mop;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.tools.cli.AbstractCli;
import org.fusesource.mop.commands.CloudMixAgent;
import org.fusesource.mop.commands.Fork;
import org.fusesource.mop.commands.Install;
import org.fusesource.mop.commands.ServiceMix;
import org.fusesource.mop.commands.Shell;
import org.fusesource.mop.support.ArtifactId;
import org.fusesource.mop.support.CommandDefinition;
import org.fusesource.mop.support.CommandDefinitions;
import org.fusesource.mop.support.Logger;
import org.fusesource.mop.support.MethodCommandDefinition;

import com.google.common.base.Nullable;
import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Runs a Java class from an artifact loaded from the local maven repository
 * using optional remote repositories.
 */
public class MOP extends AbstractCli {

    private static final transient Log LOG = LogFactory.getLog(org.fusesource.mop.MOP.class);

    public static final String DEFAULT_VERSION = "RELEASE";
    public static final String DEFAULT_TYPE = "jar";

    private  MOPRepository repository = new MOPRepository();
    private Options options;
    private String className;

    private List<ArtifactId> artifactIds;
    private List<String> reminingArgs;
    private Map<String, CommandDefinition> commands;
    private String defaultVersion = DEFAULT_VERSION;
    private String defaultType = DEFAULT_TYPE;
    private File workingDirectory;
    private ProcessRunner processRunner;
    private Map<String,String> systemProperties = Maps.newHashMap();

    public static void main(String[] args) {
        MOP mop = new MOP();
        int exitValue = mop.executeAndWait(args);
        System.exit(exitValue);
    }

    /**
     * Executes the given MOP command and waits for the response, blocking the calling thread
     * until any child processes complete.
     *
     * @return the exit code
     */
    public int executeAndWait(String[] args) {
        int answer = execute(args);
        if (processRunner != null) {
            answer = processRunner.join();
        }
        return answer;
    }


    public Options buildCliOptions(Options options) {
        this.options = options;
        options.addOption("l", "local", true, "Specifies the local mop repo");
        options.addOption("r", "repo", true, "Specifies a remote maven repo");
        options.addOption("o", "online", false, "Toggle online mode");
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

        System.out.println("Commands:");

        for (Map.Entry<String, CommandDefinition> entry : commands.entrySet()) {
            String description = removeNewLines(entry.getValue().getDescription());
            // lets remove any newlines
            System.out.printf("\t%-20s : %s\n", entry.getKey(), description);
        }

        System.out.println();

        System.out.println();
        System.out.println("Usage:");
        for (CommandDefinition command : commands.values()) {
            System.out.println(String.format("  mop [options] %-20s %s", command.getName(), removeNewLines(command.getUsage())));
        }
        System.out.println();
        System.out.println("  where <artifact> is of the format: [groupId:]artifactId[[:type[:classifier]]:version] [+<artifact>]");
        System.out.println();

        System.out.println("Options:");
        PrintWriter out = new PrintWriter(System.out);
        HelpFormatter formatter = new HelpFormatter();
        formatter.printOptions(out, 78, options, 2, 2);
        out.flush();
        System.out.println();
    }

    public Artifacts getArtifacts(LinkedList<String> argList) throws UsageException {
        artifactIds = parseArtifactList(argList);
        reminingArgs = argList;

        return new Artifacts() {
            @Override
            protected List<File> createFiles() throws Exception {
                return resolveFiles();
            }

            @Override
            protected Set<Artifact> createArtifacts() throws Exception {
                return resolveArtifacts();
            }
        };
    }

    private Set<Artifact> resolveArtifacts() throws Exception {
        return repository.resolveArtifacts(artifactIds);
    }

    /**
     * Removes any newlines in the text so its one big line
     */
    private String removeNewLines(String text) {
        StringBuilder buffer = new StringBuilder();
        BufferedReader reader = new BufferedReader(new StringReader(text.trim()));
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
        repository.setContainer(container);
        // lets process the options
        Logger.debug = cli.hasOption('X');
        repository.setScope(cli.getOptionValue('s', "compile"));
        repository.setRemoteRepos(cli.getOptionValues('r'));
        
        repository.setOnline(!cli.hasOption('o'));
        Logger.debug("online mode: " + repository.isOnline());

        String localRepo = cli.getOptionValue('l');
        if (localRepo != null) {
            repository.setLocalRepo(new File(localRepo));
        }

        // now the remaining command line args
        try {
            LinkedList<String> argList = new LinkedList<String>(cli.getArgList());
            processCommandLine(argList);
        } catch (UsageException e) {
            displayHelp();
            throw e;
        } catch (Throwable e) {
            System.err.println();
            System.err.println("Failed: " + e);
            e.printStackTrace();
            Set<Throwable> exceptions = Sets.newHashSet(e);
            for (int i = 0; i < 10; i++) {
                e = e.getCause();
                if (e != null && exceptions.add(e)) {
                    System.err.println("Reason: " + e);
                    e.printStackTrace();
                } else {
                    break;
                }
            }
        }
    }

    public ProcessRunner processCommandLine(LinkedList<String> argList) throws Exception {
        resetValues();

        if (argList.isEmpty()) {
            displayHelp();
            return null;
        }
        String command = argList.removeFirst();
        if (command.equals("exec")) {
            return execJava(argList);
        } else if (command.equals("execjar")) {
            return execJarCommand(argList);
        } else if (command.equals("jar")) {
            jarCommand(argList);
        } else if (command.equals("run")) {
            runCommand(argList);
        } else if (command.equals("echo")) {
            echoCommand(argList);
        } else if (command.equals("classpath")) {
            classpathCommand(argList);
        } else if (command.equals("copy")) {
            copyCommand(argList);
        } else if (command.equals("war")) {
            warCommand(argList);
        } else if (command.equals("help")) {
            helpCommand(argList);
        } else if (command.equals("list")) {
            listCommand(argList);
        } else if (command.equals("uninstall")) {
            uninstallCommand(argList);
        } else {
            return tryDiscoverCommand(command, argList);
        }
        return null;
    }

    protected void resetValues() {
        // lets reset values in case we chain things together...
        processRunner = null;
        defaultVersion = DEFAULT_VERSION;
        defaultType = DEFAULT_TYPE;
        workingDirectory = new File(System.getProperty("user.dir"));
        repository.setTransitive(true);

        // lets not clear the system properties as they tend to be expected to flow through to the next invocation...
        //systemProperties.clear();
    }

    private void uninstallCommand(LinkedList<String> argList) throws Exception {
        artifactIds = parseArtifactList(argList);
        List<String> errorMessages = repository.uninstall(artifactIds);
        for (String errorMessage : errorMessages) {
            System.out.println(errorMessage);
        }
    }

    private void listCommand(LinkedList<String> argList) throws Exception {
        String type = "installed";
        if (!argList.isEmpty()) {
            type = argList.removeFirst();
        }

        this.artifactIds = parseArtifactList(argList);
        Set<ArtifactId> artifactIds = repository.list(type);
        for (ArtifactId a : artifactIds) {
            System.out.println(a);
        }
    }

    protected ProcessRunner tryDiscoverCommand(String commandText, LinkedList<String> argList) throws Exception {
        checkCommandsLoaded();

        defaultVersion = DEFAULT_VERSION;
        CommandDefinition command = commands.get(commandText);
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
        return command.executeCommand(this, argList);
    }

    protected void helpCommand(LinkedList<String> argList) {
        if (argList.isEmpty()) {
            displayHelp();
            return;
        }
        for (String commandName : argList) {
            checkCommandsLoaded();
            CommandDefinition command = commands.get(commandName);
            if (command == null) {
                System.out.println("No such command '" + command + "'");
            } else {
                System.out.println();
                System.out.println("mop command: " + command.getName());
                System.out.println();
                System.out.println("usage:");
                System.out.println("\t mop [options] " + command.getName() + " " + command.getUsage());
                System.out.println();
                System.out.println(command.getDescription());
                System.out.println();
            }
        }
    }


    private ProcessRunner execJava(LinkedList<String> argList) throws Exception {
        assertNotEmpty(argList);
        artifactIds = parseArtifactList(argList);
        assertNotEmpty(argList);
        className = argList.removeFirst();
        reminingArgs = argList;

        List<File> dependencies = resolveFiles();

        return execClass(dependencies);
    }

    public ProcessRunner execJarCommand(LinkedList<String> argList) throws Exception {
        assertNotEmpty(argList);
        artifactIds = parseArtifactList(argList);
        reminingArgs = argList;

        List<File> dependencies = resolveFiles();
        setClassNameFromExecutableJar(dependencies);

        return execClass(dependencies);
    }

    protected ProcessRunner execClass(List<File> dependencies) throws Exception {
        List<String> commandLine = new ArrayList<String>();
        commandLine.add("java");
        addSystemProperties(commandLine);
        commandLine.add("-cp");
        commandLine.add(MOPRepository.classpathFiles(dependencies));
        commandLine.add(className);
        commandLine.addAll(reminingArgs);

        return exec(commandLine);
    }

    /**
     * Appends the currently defined system properties to this command line as a series of <code>-Dname=value</code> parameters
     */
    public void addSystemProperties(List<String> commandLine) {
        Set<Map.Entry<String, String>> entries = systemProperties.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            commandLine.add("-D" + entry.getKey() +"=" + entry.getValue());
        }
    }

    public ProcessRunner exec(List<String> commandLine) throws Exception {
        Logger.debug("execing: " + commandLine);

        String[] cmd = commandLine.toArray(new String[commandLine.size()]);

        String[] env = {};
        if (isWindows()) {
            String javaHome = System.getProperty("java.home");
            if (javaHome != null) {
            	env = new String[]{"JAVA_HOME=" + javaHome};
            }
        }
        

        processRunner = ProcessRunner.newInstance(ProcessRunner.newId("process"), cmd, env, workingDirectory);
        return processRunner;

    }

    private boolean isWindows() {
    	String os = System.getProperty("os.name");
        return os != null && os.toLowerCase().contains("windows") ? true : false;
    } 
    
    private void jarCommand(LinkedList<String> argList) throws Exception {
        assertNotEmpty(argList);
        artifactIds = parseArtifactList(argList);
        reminingArgs = argList;

        List<File> dependencies = resolveFiles();
        setClassNameFromExecutableJar(dependencies);

        runClass(dependencies);
    }


    protected void warCommand(LinkedList<String> argList) throws Exception {
        assertNotEmpty(argList);
        defaultType = "war";
        artifactIds = parseArtifactList(argList);
        reminingArgs = argList;

        // lets default the artiact to WAR and then find all the files and pass them in as a command line argumnet
        List<File> files = repository.resolveFiles(artifactIds, new Predicate<Artifact>() {
            public boolean apply(@Nullable Artifact artifact) {
                String type = artifact.getType();
                System.out.println("artifact: " + artifact + " has type: " + type);
                return type != null && type.equals("war");
            }
        });

        LOG.debug("Running war with files: " + files);


        LinkedList<String> newArgs = new LinkedList<String>();
        newArgs.add("jar");
        newArgs.add("org.mortbay.jetty:jetty-runner:RELEASE");
        newArgs.addAll(argList);
        for (File file : files) {
            newArgs.add(file.toString());
        }

        LOG.debug("About to run: " + newArgs);
        processCommandLine(newArgs);
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

    private void runCommand(LinkedList<String> argList) throws Exception, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        assertNotEmpty(argList);
        artifactIds = parseArtifactList(argList);
        assertNotEmpty(argList);
        className = argList.removeFirst();
        reminingArgs = argList;

        List<File> dependencies = resolveFiles();
        runClass(dependencies);
    }

    private List<File> resolveFiles() throws Exception {
        return repository.resolveFiles(artifactIds);
    }

    protected void runClass(List<File> dependencies) throws Exception {
        URLClassLoader classLoader = MOPRepository.createFileClassLoader(null, dependencies);
        Thread.currentThread().setContextClassLoader(classLoader);

        Logger.debug("Attempting to load class: " + className);
        Class<?> aClass = classLoader.loadClass(className);
        Method method = aClass.getMethod("main", String[].class);
        String[] commandLineArgs = reminingArgs.toArray(new String[reminingArgs.size()]);
        Object[] methodArgs = {commandLineArgs};
        method.invoke(null, methodArgs);
    }

    private void copyCommand(LinkedList<String> argList) throws Exception {
        assertNotEmpty(argList);
        artifactIds = parseArtifactList(argList);
        assertNotEmpty(argList);
        File targetDir = new File(argList.removeFirst());
        repository.copy(artifactIds, targetDir);
    }

    private void classpathCommand(LinkedList<String> argList) throws Exception {
        artifactIds = parseArtifactList(argList);
        String classpath = classpath();
        System.out.println(classpath);
    }

    public String classpath() throws Exception {
        return repository.classpath(artifactIds);
    }

    private void echoCommand(LinkedList<String> argList) throws Exception {
        artifactIds = parseArtifactList(argList);
        String classpath = classpath();
        System.out.print("java -cp \"" + classpath + "\"");
        for (String arg : argList) {
            System.out.print(" \"" + arg + "\"");
        }
        System.out.println();
    }

    private ArrayList<ArtifactId> parseArtifactList(LinkedList<String> values) throws UsageException {
        ArrayList<ArtifactId> rc = new ArrayList<ArtifactId>();
        assertNotEmpty(values);
        String value = values.removeFirst();
        ArtifactId id = parseArtifactId(value);
        rc.add(id);

        while (!values.isEmpty() && isAnotherArtifactId(values.getFirst())) {
            value = values.removeFirst().substring(1);
            id = parseArtifactId(value);
            rc.add(id);
        }

        return rc;

    }

    public ArtifactId parseArtifactId(String value) throws UsageException {
        ArtifactId id = ArtifactId.parse(value, defaultVersion, defaultType);
        if (id==null) {
            throw new UsageException("Invalid artifactId: " + value);
        }
        return id;
    }

    public Map<String, String> getSystemProperties() {
        return systemProperties;
    }

    /**
     * Adds the system property used for any child forked JVM if the value is not null else remove the property
     */
    public void setSystemProperty(String name, String value) {
        if (value != null) {
            systemProperties.put(name, value);
            System.setProperty(name, value);
        }
        else {
            systemProperties.remove(name);
        }
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
    
    void checkCommandsLoaded() {
        if (commands == null) {
            commands = CommandDefinitions.loadCommands(getClass().getClassLoader());

            registerDefaultCommands();

            CommandDefinitions.addCommandDescriptions(commands, getClass().getClassLoader());
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

        registerDefaultCommand("help", "<command(s)>", "displays help summarising all of the commands or shows custom help for each command listed");

        // TODO it would be better to auto-discover these from the package!!!
        registerCommandMethods(new CloudMixAgent());
        registerCommandMethods(new Fork());
        registerCommandMethods(new Install());
        registerCommandMethods(new ServiceMix());
        registerCommandMethods(new Shell());
    }

    private void registerCommandMethods(Object commandObject) {
        Class<? extends Object> type = commandObject.getClass();
        Method[] methods = type.getMethods();
        for (Method method : methods) {
            Command commandAnnotation = method.getAnnotation(Command.class);
            if (commandAnnotation != null) {
                registerCommand(new MethodCommandDefinition(commandObject, method));
            }
        }
    }


    protected void registerDefaultCommand(String name, String description) {
        registerDefaultCommand(name, "<artifact(s)> [<args(s)>]", description);
    }

    protected void registerDefaultCommand(String name, String usage, String description) {
        CommandDefinition commandDefinition = new CommandDefinition(name, usage, description);
        registerCommand(commandDefinition);
    }

    protected void registerCommand(CommandDefinition commandDefinition) {
        commands.put(commandDefinition.getName(), commandDefinition);
    }

    /**
     * Returns true if this is an additional artifact string; typically if it begins with +
     */
    public boolean isAnotherArtifactId(String arg) {
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

    public Options getOptions() {
        return options;
    }

    public Map<String, CommandDefinition> getCommands() {
        checkCommandsLoaded();
        return commands;
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

    public List<ArtifactId> getArtifactIds() {
        return artifactIds;
    }

    public void setArtifactIds(List<ArtifactId> artifactIds) {
        this.artifactIds = artifactIds;
    }

    public String getDefaultType() {
        return defaultType;
    }

    public void setDefaultType(String defaultType) {
        this.defaultType = defaultType;
    }

    public String getDefaultVersion() {
        return defaultVersion;
    }

    public void setDefaultVersion(String defaultVersion) {
        this.defaultVersion = defaultVersion;
    }

    public ProcessRunner getProcessRunner() {
        return processRunner;
    }

    public File getWorkingDirectory() {
        return workingDirectory;
    }

    public void setWorkingDirectory(File workingDirectory) {
        this.workingDirectory = workingDirectory;
    }

    public void setTransitive(boolean transitive) {
        repository.setTransitive(transitive);
    }

    public void setLocalRepo(File localRepo) {
        repository.setLocalRepo(localRepo);
    }

    public void setOnline(boolean online) {
        repository.setOnline(online);
    }

    public void setRemoteRepos(String[] remoteRepos) {
        repository.setRemoteRepos(remoteRepos);
    }

    public void setScope(String scope) {
        repository.setScope(scope);
    }

    public PlexusContainer getContainer() {
        return repository.getContainer();
    }

    public File getLocalRepo() {
        return repository.getLocalRepo();
    }

    public String[] getRemoteRepos() {
        return repository.getRemoteRepos();
    }

    public String getScope() {
        return repository.getScope();
    }

    public boolean isOnline() {
        return repository.isOnline();
    }

    public boolean isTransitive() {
        return repository.isTransitive();
    }

    public void setContainer(PlexusContainer container) {
        repository.setContainer(container);
    }

    public MOPRepository getRepository() {
        return repository;
    }

    public void setRepository(MOPRepository repository) {
        this.repository = repository;
    }
}
