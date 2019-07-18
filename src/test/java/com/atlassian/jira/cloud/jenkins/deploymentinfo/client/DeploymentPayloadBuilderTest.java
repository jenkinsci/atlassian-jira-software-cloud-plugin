package com.atlassian.jira.cloud.jenkins.deploymentinfo.client;

import com.atlassian.jira.cloud.jenkins.BaseUnitTest;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.Deployments;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.Environment;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.JiraDeploymentInfo;
import com.google.common.collect.ImmutableSet;
import hudson.AbortException;
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DeploymentPayloadBuilderTest extends BaseUnitTest {

    private static final String ISSUE_KEY = "TEST-123";

    @Test
    public void testSuccessfulBuild() throws Exception {
        // when
        final RunWrapper runWrapper = mockRunWrapper("SUCCESS");
        final Deployments deployments =
                DeploymentPayloadBuilder.getDeploymentInfo(runWrapper, mockEnvironment(), ImmutableSet.of(ISSUE_KEY));

        final JiraDeploymentInfo jiraDeploymentInfo = deployments.getDeployments().get(0);
        // then
        assertDeploymentResult(runWrapper, jiraDeploymentInfo, "successful");
    }

    @Test
    public void testFailedBuild() throws Exception {
        // when
        final RunWrapper runWrapper = mockRunWrapper("FAILURE");
        final Deployments deployments =
                DeploymentPayloadBuilder.getDeploymentInfo(runWrapper, mockEnvironment(), ImmutableSet.of(ISSUE_KEY));

        final JiraDeploymentInfo jiraDeploymentInfo = deployments.getDeployments().get(0);
        // then
        assertDeploymentResult(runWrapper, jiraDeploymentInfo, "failed");
    }

    private RunWrapper mockRunWrapper(final String result) throws Exception {
        final RunWrapper runWrapper = mock(RunWrapper.class);

        when(runWrapper.getFullProjectName()).thenReturn("multibranch-1/TEST-123-branch-name");
        when(runWrapper.getNumber()).thenReturn(1);
        when(runWrapper.getDisplayName()).thenReturn("#1");
        when(runWrapper.getAbsoluteUrl())
                .thenReturn("http://localhost:8080/jenkins/multibranch-1/job/TEST-123-branch-name");
        when(runWrapper.getCurrentResult()).thenReturn(result);

        return runWrapper;
    }

    private Environment mockEnvironment() {
        final Environment environment = mock(Environment.class);
        when(environment.getDisplayName()).thenReturn("prod-east-1");
        when(environment.getId()).thenReturn("prod-east-1");
        when(environment.getType()).thenReturn("production");
        return environment;
    }

    private void assertDeploymentResult(RunWrapper runWrapper, JiraDeploymentInfo jiraDeploymentInfo, String status) throws AbortException {
        assertThat(jiraDeploymentInfo.getIssueKeys()).containsExactlyInAnyOrder(ISSUE_KEY);
        assertThat(jiraDeploymentInfo.getDisplayName()).isEqualTo(runWrapper.getDisplayName());
        assertThat(jiraDeploymentInfo.getUrl()).isEqualTo(runWrapper.getAbsoluteUrl());
        assertThat(jiraDeploymentInfo.getDescription()).isEqualTo(runWrapper.getDisplayName());
        assertThat(jiraDeploymentInfo.getLabel()).isEqualTo(runWrapper.getDisplayName());
        assertThat(jiraDeploymentInfo.getState()).isEqualTo(status);
        assertThat(jiraDeploymentInfo.getEnvironment().getId()).isEqualTo("prod-east-1");
        assertThat(jiraDeploymentInfo.getEnvironment().getDisplayName()).isEqualTo("prod-east-1");
        assertThat(jiraDeploymentInfo.getEnvironment().getType()).isEqualTo("production");
        assertThat(jiraDeploymentInfo.getPipeline().getId()).isEqualTo(runWrapper.getFullProjectName());
        assertThat(jiraDeploymentInfo.getPipeline().getDisplayName()).isEqualTo(runWrapper.getFullProjectName());
        assertThat(jiraDeploymentInfo.getPipeline().getUrl()).isEqualTo(runWrapper.getAbsoluteUrl());
    }
}
