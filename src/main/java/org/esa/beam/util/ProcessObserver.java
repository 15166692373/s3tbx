package org.esa.beam.util;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.NullProgressMonitor;
import com.bc.ceres.core.ProgressMonitor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * An observer that notifies its {@link ProcessObserver.Handler handlers} about lines of characters that have been written
 * by a process to both {@code stdout} and {@code stderr} output streams.
 * <p/>
 * <pre>
 * TODO
 * External Process Invocation API (EPIA)
 * - address that executables might have different extensions (and paths) on different OS (.exe, .bat, .sh)
 * </pre>
 *
 * @author Norman Fomferra
 */
public class ProcessObserver {

    private static final String MAIN = "main";
    private static final String STDOUT = "stdout";
    private static final String STDERR = "stderr";

    private final Process process;

    private String processName;
    private int pollPeriod;
    private ProgressMonitor progressMonitor;
    private Handler handler;
    private Mode mode;
    private ObservedProcessImpl observedProcess;

    /**
     * The observation mode.
     */
    public enum Mode {
        /**
         * {@link ProcessObserver#start()} blocks until the process ends.
         */
        BLOCKING,
        /**
         * {@link ProcessObserver#start()} returns immediately.
         */
        NON_BLOCKING,
        /**
         * {@link ProcessObserver#start()} returns immediately, but the process cannot be cancelled.
         */
        NON_CANCELLABLE,
    }

    /**
     * Constructor.
     *
     * @param process The process to be observed
     */
    public ProcessObserver(final Process process) {
        this.process = process;
        this.processName = "process";
        this.pollPeriod = 100;
        this.progressMonitor = new NullProgressMonitor();
        this.handler = new DefaultHandler();
        this.mode = Mode.BLOCKING;
    }

    /**
     * Default is "process".
     *
     * @param processName A name that represents the process.
     * @return this
     */
    public ProcessObserver withName(String processName) {
        Assert.notNull(processName, "processName");
        this.processName = processName;
        return this;
    }

    /**
     * Default-handler prints to stdout / stderr.
     *
     * @param handler A handler.
     * @return this
     */
    public ProcessObserver withHandler(Handler handler) {
        Assert.notNull(handler, "handler");
        this.handler = handler;
        return this;
    }

    /**
     * Default does nothing.
     *
     * @param progressMonitor A progress monitor.
     * @return this
     */
    public ProcessObserver withProgressMonitor(ProgressMonitor progressMonitor) {
        Assert.notNull(progressMonitor, "progressMonitor");
        this.progressMonitor = progressMonitor;
        return this;
    }

    /**
     * Default is {@link Mode#BLOCKING}.
     *
     * @param mode The observation mode.
     * @return
     */
    public ProcessObserver withMode(Mode mode) {
        Assert.notNull(mode, "mode");
        this.mode = mode;
        return this;
    }

    /**
     * Default is 100 milliseconds.
     *
     * @param pollPeriod time in milliseconds between successive process status queries.
     * @return this
     */
    public ProcessObserver withPollPeriod(int pollPeriod) {
        Assert.notNull(pollPeriod, "pollPeriod");
        this.pollPeriod = pollPeriod;
        return this;
    }

    /**
     * Starts observing the given process.
     */
    public ObservedProcess start() {
        if (observedProcess != null) {
            throw new IllegalStateException("process already observed.");
        }
        observedProcess = new ObservedProcessImpl();
        observedProcess.startObservation();
        return observedProcess;
    }

    /**
     * The observed process.
     */
    public static interface ObservedProcess {
        /**
         * @return The process' name.
         */
        String getName();

        /**
         * Submits a request to cancel an observed process.
         */
        void cancel();
    }

    /**
     * A handler that will be notified during the observation of the process.
     */
    public static interface Handler {
        /**
         * Called if the process is started being observed.
         *
         * @param process The observed process.
         * @param pm      The progress monitor, that is used to monitor the progress of the running process.
         */
        void onObservationStarted(ObservedProcess process, ProgressMonitor pm);

        /**
         * Called if a new text line that has been received from {@code stdout}.
         *
         * @param process The observed process.
         * @param line    The line.
         * @param pm      The progress monitor, that is used to monitor the progress of the running process.
         */
        void onStdoutLineReceived(ObservedProcess process, String line, ProgressMonitor pm);

