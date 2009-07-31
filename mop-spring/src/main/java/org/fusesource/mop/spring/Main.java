/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.mop.spring;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

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
