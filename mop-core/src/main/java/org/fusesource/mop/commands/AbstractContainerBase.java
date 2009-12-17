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
package org.fusesource.mop.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.plexus.util.FileUtils;
import org.fusesource.mop.MOP;
import org.fusesource.mop.ProcessRunner;
import org.fusesource.mop.support.ConfiguresMop;

public abstract class AbstractContainerBase implements ConfiguresMop {
    
    private static final transient Log LOG = LogFactory.getLog(AbstractContainerBase.class);
    private static final long START_DELAY = 25 * 1000; // 25 secs
    
    protected String version = "RELEASE";
    protected MOP mop;
    protected String[] environment;
    protected String secondaryArgs = "";
    
    public void installAndLaunch(List<String> params) throws Exception {
        LOG.info(String.format("Installing " + getContainerName() + " %s", version));
        
        mop.execute(new String[] {
                "install", 
                getArtefactId() + getExtension() + ":" + version,
                mop.getWorkingDirectory().getAbsolutePath()
                });
        
        File root = getRoot();
        
        LOG.info(String.format("Starting " + getContainerName() + " %s", version));
        mop.exec(getCommand(root, params), environment);
        final ShutdownHook hook = new ShutdownHook();
        Runtime.getRuntime().addShutdownHook(hook);

        for (String param : params) {
            LOG.info(String.format("Deploying artifact %s", param));
            File file = getFile(param);
            File deployFolder = getDeployFolder(root);
            FileUtils.copyFileToDirectory(file, deployFolder);            
        }

        List<String> secondary = getSecondaryCommand(root, params);
        if (secondary != null) {
            Thread.sleep(START_DELAY);
            mop.execAndWait(secondary);
        }
                
        ProcessRunner runner = mop.getProcessRunner();
        
        String input = getInput();
        if (input != null) {
            Thread.sleep(5 * 1000);
            runner.sendToInput(input);
        }
        
        if( runner!=null ) {
            runner.join();
        }
        LOG.info(getContainerName() + " has been stopped");
        
        //ServiceMix instance has been stopped -- we no longer need the shutdown hook to kill it
        Runtime.getRuntime().removeShutdownHook(hook);
    }
    
    protected abstract String getContainerName();
    
    protected abstract String getArtefactId();
    
    protected abstract String getPrefix();
    
    protected abstract String getCommandName();
    
    protected abstract List<String> processArgs(List<String> command, List<String> params);
    
    protected abstract File getDeployFolder(File root);

    protected abstract List<String> getSecondaryCommand(File root, List<String> params);
    
    protected abstract String getInput();

    protected String[] getEnvironment() {
        return environment;
    }

    protected void extractEnvironment(List<String> params) {
        Map<String, String> map = new HashMap<String, String>();
        for (int i = 0 ; i < params.size() ; i++) {
            String param = params.get(i);
            if ("-e".equals(param) || "--environment".equals(param)) {
                params.remove(i);
                List<String> envs = new ArrayList<String>();
                while  (i < params.size() && !params.get(i).startsWith("--")) {
                    String env = params.remove(i);
                    String name = env.substring(0, env.indexOf("="));
                    String value = env.substring(env.indexOf("=") + 1);
                    if (map.containsKey(name)) {
                        envs.remove(name + "=" + map.get(name));
                        map.put(name, map.get(name) + " " + value);
                        env = name + "=" + map.get(name);
                    } else {
                        map.put(name, value);
                    }
                    envs.add(env);
                }
                environment = envs.toArray(new String[envs.size()]);
                break;
            }
        }
    }

    protected void extractSecondaryCommands(List<String> params) {
        for (int i = 0 ; i < params.size() ; i++) {
            String param = params.get(i);
            if ("-c".equals(param) || "--commands".equals(param)) {
                int remaining = params.size() - (i + 1);
                params.remove(i);
                for (int j = 0 ; j < remaining ; j++) {
                    secondaryArgs += params.get(i);
                    secondaryArgs += " ";
                    params.remove(i);
                }
            }
        }
    }
    
    protected boolean isWindows() {
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

    protected List<String> getCommand(File root, List<String> params) {
        String sep = File.separator;
        List<String> command = new LinkedList<String>();
        String cmd = root.getAbsolutePath() + sep + "bin" + sep + getCommandName();
        if (isWindows())  {
            cmd += ".bat";
        }
        command.add(cmd);

        return processArgs(command, params);
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
