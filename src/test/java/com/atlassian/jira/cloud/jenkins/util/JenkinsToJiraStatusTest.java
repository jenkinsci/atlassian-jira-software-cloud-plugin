package com.atlassian.jira.cloud.jenkins.util;

import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.State;
import hudson.model.Result;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JenkinsToJiraStatusTest {

    private WorkflowRun run = mock(WorkflowRun.class);

    @Test
    public void getStatus_forSuccess() {
        when(run.getResult()).thenReturn(Result.SUCCESS);
        final State result = JenkinsToJiraStatus.getState(run.getResult());

        assertThat(result.value).isEqualTo("successful");
    }

    @Test
    public void getStatus_forFailure() {
        when(run.getResult()).thenReturn(Result.FAILURE);
        final State result = JenkinsToJiraStatus.getState(run.getResult());

        assertThat(result.value).isEqualTo("failed");
    }

    @Test
    public void getStatus_forUnknown() {
        when(run.getResult()).thenReturn(Result.UNSTABLE);
        final State result = JenkinsToJiraStatus.getState(run.getResult());

        assertThat(result.value).isEqualTo("unknown");
    }

    /**
     * When the build is still running (isCompleted=false) and getResult() is null, the state should
     * be IN_PROGRESS.
     */
    @Test
    public void getStatus_forNullResult_whenBuildIsRunning_shouldBeInProgress() {
        final State result = JenkinsToJiraStatus.getState((Result) null, false);

        assertThat(result).isEqualTo(State.IN_PROGRESS);
    }

    /**
     * In scripted pipelines, WorkflowRun.getResult() returns null for successful builds even after
     * completion (see JENKINS-46325). When the build has completed (isCompleted=true) and
     * getResult() is null, the state should be SUCCESSFUL.
     */
    @Test
    public void getStatus_forNullResult_whenBuildIsComplete_shouldBeSuccessful() {
        final State result = JenkinsToJiraStatus.getState((Result) null, true);

        assertThat(result).isEqualTo(State.SUCCESSFUL);
    }

    /**
     * The single-arg overload (backwards-compatible) treats null as IN_PROGRESS, which is the safe
     * default for callers that don't have completion context.
     */
    @Test
    public void getStatus_forNullResult_singleArgOverload_defaultsToInProgress() {
        final State result = JenkinsToJiraStatus.getState((Result) null);

        assertThat(result).isEqualTo(State.IN_PROGRESS);
    }
}
