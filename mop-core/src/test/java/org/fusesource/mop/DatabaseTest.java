/**
 *  Copyright (C) 2009 Progress Software, Inc. All rights reserved.
 *  http://fusesource.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.fusesource.mop;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.TestCase;

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
        database.open(true);
        Set<String> rc = new TreeSet<String>(database.findByArtifactId("test"));

        artifacts = new LinkedHashSet<String>();
        artifacts.add("org.fusesource.mop:test:jar:1.0");
        artifacts.add("org.test:test:jar:1.0");
        
        assertEquals(artifacts,rc);
        database.close();

    }

}