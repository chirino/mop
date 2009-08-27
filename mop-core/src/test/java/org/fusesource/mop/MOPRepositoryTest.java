package org.fusesource.mop;

import junit.framework.TestCase;

import java.io.File;
import java.util.Set;
import java.util.List;

import org.fusesource.mop.support.ArtifactId;
import org.apache.maven.artifact.Artifact;

/**
 * @author chirino
 */
public class MOPRepositoryTest extends TestCase {

    public void testInstall() throws Exception {
        MOPRepository repository = new MOPRepository();
        repository.setLocalRepo(new File("target/test-repo"));
        List<File> list = repository.resolveFiles(ArtifactId.parse("commons-logging:commons-logging:1.1"));
        System.out.println(list);
    }

}