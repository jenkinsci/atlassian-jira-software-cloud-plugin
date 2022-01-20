package com.atlassian.jira.cloud.jenkins.util;

import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.State;
import hudson.model.Result;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Test;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class JenkinsToJiraStatusTest {

    @Mock private WorkflowRun run;

    @Test
    public void getStatus_forSuccess() {
        when(run.getResult()).thenReturn(Result.SUCCESS);
        final State result = JenkinsToJiraStatus.getState(run);

        assertThat(result.value).isEqualTo("successful");
    }

    @Test
    public void getStatus_forFailure() {
        when(run.getResult()).thenReturn(Result.FAILURE);
        final State result = JenkinsToJiraStatus.getState(run);

        assertThat(result.value).isEqualTo("failed");
    }

    @Test
    public void getStatus_forUnknown() {
        when(run.getResult()).thenReturn(Result.UNSTABLE);
        final State result = JenkinsToJiraStatus.getState(run);

        assertThat(result.value).isEqualTo("unknown");
    }
}
