package org.fusesource.mop;

import junit.framework.TestCase;

import java.io.File;
import java.util.Set;
import java.util.List;
import java.util.Enumeration;
import java.net.URL;

import org.fusesource.mop.support.ArtifactId;
import org.fusesource.mop.support.Logger;
import org.apache.maven.artifact.Artifact;

/**
 * @author chirino
 */
public class MOPRepositoryTest extends TestCase {

    public void testInstall() throws Exception {
        Logger.debug = true;
        MOPRepository repository = new MOPRepository();


        String list = repository.classpath(ArtifactId.parse("org.fusesource.meshkeeper:meshkeeper-api:1.0-SNAPSHOT"));
        System.out.println(list);
    }

    public static void main(String args[]) throws Exception {
        Logger.debug = true;
        MOPRepository repository = new MOPRepository();
        String list = repository.classpath(ArtifactId.parse(args[0]));
        System.out.println(list);

    }

}