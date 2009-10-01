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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.maven.artifact.Artifact;
import org.codehaus.plexus.PlexusContainer;
import org.fusesource.mop.commands.CloudMixAgent;
import org.fusesource.mop.commands.CloudMixController;
import org.fusesource.mop.commands.Fork;
import org.fusesource.mop.commands.Install;
import org.fusesource.mop.commands.Karaf;
import org.fusesource.mop.commands.ServiceMix;
import org.fusesource.mop.commands.Shell;
import org.fusesource.mop.support.ArtifactId;
import org.fusesource.mop.support.CommandDefinition;
import org.fusesource.mop.support.CommandDefinitions;
import org.fusesource.mop.support.Logger;
import org.fusesource.mop.support.MethodCommandDefinition;

import static org.fusesource.mop.support.OptionBuilder.ob;

/**
 * Runs a Java class from an artifact loaded from the local maven repository
 * using optional remote repositories.
 */
public class MOP {

    private static final transient Log LOG = LogFactory.getLog(org.fusesource.mop.MOP.class);

    public static final String DEFAULT_VERSION = "RELEASE";
    public static final String DEFAULT_TYPE = "jar";
    public static final String MOP_WORKING_DIR_SYSPROPERTY = "org.fusesource.mop.workingdir";

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
    private Map<String,String> systemProperties = new HashMap<String, String>();

    static public Options createOptions() {
        Options options = new Options();
        options.addOption("h", "help",    false, "Display help information");
        options.addOption("o", "offline", false, "Work offline");
        options.addOption("X", "debug",   false, "Produce execution debug output");

        options.addOption(ob()
                .id("n")
                .name("no-repos")
                .description("Do not use any default repos").op());

        options.addOption(ob()
                .id("l")
                .name("local")
                .arg("directory")
                .description("Specifies the local mop repo").op());

        options.addOption(ob()
                .id("r")
                .name("repo")
                .arg("repo")
                .description("Add a remote maven repo").op());

        options.addOption(ob()
                .id("s")
                .name("scope")
                .arg("scope")
                .description("Maven scope of transitive dependencies to include, defaults to 'runtime'").op());

        return options;
    }

    public void displayHelp() {
        System.err.flush();
        String app = System.getProperty("mop.application", "mop");

        // The commented out line is 80 chars long.  We have it here as a visual reference
//      p("                                                                                ");
        p();
        p("Usage: "+ app +" [options] <command>");
        p();
        p("Description:");
        p();
        pw("  mop is a tool for running Java code on the command line using maven repositories to download code and create classpaths.", 2);
        p();

        p("Options:");
        p();
        PrintWriter out = new PrintWriter(System.out);
        HelpFormatter formatter = new HelpFormatter();
        formatter.printOptions(out, 78, createOptions(), 2, 2);
        out.flush();
        p();

        checkCommandsLoaded();
        p("Commands:");
        p();
        for (Map.Entry<String, CommandDefinition> entry : commands.entrySet()) {
            CommandDefinition command = entry.getValue();
            pw("  * "+entry.getKey()+": "+removeNewLines(command.getDescription()), 4);
            pw("      usage: "+app+" [options] "+entry.getKey()+" "+removeNewLines(command.getUsage()), 6);
            p();
        }

        p("Where:");
        p();
        p("  <repo>     is of the format: repo_id=repo_url");
        p("  <artifact> is of the format: ");
        p("             [groupId:]artifactId[[:type[:classifier]]:version] [+<artifact>]");
        p();
        p("Learn more at: http://mop.fusesource.org/");
    }

    private void pw(String message, int indent) {
        PrintWriter out = new PrintWriter(System.out);
        HelpFormatter formatter = new HelpFormatter();
        formatter.printWrapped(out, 78, indent, message);
        out.flush();
    }

    public static void main(String[] args) {

        String jv = System.getProperty("java.version").substring(0, 3);
        if (jv.compareTo("1.5") < 0) {
            System.err.println("The Launch Agent requires jdk 1.5 or higher to run, the current java version is " + System.getProperty("java.version"));
            System.exit(-1);
            return;
        }

        MOP mop = new MOP();
        String workDir = System.getProperty(MOP_WORKING_DIR_SYSPROPERTY);
        if (workDir != null) {
            mop.setWorkingDirectory(new File(workDir));
        } else {
            String userDir = System.getProperty("user.dir");
            if (userDir != null) {
                mop.setWorkingDirectory(new File(userDir));
            }
        }
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
        int answer1 = answer;
        if (processRunner != null) {
            answer1 = processRunner.join();
        }
        answer = answer1;
        return answer;
    }

