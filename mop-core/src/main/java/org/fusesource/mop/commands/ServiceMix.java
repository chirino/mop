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

public class ServiceMix implements ConfiguresMop {
    
    private static final transient Log LOG = LogFactory.getLog(ServiceMix.class);
    
    private String version = "RELEASE";
    private MOP mop;
    
    @Command
    public void servicemix(List<String> params) throws Exception {
        LOG.info(String.format("Installing ServiceMix %s", version));
        
        mop.execute(new String[] {
        		"install", 
        		"org.apache.servicemix:apache-servicemix:" + getExtension() + ":" + version,
        		mop.getWorkingDirectory().getAbsolutePath()
        		});
        
        File root = getRoot();
        
        LOG.info(String.format("Starting ServiceMix %s", version));
        mop.exec(getCommand(root));
        final ShutdownHook hook = new ShutdownHook();
        Runtime.getRuntime().addShutdownHook(hook);

        for (String param : params) {
            LOG.info(String.format("Deploying JBI artifact %s", param));
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

    private boolean isWindows() {
    	String os = System.getProperty("os.name");
        return os != null && os.toLowerCase().contains("windows") ? true : false;
    }
    
    private File getRoot() {
    	String sep = File.separator;
    	return new File(mop.getWorkingDirectory().getAbsoluteFile() + sep + "apache-servicemix-" + version);
    }
    
    private String getExtension() {
        return isWindows() ? "zip" : "tar.gz";
    } 

    protected List<String> getCommand(File root) {
    	String sep = File.separator;
        List<String> command = new LinkedList<String>();
        String cmd = root.getAbsolutePath() + sep + "bin" + sep + "servicemix";
        if (isWindows())  {
        	cmd += ".bat";
        }
        command.add(cmd);
        
        if (!version.startsWith("3")) {
            command.add("server");
        }
        return command;
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
    
    private final class ShutdownHook extends Thread {

        private ShutdownHook() {
            super("MOP - ServiceMix shutdown hook thread");
            setDaemon(true);
        }
        
        @Override
        public void run() {
            try {
                ProcessRunner runner = mop.getProcessRunner();
                if( runner!=null ) {
                    LOG.info("Killing the forked ServiceMix instance");
                    runner.kill();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
