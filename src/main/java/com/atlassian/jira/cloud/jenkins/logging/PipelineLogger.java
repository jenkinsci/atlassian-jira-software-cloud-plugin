package com.atlassian.jira.cloud.jenkins.logging;

import com.atlassian.jira.cloud.jenkins.config.JiraCloudPluginConfig;
import hudson.model.TaskListener;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

/**
 * A logger that logs output into the Jenkins pipeline logs. A Jenkins user can view these logs
 * during or after a pipeline run to analyze any potential errors.
 */
public class PipelineLogger {

    private final PrintStream printStream;

    private static PipelineLogger NOOP_INSTANCE;

    private Boolean debugLogging = Boolean.FALSE;

    /**
     * @param taskLogger a PrintStream that points to the Jenkins pipeline logs. See {@link
     *     TaskListener#getLogger()}.
     */
    public PipelineLogger(final PrintStream taskLogger) {
        this.printStream = taskLogger;

        JiraCloudPluginConfig config = JiraCloudPluginConfig.get();
        if (config != null) {
            this.debugLogging = config.getDebugLogging();
        }
    }

    public PipelineLogger(final PrintStream taskLogger, final Boolean debugLogging) {
        this.printStream = taskLogger;
        this.debugLogging = debugLogging;
    }

    public static PipelineLogger noopInstance() {
        if (NOOP_INSTANCE == null) {
            try {
                NOOP_INSTANCE =
                        new PipelineLogger(
                                new PrintStream(
                                        new OutputStream() {
                                            @Override
                                            public void write(final int b) {
                                                // do nothing
                                            }
                                        },
                                        true,
                                        "UTF8"));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        return NOOP_INSTANCE;
    }

    public void warn(final String message) {
        printStream.printf("[ATLASSIAN CLOUD PLUGIN] [WARN] %s%n", message);
        printStream.flush();
    }

    public void warn(final String message, final Exception e) {
        ByteArrayOutputStream stacktraceOut = new ByteArrayOutputStream();
        try {
            e.printStackTrace(new PrintStream(stacktraceOut, true, "UTF-8"));
        } catch (UnsupportedEncodingException e2) {
            error("Missing stacktrace because Jenkins server doesn't support UTF-8!");
        }
        printStream.printf(
                "[ATLASSIAN CLOUD PLUGIN] [WARN] %s Stacktrace: %s%n", message, stacktraceOut);
        printStream.flush();
    }

    public void info(final String message) {
        printStream.printf("[ATLASSIAN CLOUD PLUGIN] [INFO] %s%n", message);
        printStream.flush();
    }

    public void error(final String message) {
        printStream.printf("[ATLASSIAN CLOUD PLUGIN] [ERROR] %s%n", message);
        printStream.flush();
    }

    public void debug(final String message) {
        if (this.debugLogging) {
            printStream.printf("[ATLASSIAN CLOUD PLUGIN] [DEBUG] %s%n", message);
            printStream.flush();
        }
    }
}
