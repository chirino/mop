/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.mop.commands;

import java.io.File;

import junit.framework.TestCase;

import org.fusesource.mop.MOP;
import org.fusesource.mop.MOPRepository;

/**
 * PurgeTest
 * <p>
 * Description:
 * </p>
 * 
 * @author cmacnaug
 * @version 1.0
 */
public class PurgeTest extends TestCase {

    public void testPurgeCommand() throws Exception {

        System.setProperty(MOPRepository.MOP_BASE, System.getProperty("basedir", ".") + File.separator + "target" + File.separator + "test-data" + File.separator + getClass().getSimpleName());
        MOP mop = new MOP();
        mop.setArtifactIds(mop.parseArtifactId("org.fusesource.mop:mop-core:1.0-SNAPSHOT"));
        assertNotNull(mop.classpath());

        mop.setOnline(false);
        assertNotNull(mop.classpath());
        mop.purgeRepository();

        Exception expected = null;
        try {
            mop.classpath();
        } catch (Exception e) {
            System.out.println("Got expected exception resolving artifact: " + e.getMessage());
            e.printStackTrace();
            expected = e;
        }
        assertNotNull(expected);

        mop.setOnline(true);
        assertNotNull(mop.classpath());
    }
}
