![Mop][1]
==========

Description
-----------

[Mop][2] is a small utility for executing Java programs which are stored as artifacts like jars or bundles in a Maven repository.

Mop automatically deals with the following for you
* transitive dependencies
* downloading artifacts from remote repositories and caching them locally
* building the application classpath

Synopsis
--------

Currently supported generic commands:

* `mop classpath`            : displays the classpath for the artifact
* `mop copy`                 : copies all the jars into the given directory
* `mop echo`                 : displays the command line to set the classpath and run the class's main() method
* `mop exec`                 : spawns a separate process to run the class's main() method in a new JVM
* `mop execjar`              : spawns a separate process to run the Main class from the executable jar in a new JVM
* `mop fork`                 : Forks a new child JVM and executes the remaining arguments as a child Mop process
* `mop install`              : Installs the given artifacts in the given target directory
* `mop jar`                  : uses an embedded class loader to run the Main class from the executable jar
* `mop run`                  : uses an embedded class loader to run the class's main() method
* `mop shell`                : Forks a new child process by running an external command

Command extensions:

* `mop broker`               : Starts an Apache ActiveMQ broker along with the web console. You can use broker:version to specify the ActiveMQ version
* `mop camel-example-pojo`   : Runs the Apache Camel POJO Messaging Example. For more see: http://camel.apache.org/pojo-messaging-example.html
* `mop cloudmixAgent`        : Starts a CloudMix agent
* `mop guice`                : Runs the given artifact(s) by starting a Guice injector and injecting all of the given modules listed on the command line
* `mop karaf`                : Starts an embedded Karaf container and deploys the given artifact(s) inside it
* `mop servicemix`           : 
* `mop spring`               : Runs the given artifact(s) by starting the XML application context specified as an argument or defaults to META-INF/spring/\*.xml
* `mop war`                  : runs the given (typically war) archetypes in the jetty servlet engine via jetty-runner

Examples
--------

Need to build a script file to start your java application and you having a hard time setting up a class path that contains all the dependencies?  Mop can help.  Just capture the result of the `mop classpath` command in the `CLASSPATH` variable:

    $ CLASSPATH=`mop classpath commons-logging:commons-logging:1.1`
    $ echo $CLASSPATH
    /opt/mop/repository/commons-logging/commons-logging/1.1/commons-logging-1.1.jar
    :/opt/mop/repository/log4j/log4j/1.2.12/log4j-1.2.12.jar:/opt/mop/repository/lo
    gkit/logkit/1.0.1/logkit-1.0.1.jar:/opt/mop/repository/avalon-framework/avalon-
    framework/4.1.3/avalon-framework-4.1.3.jar:/opt/mop/repository/javax/servlet/se
    rvlet-api/2.3/servlet-api-2.3.jar

The following example will boot up one of the [Apache Camel][3] example programs

    mop run org.apache.camel:camel-example-pojo-messaging org.apache.camel.spring.Main

The `mop run` command takes a maven artifact name then a Java class name along with optional command line arguments. The maven artifact uses the following format

* `[groupId:]artifactId[[:type[:classifier]]:version]`

Note that that you can omit the group id, type, classifier and version. You typically may supply
just groupId, artifactId, version if you like - or if you really need to specify type (jar/war etc) and
classifier if needed.

Project Links
-------------

* [Project Home][2]
* [Release Downloads](http://mop.fusesource.org/downloads/index.html)
* [GitHub](http://github.com/chirino/mop/tree/master)
* Source: `git clone git://forge.fusesource.com/mop.git`

[1]: http://mop.fusesource.org/images/mop-logo-small.png "Mop"
[2]: http://mop.fusesource.org "Project Home Page"
[3]: http://camel.apache.org "Camel Home Page"
