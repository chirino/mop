/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.mop.support;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.fusesource.mop.Artifacts;
import org.fusesource.mop.Description;
import org.fusesource.mop.Lookup;
import org.fusesource.mop.MOP;
import org.fusesource.mop.Optional;

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
        return "<arg(s>";
    }

    public Object getBean() {
        return bean;
    }

    public Method getMethod() {
        return method;
    }

    @Override
    public void executeCommand(MOP mop, LinkedList<String> argList) throws Exception {
        // lets inject fields
        for (Class<? extends Object> beanType = bean.getClass(); beanType != Object.class; beanType = beanType.getSuperclass()) {
            Field[] fields = beanType.getDeclaredFields();
            for (Field field : fields) {
                Lookup lookup = field.getAnnotation(Lookup.class);
                if (lookup != null) {
                    Class<?> type = field.getType();
                    Object value = mop.getContainer().lookup(type);
                    if (value != null) {
                        field.setAccessible(true);
                        field.set(bean, value);
                    }
                }
            }
        }

        Class<?>[] paramTypes = method.getParameterTypes();
        int size = paramTypes.length;
        Object[] args = new Object[size];
        Annotation[][] parameterAnnotations = method.getParameterAnnotations();


        for (int i = 0; i < size; i++) {
            Class<?> paramType = paramTypes[i];
            if (MOP.class.isAssignableFrom(paramType)) {
                args[i] = mop;
            } else if (Iterable.class.isAssignableFrom(paramType)) {
                // lets assume its the command arguments
                args[i] = argList;
            } else {
                if (argList.isEmpty()) {
                    // lets look and see if we should allow a null
                    Annotation[] annotations = parameterAnnotations[i];
                    if (isOptionalValue(annotations)) {
                        continue;
                    }
                    // TODO we need to find the name of the parameter to log better!
                    throw new Exception("missing argument!");
                }

                // now lets extract the actual arguments
                if (Artifacts.class.isAssignableFrom(paramType)) {
                    args[i] = mop.getArtifacts(argList);
                } else if (paramType == File.class) {
                    args[i] = new File(argList.removeFirst());
                } else if (paramType == String.class) {
                    args[i] = argList.removeFirst();
                } else {
                    throw new Exception("Unable to inject type " + paramType.getName() + " from arguments " + argList + " for method " + method);
                }
            }

        }

        if (bean instanceof ConfiguresMop) {
            ConfiguresMop configuresMop = (ConfiguresMop) bean;
            configuresMop.configure(mop);
        }
        try {
            method.invoke(bean, args);
        } catch (Exception e) {
            LOG.error("Failed to invoke " + method + " with args " + Arrays.asList(args) + " due to: " + e, e);
            throw e;
        }
    }

    /**
     * Returns true if the parameter is annotated as being optional
     */
    protected boolean isOptionalValue(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            if (annotation instanceof Optional) {
                return true;
            }
        }
        return false;

    }
}
