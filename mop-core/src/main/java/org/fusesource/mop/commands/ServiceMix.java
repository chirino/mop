/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.mop.commands;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.plexus.util.FileUtils;
import org.fusesource.mop.Command;
import org.fusesource.mop.MOP;
import org.fusesource.mop.support.ConfiguresMop;

public class ServiceMix implements ConfiguresMop {
    
    private static final transient Log LOG = LogFactory.getLog(ServiceMix.class);
    
    private String version = "RELEASE";
    private MOP mop;
    
    @Command
    public void servicemix(List<String> params) throws Exception {
        LOG.info(String.format("Installing ServiceMix %s", version));
        
        mop.execute(new String[] {"install", "org.apache.servicemix:apache-servicemix:tar.gz:"+version, mop.getWorkingDirectory().getAbsolutePath()});
        
        File root = new File(mop.getWorkingDirectory().getAbsoluteFile() + "/apache-servicemix-" + version);
        mop.execute(getCommand(root));
           
        for (String param : params) {
            LOG.info(String.format("Deploying JBI artifact %s", param));
            File file = getFile(param);
            File deployFolder = getDeployFolder(root);
            FileUtils.copyFileToDirectory(file, deployFolder);            
        }
    }

    protected String[] getCommand(File root) {
        if (version.startsWith("3")) {
            return new String[] { "shell", root.getAbsolutePath() + "/bin/servicemix" };
        } else {
            return new String[] { "shell", root.getAbsolutePath() + "/bin/servicemix", "server" };
        }
    }

    protected File getDeployFolder(File root) {
        if (version.startsWith("3")) {
            return new File(root, "hotdeploy");
        } else {
            return new File(root, "deploy");
        }
    }

    private File getFile(String id) throws Exception {
        LinkedList<String> ids = new LinkedList<String>();
        ids.add(id);
        return mop.getArtifacts(ids).getFiles().get(0);
    }

    public void configure(MOP mop) {
        version = mop.getDefaultVersion();
        this.mop = mop;
    }
}
