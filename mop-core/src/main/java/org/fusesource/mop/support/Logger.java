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
