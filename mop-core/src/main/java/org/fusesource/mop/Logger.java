/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.mop;

/**
 * @version $Revision: 1.1 $
 */
public class Logger {
    static boolean debug;

    public static void debug(String message) {
        if (debug) {
            System.out.println("[DEBUG] " + message);
        }
    }

    public static void debug(String message, Throwable e) {
        if (debug) {
            System.out.println("[DEBUG] " + message + ". " + e);
            e.printStackTrace();
        }
    }

    public static boolean isDebug() {
        return debug;
    }

    public static void setDebug(boolean debug) {
        Logger.debug = debug;
    }

}
