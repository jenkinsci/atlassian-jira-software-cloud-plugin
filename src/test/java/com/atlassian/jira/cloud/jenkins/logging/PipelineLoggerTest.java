package com.atlassian.jira.cloud.jenkins.logging;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

public class PipelineLoggerTest {

    @Test
    public void stackTraceIsLogged() {
        Exception e = new RuntimeException("BWAAAH!");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(out);
        PipelineLogger pipelineLogger = new PipelineLogger(printStream, true);
        pipelineLogger.warn("log message with exception!", e);
        assertThat(out.toString())
                .contains(
                        "at com.atlassian.jira.cloud.jenkins.logging.PipelineLoggerTest.stackTraceIsLogged");
    }
}
