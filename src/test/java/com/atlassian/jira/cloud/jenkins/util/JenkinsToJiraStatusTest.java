package com.atlassian.jira.cloud.jenkins.util;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JenkinsToJiraStatusTest {

    @Test
    public void getStatus_forSuccess() {
        final String result = JenkinsToJiraStatus.getStatus("SUCCESS");

        assertThat(result).isEqualTo("successful");
    }

    @Test
    public void getStatus_forFailure() {
        final String result = JenkinsToJiraStatus.getStatus("FAILURE");

        assertThat(result).isEqualTo("failed");
    }

    @Test
    public void getStatus_forUnknown() {
        final String result = JenkinsToJiraStatus.getStatus("IN_PROGRESS");

        assertThat(result).isEqualTo("unknown");
    }

}
