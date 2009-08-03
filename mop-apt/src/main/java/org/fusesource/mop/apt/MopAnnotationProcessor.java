/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.mop.apt;

import com.sun.mirror.apt.AnnotationProcessor;
import com.sun.mirror.apt.AnnotationProcessorEnvironment;
import com.sun.mirror.declaration.TypeDeclaration;
import static com.sun.mirror.util.DeclarationVisitors.NO_OP;
import static com.sun.mirror.util.DeclarationVisitors.getDeclarationScanner;

/**
 * @version $Revision: 1.1 $
 */
public class MopAnnotationProcessor implements AnnotationProcessor {
    private final AnnotationProcessorEnvironment env;

    public MopAnnotationProcessor(AnnotationProcessorEnvironment env) {
        this.env = env;
    }

    public void process() {
        CommandAnnotationProcessor processor = new CommandAnnotationProcessor(env);
        for (TypeDeclaration typeDecl : env.getSpecifiedTypeDeclarations()) {
            typeDecl.accept(getDeclarationScanner(processor, NO_OP));
        }
        processor.writeResources();
    }
}