    public int execute(String[] args) {
        CommandLine cli = null;
        try {
            cli = new GnuParser().parse(createOptions(), args, true);
        } catch (ParseException e) {
            System.err.println( "Unable to parse command line options: " + e.getMessage() );
            displayHelp();
            return 1;
        }

        if( cli.hasOption("h") ) {
            displayHelp();
            return 0;
        }

        // lets process the options
        Logger.debug = cli.hasOption("X");

        if( cli.hasOption("n") ) {
            getRemoteRepositories().clear();
        }

        String scope = cli.getOptionValue("s", "runtime");
        repository.setScope(scope);
        String[] repos = cli.getOptionValues("r");
        if( repos!=null ) {
            for (String repo : repos) {
                String[] rc = repo.split("=", 2);
                if( rc.length != 2 ) {
                    System.err.println("Invalid repository.  Expected format is: <id>=<url>, actual: "+repo);
                    displayHelp();
                    return 1;
                }
                getRemoteRepositories().put(rc[0], rc[1]);
            }
        }

        repository.setOnline(!cli.hasOption("o"));
        Logger.debug("online mode: " + repository.isOnline());

        String localRepo = cli.getOptionValue('l');
        if (localRepo != null) {
            repository.setLocalRepo(new File(localRepo));
        }

        // now the remaining command line args
        try {
            LinkedList<String> argList = new LinkedList<String>(Arrays.asList(cli.getArgs()));
            executeCommand(argList);
        } catch (UsageException e) {
            displayHelp();
            return 1;
        } catch (Throwable e) {
            System.err.println();
            System.err.println("Failed: " + e);
            e.printStackTrace();
            Set<Throwable> exceptions = new HashSet<Throwable>();
            exceptions.add(e);
            for (int i = 0; i < 10; i++) {
                e = e.getCause();
                if (e != null && exceptions.add(e)) {
                    System.err.println("Reason: " + e);
                    e.printStackTrace();
                } else {
                    break;
                }
            }
            return 2;
        }
        return 0;
    }

    public void executeCommand(LinkedList<String> argList) throws Exception {
        resetValues();
        if (argList.isEmpty()) {
            throw new UsageException("No command specified.");
        }

        String command = argList.removeFirst();
        if (command.equals("exec")) {
            execJava(argList);
        } else if (command.equals("execjar")) {
            execJarCommand(argList);
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
            tryDiscoverCommand(command, argList);
        }
    }

    //-------------------------------------------------------------------------
    // sub command implementations
    //-------------------------------------------------------------------------
    private void execJava(LinkedList<String> argList) throws Exception {
        assertNotEmpty(argList);
        artifactIds = parseArtifactList(argList);
        assertNotEmpty(argList);
        className = argList.removeFirst();
        reminingArgs = argList;

        List<File> dependencies = resolveFiles();

        execClass(dependencies);
    }

    public void execJarCommand(LinkedList<String> argList) throws Exception {
        assertNotEmpty(argList);
        artifactIds = parseArtifactList(argList);
        reminingArgs = argList;

        List<File> dependencies = resolveFiles();
        setClassNameFromExecutableJar(dependencies);

        execClass(dependencies);
    }

    private void jarCommand(LinkedList<String> argList) throws Exception {
        assertNotEmpty(argList);
        artifactIds = parseArtifactList(argList);
        reminingArgs = argList;

        List<File> dependencies = resolveFiles();
        setClassNameFromExecutableJar(dependencies);

        runClass(dependencies);
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

    private void echoCommand(LinkedList<String> argList) throws Exception {
        artifactIds = parseArtifactList(argList);
        String classpath = classpath();
        System.out.print("java -cp \"" + classpath + "\"");
        for (String arg : argList) {
            System.out.print(" \"" + arg + "\"");
        }
        p();
    }


    private void classpathCommand(LinkedList<String> argList) throws Exception {
        artifactIds = parseArtifactList(argList);
        String classpath = classpath();
        p(classpath);
    }

    private void copyCommand(LinkedList<String> argList) throws Exception {
        assertNotEmpty(argList);
        artifactIds = parseArtifactList(argList);
        assertNotEmpty(argList);
        File targetDir = new File(argList.removeFirst());
        repository.copy(targetDir, artifactIds);
    }


    protected void warCommand(LinkedList<String> argList) throws Exception {
        assertNotEmpty(argList);
        defaultType = "war";
        artifactIds = parseArtifactList(argList);
        reminingArgs = argList;

        // lets default the artiact to WAR and then find all the files and pass them in as a command line argumnet
        repository.setTransitive(false); // We just need the wars.. not the transitive deps.
        List<File> files = repository.resolveFiles(artifactIds);
        // We will need transitive deps to load up jettty
        repository.setTransitive(true);

        LOG.debug("Running war with files: " + files);


        LinkedList<String> newArgs = new LinkedList<String>();
        newArgs.add("jar");
        newArgs.add("org.mortbay.jetty:jetty-runner:RELEASE");
        newArgs.addAll(argList);
        for (File file : files) {
            newArgs.add(file.toString());
        }

        LOG.debug("About to run: " + newArgs);
        executeCommand(newArgs);
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
                p("No such command '" + command + "'");
            } else {
                p();
                p("mop command: " + command.getName());
                p();
                p("usage:");
                p("\t mop [options] " + command.getName() + " " + command.getUsage());
                p();
                p(command.getDescription());
                p();
            }
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



    private void uninstallCommand(LinkedList<String> argList) throws Exception {
        artifactIds = parseArtifactList(argList);
        List<String> errorMessages = repository.uninstall(artifactIds);
        for (String errorMessage : errorMessages) {
            p(errorMessage);
        }
    }

    protected void tryDiscoverCommand(String commandText, LinkedList<String> argList) throws Exception {
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
        command.executeCommand(this, argList);
    }

    //-------------------------------------------------------------------------
    // Other plublic methods... perhaps this needs a little clean up...
    //-------------------------------------------------------------------------

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

    /**
     * Appends the currently defined system properties to this command line as a series of <code>-Dname=value</code> parameters
     */
    public void addSystemProperties(List<String> commandLine) {
        Set<Map.Entry<String, String>> entries = systemProperties.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            commandLine.add("-D" + entry.getKey() + "=" + entry.getValue());
        }
    }

