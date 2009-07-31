package org.fusesource.mop;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;

/**
 * @author chirino
 */
public class DatabaseTest extends TestCase {

    public void testDatabase() throws IOException {

        Database database = new Database();
        File directroy = new File("./target/test-data");
        database.setDirectroy(directroy);
        database.delete();

        database.open(false);
        Set<String> artifacts = new HashSet<String>();
        artifacts.add("org.fusesource.mop:test:jar:1.0");
        artifacts.add("org.fusesource.mop:foo:jar:1.0");
        database.install(artifacts);
        database.close();

        database = new Database();
        database.setDirectroy(directroy);
        database.open(false);
        artifacts = new HashSet<String>();
        artifacts.add("org.test:test:jar:1.0");
        artifacts.add("org.test:other:jar:1.0");
        database.install(artifacts);
        database.close();


        database = new Database();
        database.setDirectroy(directroy);
        database.open(false);
        Set<String> rc = new TreeSet(database.findByArtifactId("test"));

        artifacts = new TreeSet<String>();
        artifacts.add("org.fusesource.mop:test:jar:1.0");
        artifacts.add("org.test:test:jar:1.0");
        
        assertEquals(artifacts,rc);
        database.close();

    }

}