        /**
         * Called if a new text line that has been received from {@code stderr}.
         *
         * @param process The observed process.
         * @param line    The line.
         * @param pm      The progress monitor, that is used to monitor the progress of the running process.
         */
        void onStderrLineReceived(ObservedProcess process, String line, ProgressMonitor pm);

        /**
         * Called if the process is no longer being observed.
         *
         * @param process  The observed process.
         * @param exitCode The exit code, may be {@code null} if unknown.
         * @param pm       The progress monitor, that is used to monitor the progress of the running process.
         */
        void onObservationEnded(ObservedProcess process, Integer exitCode, ProgressMonitor pm);
    }

    /**
     * Default implementation of {@link Handler}, which simply prints observations to the console.
     */
    public static class DefaultHandler implements Handler {
        @Override
        public void onObservationStarted(ObservedProcess process, ProgressMonitor pm) {
            System.out.println(process.getName() + " started");
        }

        @Override
        public void onStdoutLineReceived(ObservedProcess process, String line, ProgressMonitor pm) {
            System.out.println(process.getName() + ": " + line);
        }

        @Override
        public void onStderrLineReceived(ObservedProcess process, String line, ProgressMonitor pm) {
            System.err.println(process.getName() + ": " + line);
        }

        @Override
        public void onObservationEnded(ObservedProcess process, Integer exitCode, ProgressMonitor pm) {
            System.out.println(process.getName() + " ended, exit code " + exitCode);
        }
    }

    private class LineReaderThread extends Thread {
        private final String type;

        public LineReaderThread(ThreadGroup threadGroup, String type) {
            super(threadGroup, processName + "-" + type);
            this.type = type;
        }

        @Override
        public void run() {
            try {
                read();
            } catch (IOException e) {
                // cannot be handled
            }
        }

        private void read() throws IOException {
            final InputStream inputStream = type.equals(STDOUT) ? process.getInputStream() : process.getErrorStream();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    fireLineReceived(line);
                }
            } finally {
                reader.close();
            }
        }

        private void fireLineReceived(String line) {
            if (type.equals(STDOUT)) {
                handler.onStdoutLineReceived(observedProcess, line, progressMonitor);
            } else {
                handler.onStderrLineReceived(observedProcess, line, progressMonitor);
            }
        }
    }

    private class ObservedProcessImpl implements ObservedProcess {
        private ThreadGroup threadGroup;
        private Thread stdoutThread;
        private Thread stderrThread;
        private boolean cancellationRequested;
        private boolean cancelled;

        ObservedProcessImpl() {
            this.threadGroup = new ThreadGroup(processName);
            this.stdoutThread = new LineReaderThread(threadGroup, STDOUT);
            this.stderrThread = new LineReaderThread(threadGroup, STDERR);
        }

        @Override
        public String getName() {
            return processName;
        }

        @Override
        public void cancel() {
            cancellationRequested = true;
        }


        private void startObservation() {
            handler.onObservationStarted(observedProcess, progressMonitor);
            stdoutThread.start();
            stderrThread.start();
            if (mode == Mode.BLOCKING) {
                awaitTermination();
            } else if (mode == Mode.NON_BLOCKING) {
                Thread mainThread = new Thread(threadGroup,
                                               new Runnable() {
                                                   @Override
                                                   public void run() {
                                                       awaitTermination();
                                                   }
                                               },
                                               processName + "-" + MAIN);
                mainThread.start();
            }

        }

        private void awaitTermination() {
            while (true) {
                try {
                    Thread.sleep(pollPeriod);
                } catch (InterruptedException e) {
                    // todo - parametrise what is best done now:
                    //      * 1. just leave, and let the process be unattended (current impl.)
                    //        2. destroy the process
                    //        3. throw a checked ProgressObserverException
                    handler.onObservationEnded(this, null, progressMonitor);
                    break;
                }
                if ((progressMonitor.isCanceled() || cancellationRequested) && !cancelled) {
                    cancelled = true;
                    // todo - parametrise what is best done now:
                    //        1. just leave, and let the process be unattended
                    //      * 2. destroy the process (current impl.)
                    //        3. throw a checked ProgressObserverException
                    process.destroy();
                }
                try {
                    final int exitCode = process.exitValue();
                    handler.onObservationEnded(this, exitCode, progressMonitor);
                    break;
                } catch (IllegalThreadStateException e) {
                    // ok, continue
                }
            }
        }
    }
}