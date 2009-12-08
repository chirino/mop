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


import junit.framework.TestCase;

import org.fusesource.mop.support.ArtifactId;
import org.fusesource.mop.support.Logger;


/**
 * @author chirino
 */
public class MOPRepositoryTest extends TestCase {

    public void testInstall() throws Exception {
        Logger.debug = true;
        MOPRepository repository = new MOPRepository();


        String list = repository
            .classpath(ArtifactId.parse("xalan:xalan:2.7.1"));
        System.out.println(list);
    }

    public static void main(String args[]) throws Exception {
        Logger.debug = true;
        MOPRepository repository = new MOPRepository();
        String list = repository.classpath(ArtifactId.parse(args[0]));
        System.out.println(list);

    }

}