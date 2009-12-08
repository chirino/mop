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



import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableCollection;

import com.sun.mirror.apt.AnnotationProcessor;
import com.sun.mirror.apt.AnnotationProcessorEnvironment;
import com.sun.mirror.apt.AnnotationProcessorFactory;
import com.sun.mirror.declaration.AnnotationTypeDeclaration;



/**
 * @version $Revision: 1.1 $
 */

public class MopAnnotationProcessorFactory implements AnnotationProcessorFactory {
    public static final String COMMAND_ANNOTATION_CLASSNAME = "org.fusesource.mop.Command";

    // Process any set of annotations
    private static final Collection<String> SUPPORTED_ANNOTATIONS 
        = unmodifiableCollection(Arrays.asList(COMMAND_ANNOTATION_CLASSNAME));

    // No supported options
    private static final Collection<String> SUPPORTED_OPTIONS = emptySet();

    public Collection<String> supportedAnnotationTypes() {
        return SUPPORTED_ANNOTATIONS;
    }

    public Collection<String> supportedOptions() {
        return SUPPORTED_OPTIONS;
    }

    public AnnotationProcessor getProcessorFor(Set<AnnotationTypeDeclaration> atds,
                                               AnnotationProcessorEnvironment env) {
        return new MopAnnotationProcessor(env);
    }
}