    public void exec(List<String> commandLine) throws Exception {
        System.out.println("*** execing: " + commandLine);
        processRunner = doExec(commandLine, true);
    }
    
    public void execAndWait(List<String> commandLine) throws Exception {
        ProcessRunner pRunner = doExec(commandLine, false);
        if (pRunner != null) {
            pRunner.join();
        }
    }
    
    private ProcessRunner doExec(List<String> commandLine, boolean redirectInput) throws Exception {
        Logger.debug("execing: " + commandLine);

        String[] cmd = commandLine.toArray(new String[commandLine.size()]);

        String[] env = null;
        if (isWindows()) {
            Map<String, String> envMap = System.getenv();
            env = new String[envMap.size()];
            int ind = 0;
            for (Map.Entry<String, String> entry : envMap.entrySet()) {
                env[ind++] = entry.getKey() + "=" + entry.getValue();
            }
        }

        return ProcessRunner.newInstance(ProcessRunner.newId("process"), cmd, env,
                                         workingDirectory, redirectInput);
    }


    public String classpath() throws Exception {
        return repository.classpath(artifactIds);
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


    /**
     * Returns true if this is an additional artifact string; typically if it begins with +
     */
    public boolean isAnotherArtifactId(String arg) {
        return arg.startsWith("+");
    }

    //-------------------------------------------------------------------------
    // Implementation methods
    //-------------------------------------------------------------------------

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





    protected void resetValues() {
        // lets reset values in case we chain things together...
        processRunner = null;
        defaultVersion = DEFAULT_VERSION;
        defaultType = DEFAULT_TYPE;
        repository.setTransitive(true);

        // lets not clear the system properties as they tend to be expected to flow through to the next invocation...
        //systemProperties.clear();
    }


    protected void execClass(List<File> dependencies) throws Exception {
        List<String> commandLine = new ArrayList<String>();
        commandLine.add("java");
        addSystemProperties(commandLine);
        commandLine.add("-cp");
        commandLine.add(MOPRepository.classpathFiles(dependencies));
        commandLine.add(className);
        commandLine.addAll(reminingArgs);

        exec(commandLine);
    }

    private boolean isWindows() {
    	String os = System.getProperty("os.name");
        return os != null && os.toLowerCase().contains("windows") ? true : false;
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
        registerCommandMethods(new CloudMixController());
        registerCommandMethods(new Fork());
        registerCommandMethods(new Install());
        registerCommandMethods(new ServiceMix());
        registerCommandMethods(new Karaf());
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

    private void p() {
        System.out.println();
    }

    private void p(String s) {
        System.out.println(s);
    }

    //-------------------------------------------------------------------------
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

    public void setScope(String scope) {
        repository.setScope(scope);
    }

    public PlexusContainer getContainer() {
        return repository.getContainer();
    }

    public File getLocalRepo() {
        return repository.getLocalRepo();
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

    public LinkedHashMap<String, String> getRemoteRepositories() {
        return repository.getRemoteRepositories();
    }

    public void setRemoteRepositories(LinkedHashMap<String, String> remoteRepositories) {
        repository.setRemoteRepositories(remoteRepositories);
    }
}
