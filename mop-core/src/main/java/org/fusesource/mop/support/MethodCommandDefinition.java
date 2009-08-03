/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.mop.support;

import org.fusesource.mop.Description;
import org.fusesource.mop.MOP;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import java.util.Arrays;

/**
 * @version $Revision: 1.1 $
 */
public class MethodCommandDefinition extends CommandDefinition {
    private static final transient Log LOG = LogFactory.getLog(MethodCommandDefinition.class);

    private final Object bean;
    private final Method method;

    public MethodCommandDefinition(Object bean, Method method) {
        super(method.getName(), createUsage(method), "");
        this.bean = bean;
        this.method = method;

        Description description = method.getAnnotation(Description.class);
        if (description != null) {
            setDescription(description.value());
        }
    }

    protected static String createUsage(Method method) {
        // TODO we could auto-create these from the types and the compile time names?

        // lets look at the arguments and determine the description to use
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        Class<?>[] paramTypes = method.getParameterTypes();
        for (Class<?> paramType : paramTypes) {
            String name = paramType.getName();
            if (first) {
                first = false;
                if (name.startsWith("java.util.List")) {
                    // TODO dirty hack!
                    builder.append("<artifact(s)>");
                    continue;
                }
            } else {
                builder.append(" ");

                // TODO figure out how to turn the parameter names into a meaningful usage string
                builder.append("<arg(s)>");
                break;
            }
        }
        return builder.toString();
    }

    public Object getBean() {
        return bean;
    }

    public Method getMethod() {
        return method;
    }

    @Override
    public void executeCommand(MOP mop, LinkedList<String> argList) throws Exception {
        Class<?>[] paramTypes = method.getParameterTypes();
        int size = paramTypes.length;
        Object[] args = new Object[size];
        for (int i = 0; i < size; i++) {
            Class<?> paramType = paramTypes[i];
            if (MOP.class.isAssignableFrom(paramType)) {
                args[i] = mop;
            }
            else if (Iterable.class.isAssignableFrom(paramType)) {
                // lets assume its the files
                List<File> list = mop.parseArtifacts(argList);
                args[i] = list;
            } else if (paramType == File.class) {
                args[i] = new File(argList.removeFirst());
            } else if (paramType == String.class) {
                args[i] = new File(argList.removeFirst());
            } else {
                throw new Exception("Unable to inject type " + paramType.getName() + " from arguments " + argList + " for method " + method);
            }

        }

        if (bean instanceof HasDefaultTargetType) {
            HasDefaultTargetType hasDefaultTargetType = (HasDefaultTargetType) bean;
            mop.setDefaultType(hasDefaultTargetType.getDefaultType());
        }
        try {
            method.invoke(bean, args);
        } catch (Exception e) {
            LOG.error("Failed to invoke " + method + " with args " + Arrays.asList(args) + " due to: " + e, e);
            throw e;
        }
    }
}
