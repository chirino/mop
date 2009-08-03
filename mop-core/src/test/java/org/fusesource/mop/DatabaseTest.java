/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.mop;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.LinkedHashSet;

import org.fusesource.mop.support.Database;

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
        LinkedHashSet<String> artifacts = new LinkedHashSet<String>();
        artifacts.add("org.fusesource.mop:test:jar:1.0");
        artifacts.add("org.fusesource.mop:foo:jar:1.0");
        database.install(artifacts);
        database.close();

        database = new Database();
        database.setDirectroy(directroy);
        database.open(false);
        artifacts = new LinkedHashSet<String>();
        artifacts.add("org.test:test:jar:1.0");
        artifacts.add("org.test:other:jar:1.0");
        database.install(artifacts);
        database.close();


        database = new Database();
        database.setDirectroy(directroy);
        database.open(false);
        Set<String> rc = new TreeSet(database.findByArtifactId("test"));

        artifacts = new LinkedHashSet<String>();
        artifacts.add("org.fusesource.mop:test:jar:1.0");
        artifacts.add("org.test:test:jar:1.0");
        
        assertEquals(artifacts,rc);
        database.close();

    }

}