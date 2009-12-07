/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.mop.support;

/**
 * @version $Revision: 1.1 $
 */
public class Logger {
    public static boolean debug;

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

    public static void info(String message) {
        System.out.println("[INFO] " + message);
    }

    public static void info(String message, Throwable e) {
        System.out.println("[INFO] " + message + ". " + e);
        if( debug ) {
            e.printStackTrace();
        }
    }

    public static void warn(String message) {
        System.out.println("[WARN] " + message);
    }

    public static void warn(String message, Throwable e) {
        System.out.println("[WARN] " + message + ". " + e);
        if( debug ) {
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
