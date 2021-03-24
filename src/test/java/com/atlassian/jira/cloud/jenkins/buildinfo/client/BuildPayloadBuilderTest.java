package com.atlassian.jira.cloud.jenkins.buildinfo.client;

import com.atlassian.jira.cloud.jenkins.BaseUnitTest;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.Builds;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.JiraBuildInfo;
import com.google.common.collect.ImmutableSet;
import hudson.model.Run;
import hudson.tasks.junit.TestResultAction;
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class BuildPayloadBuilderTest extends BaseUnitTest {

    private static final String ISSUE_KEY = "TEST-123";

    @Test
    public void testSuccessfulBuild() throws Exception {
        // when
        final RunWrapper runWrapper = mockRunWrapper("SUCCESS");
        final Builds buildPayload =
                BuildPayloadBuilder.getBuildPayload(runWrapper, ImmutableSet.of(ISSUE_KEY));

        final JiraBuildInfo buildInfo = buildPayload.getBuilds().get(0);
        // then
        assertThat(buildPayload.getProviderMetadata().getProduct()).isEqualTo("jenkins");
        assertThat(buildInfo.getPipelineId()).isEqualTo(runWrapper.getFullProjectName());
        assertThat(buildInfo.getBuildNumber()).isEqualTo(runWrapper.getNumber());
        assertThat(buildInfo.getDisplayName()).isEqualTo(runWrapper.getDisplayName());
        assertThat(buildInfo.getUrl()).isEqualTo(runWrapper.getAbsoluteUrl());
        assertThat(buildInfo.getState()).isEqualTo("successful");
        assertThat(buildInfo.getIssueKeys()).containsExactlyInAnyOrder(ISSUE_KEY);

        assertThat(buildInfo.getTestInfo()).isNotNull();
        assertThat(buildInfo.getTestInfo().getTotalNumber()).isEqualTo(10);
        assertThat(buildInfo.getTestInfo().getNumberPassed()).isEqualTo(5);
        assertThat(buildInfo.getTestInfo().getNumberFailed()).isEqualTo(3);
        assertThat(buildInfo.getTestInfo().getNumberSkipped()).isEqualTo(2);
    }

    @Test
    public void testFailedBuild() throws Exception {
        // when
        final RunWrapper runWrapper = mockRunWrapper("FAILURE");
        final Builds buildPayload =
                BuildPayloadBuilder.getBuildPayload(runWrapper, ImmutableSet.of(ISSUE_KEY));

        final JiraBuildInfo buildInfo = buildPayload.getBuilds().get(0);

        // then
        assertThat(buildPayload.getProviderMetadata().getProduct()).isEqualTo("jenkins");
        assertThat(buildInfo.getPipelineId()).isEqualTo(runWrapper.getFullProjectName());
        assertThat(buildInfo.getBuildNumber()).isEqualTo(runWrapper.getNumber());
        assertThat(buildInfo.getDisplayName()).isEqualTo(runWrapper.getDisplayName());
        assertThat(buildInfo.getUrl()).isEqualTo(runWrapper.getAbsoluteUrl());
        assertThat(buildInfo.getState()).isEqualTo("failed");
        assertThat(buildInfo.getIssueKeys()).containsExactlyInAnyOrder(ISSUE_KEY);
    }

    private RunWrapper mockRunWrapper(final String result) throws Exception {
        final RunWrapper runWrapper = mock(RunWrapper.class);
        final Run<?, ?> run = mock(Run.class);
        final TestResultAction testResultAction = mock(TestResultAction.class);

        when(runWrapper.getFullProjectName()).thenReturn("multibranch-1/TEST-123-branch-name");
        when(runWrapper.getNumber()).thenReturn(1);
        when(runWrapper.getDisplayName()).thenReturn("#1");
        when(runWrapper.getAbsoluteUrl())
                .thenReturn("http://localhost:8080/jenkins/multibranch-1/job/TEST-123-branch-name");
        when(runWrapper.getCurrentResult()).thenReturn(result);

        when(testResultAction.getTotalCount()).thenReturn(10);
        when(testResultAction.getFailCount()).thenReturn(3);
        when(testResultAction.getSkipCount()).thenReturn(2);

        doReturn(run).when(runWrapper).getRawBuild();
        doReturn(testResultAction).when(run).getAction(TestResultAction.class);

        return runWrapper;
    }
}
