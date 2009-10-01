package org.fusesource.mop;


import junit.framework.TestCase;

import org.fusesource.mop.support.ArtifactId;
import org.fusesource.mop.support.Logger;


/**
 * @author chirino
 */
public class MOPRepositoryTest extends TestCase {

    public void testInstall() throws Exception {
        Logger.debug = true;
        MOPRepository repository = new MOPRepository();


        String list = repository
            .classpath(ArtifactId.parse("xalan:xalan:2.7.1"));
        System.out.println(list);
    }

    public static void main(String args[]) throws Exception {
        Logger.debug = true;
        MOPRepository repository = new MOPRepository();
        String list = repository.classpath(ArtifactId.parse(args[0]));
        System.out.println(list);

    }

}