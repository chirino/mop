/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.mop.support;

import org.fusesource.mop.MOP;

/**
 * A marker interface to allow a Command to configure the MOP before an attempt is made
 * to inject its method parameters such as setting the default type or enabling or disabling
 * transitive dependencies etc.
 *
 * @version $Revision: 1.1 $
 */
public interface ConfiguresMop {
    void configure(MOP mop);
}
