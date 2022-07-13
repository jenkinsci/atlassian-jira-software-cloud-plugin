package com.atlassian.jira.cloud.jenkins.util;

import hudson.model.TaskListener;

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

    /**
     * @param taskLogger a PrintStream that points to the Jenkins pipeline logs. See {@link
     *     TaskListener#getLogger()}.
     */
    public PipelineLogger(final PrintStream taskLogger) {
        this.printStream = taskLogger;
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
        printStream.println("[ATLASSIAN CLOUD PLUGIN] [WARN] " + message);
    }

    public void info(final String message) {
        printStream.println("[ATLASSIAN CLOUD PLUGIN] [INFO]" + message);
    }
}
