/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.mop;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A helper class for invoking a child process
 * 
 * @version $Revision: 1.1 $
 */
public class ProcessRunner {
    private static final transient Log LOG = LogFactory.getLog(ProcessRunner.class);

    private static AtomicLong processIdCounter = new AtomicLong(0);

    private Thread thread;
    private String id;
    private Process process;
    private AtomicBoolean running = new AtomicBoolean(true);
    private int exitValue = -1;
    private StreamPipe errorHandler;
    private StreamPipe outputHandler;
    private StreamPipe inputHandler;

    public static ProcessRunner newInstance(String id, String[] cmd, String[] env, File workingDirectory) throws Exception {
        return newInstance(id, cmd, env, workingDirectory, false);
    }

    public static ProcessRunner newInstance(String id, String[] cmd, String[] env, File workingDirectory, boolean redirectInput) throws Exception {
        return newInstance(id, cmd, env, workingDirectory, System.out, System.err, redirectInput);
    }

    /**
     * Returns a newly created process runner
     */
    private static ProcessRunner newInstance(String id, String[] cmd, String[] env, File workingDirectory, OutputStream sout, OutputStream serr, boolean redirectInput) throws Exception {
        final Process process = Runtime.getRuntime().exec(cmd, env, workingDirectory);

        if (process == null) {
            throw new Exception("Process launched failed (returned null).");
        }
        return new ProcessRunner(id, process, sout, serr, redirectInput);
    }

    public static String newId(String prefix) {
        return prefix + processIdCounter.incrementAndGet();
    }

    public ProcessRunner(String id, Process theProcess, OutputStream sout, OutputStream serr, boolean redirectInput) {
        this.id = id;
        this.process = theProcess;

        errorHandler = new StreamPipe(process.getErrorStream(), "Process Error Handler for: " + id, serr);
        outputHandler = new StreamPipe(process.getInputStream(), "Process Output Handler for: " + id, sout);
        if (redirectInput) {
            inputHandler = new StreamPipe(System.in, "Process Input Hander for: " + id, process.getOutputStream());
        }

        thread = new Thread("Process Watcher for: " + id) {
            @Override
            public void run() {
                try {
                    process.waitFor();
                    exitValue = process.exitValue();
                    //Prior to sending exit join the output
                    //handler threads to make sure that all
                    //data is sent:
                    errorHandler.join();
                    outputHandler.join();
                    if (inputHandler != null) {
                        inputHandler.interrupt();
                    }
                } catch (InterruptedException e) {
                } finally {
                    running.set(false);
                    onCompleted();
                }
            }
        };
        thread.start();
    }

    public void sendToInput(String s) {
        try {
            OutputStream out = process.getOutputStream();
            out.write(s.getBytes());
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Joins the process, waiting for it to complete
     * 
     * @returns the exit code
     */
    public int join() {
        while (isRunning()) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                // ignore
            }
        }
        return getExitValue();
    }

    /**
     * Returns -1 if the process is still running or the actual exit code if it
     * is completed
     */
    public int getExitValue() {
        return exitValue;
    }

    /**
     * Returns true if this process is still running
     * 
     * @return
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Kills the process
     */
    public void kill() throws Exception {
        if (running.compareAndSet(true, false)) {
            try {
                LOG.info("Killing process " + process + " [id = " + id + "]");
                process.destroy();
                if (thread != null) {
                    thread.join();
                }
                LOG.info("...DONE.");
            } catch (Exception e) {
                LOG.error("ERROR: destroying process " + process + " [id = " + id + "]", e);
                throw e;
            }
        }
    }

    /**
     * Provides a custom hook when a process completes (with or without a
     * failure)
     */
    protected void onCompleted() {
    }

    private class StreamPipe implements Runnable {
        private final InputStream in;
        private OutputStream out;
        private Thread thread;

        public StreamPipe(InputStream in, String name, OutputStream out) {
            this.in = new BufferedInputStream(in, 2048);
            this.out = new BufferedOutputStream(out, 2048);
            thread = new Thread(this, name);
            thread.setDaemon(true);
            thread.start();
        }

        public void join() throws InterruptedException {
            thread.join();
        }

        public void interrupt() {
            thread.interrupt();
        }

        public void run() {
            try {
                while (true) {
                    int b = in.read();
                    if (b < 0) {
                        break;
                    }

                    out.write(b);
                    if (in.available() > 0) {
                        continue;
                    }

                    try {
                        Thread.sleep(50);
                        
                    } catch (InterruptedException ie) {
                        if (in.available() > 0) {
                            Thread.currentThread().interrupt();
                            continue;
                        } else {
                            out.flush();
                            throw ie;
                        }
                    }
                    
                    if (in.available() > 0) {
                        continue;
                    }
                    out.flush();

                }
            } catch (EOFException expected) {
                // expected
            } catch (Exception e) {
                if (running.get()) {
                    LOG.error("ERROR: reading from process output: " + e, e);
                }
            }
        }
    }
}
