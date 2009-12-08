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