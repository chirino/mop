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
package org.fusesource.mop.spring;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;


/**
 * Starts up a Spring based application
 *
 * @version $Revision: 1.1 $
 */
public class Main {
    private static final transient Log LOG = LogFactory.getLog(Main.class);

    private AbstractXmlApplicationContext applicationContext;

    public static void main(String[] args) {
        try {
            Main main = new Main();
            main.run(args);
        } catch (Exception e) {
            LOG.error("Failed: " + e, e);
        }
    }

    private void run(String[] args) throws Exception {
        applicationContext = createApplicationContext(args);
        applicationContext.start();

        System.out.println("Enter quit to stop");

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                String line = reader.readLine();
                if (line == null || line.trim().equalsIgnoreCase("quit")) {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Caught: " + e);
            e.printStackTrace(System.err);
        }

        applicationContext.close();
    }

    protected ClassPathXmlApplicationContext createApplicationContext(String[] args) {
        if (args.length == 0) {
            return new ClassPathXmlApplicationContext("META-INF/spring/*.xml");
        } else {
            return new ClassPathXmlApplicationContext(args);
        }
    }
}
