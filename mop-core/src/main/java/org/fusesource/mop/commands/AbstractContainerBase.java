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
import org.fusesource.mop.ProcessRunner;
import org.fusesource.mop.support.ConfiguresMop;

public abstract class AbstractContainerBase implements ConfiguresMop {
    
    private static final transient Log LOG = LogFactory.getLog(AbstractContainerBase.class);
    
    protected String version = "RELEASE";
    protected MOP mop;
    
    public void installAndLaunch(List<String> params) throws Exception {
        LOG.info(String.format("Installing " + getContainerName() + " %s", version));
        
        mop.execute(new String[] {
                "install", 
                getArtefactId() + getExtension() + ":" + version,
                mop.getWorkingDirectory().getAbsolutePath()
                });
        
        File root = getRoot();
        
        LOG.info(String.format("Starting " + getContainerName() + " %s", version));
        mop.exec(getCommand(root));
        final ShutdownHook hook = new ShutdownHook();
        Runtime.getRuntime().addShutdownHook(hook);

        for (String param : params) {
            LOG.info(String.format("Deploying artifact %s", param));
            File file = getFile(param);
            File deployFolder = getDeployFolder(root);
            FileUtils.copyFileToDirectory(file, deployFolder);            
        }

        ProcessRunner runner = mop.getProcessRunner();
        if( runner!=null ) {
            runner.join();
        }
        LOG.info("ServiceMix has been stopped");
        
        //ServiceMix instance has been stopped -- we no longer need the shutdown hook to kill it
        Runtime.getRuntime().removeShutdownHook(hook);
    }
    
    protected abstract String getContainerName();
    
    protected abstract String getArtefactId();
    
    protected abstract String getPrefix();
    
    protected abstract String getCommandName();
    
    protected abstract List<String> addArgs(List<String> command);
    
    protected abstract File getDeployFolder(File root);


    private boolean isWindows() {
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase().contains("windows") ? true : false;
    }
    
    private File getRoot() {
        String sep = File.separator;
        return new File(mop.getWorkingDirectory().getAbsoluteFile() + sep + getPrefix() + version);
    }
    
    private String getExtension() {
        return isWindows() ? "zip" : "tar.gz";
    } 

    protected List<String> getCommand(File root) {
        String sep = File.separator;
        List<String> command = new LinkedList<String>();
        String cmd = root.getAbsolutePath() + sep + "bin" + sep + getCommandName();
        if (isWindows())  {
            cmd += ".bat";
        }
        command.add(cmd);

        return addArgs(command);
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
    
    private final class ShutdownHook extends Thread {

        private ShutdownHook() {
            super("MOP - " + getContainerName() + " shutdown hook thread");
            setDaemon(true);
        }
        
        @Override
        public void run() {
            try {
                ProcessRunner runner = mop.getProcessRunner();
                if( runner!=null ) {
                    LOG.info("Killing the forked " + getContainerName() + " instance");
                    runner.kill();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
