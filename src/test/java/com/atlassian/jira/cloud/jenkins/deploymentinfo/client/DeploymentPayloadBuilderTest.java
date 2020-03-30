package com.atlassian.jira.cloud.jenkins.deploymentinfo.client;

import com.atlassian.jira.cloud.jenkins.BaseUnitTest;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.Association;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.AssociationType;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.Deployments;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.Environment;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.JiraDeploymentInfo;
import com.google.common.collect.ImmutableSet;
import hudson.AbortException;
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper;
import org.junit.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DeploymentPayloadBuilderTest extends BaseUnitTest {

    private static final String ISSUE_KEY = "TEST-123";

    private static final String SERVICE_ID = "aGVsbG8K";
    private static final Association ISSUE_KEY_ASSOCIATION =
            Association.builder()
                    .withAssociationType(AssociationType.ISSUE_KEYS)
                    .withValues(ImmutableSet.of(ISSUE_KEY))
                    .build();

    private static final Association SERVICE_ID_ASSOCIATION =
            Association.builder()
                    .withAssociationType(AssociationType.SERVICE_ID_OR_KEYS)
                    .withValues(ImmutableSet.of(SERVICE_ID))
                    .build();

    private static final Set<Association> ASSOCIATIONS =
            ImmutableSet.of(SERVICE_ID_ASSOCIATION, ISSUE_KEY_ASSOCIATION);

    @Test
    public void testSuccessfulBuild() throws Exception {
        // when
        final RunWrapper runWrapper = mockRunWrapper();
        final Deployments deployments =
                DeploymentPayloadBuilder.getDeploymentInfo(runWrapper, mockEnvironment(), ASSOCIATIONS, "successful");

        final JiraDeploymentInfo jiraDeploymentInfo = deployments.getDeployments().get(0);
        // then
        assertThat(deployments.getProviderMetadata().getProduct()).isEqualTo("jenkins");
        assertDeploymentResult(runWrapper, jiraDeploymentInfo, "successful");
    }

    @Test
    public void testFailedBuild() throws Exception {
        // when
        final RunWrapper runWrapper = mockRunWrapper();
        final Deployments deployments =
                DeploymentPayloadBuilder.getDeploymentInfo(runWrapper, mockEnvironment(), ASSOCIATIONS, "failed");

        final JiraDeploymentInfo jiraDeploymentInfo = deployments.getDeployments().get(0);
        // then
        assertThat(deployments.getProviderMetadata().getProduct()).isEqualTo("jenkins");
        assertDeploymentResult(runWrapper, jiraDeploymentInfo, "failed");
    }

    private RunWrapper mockRunWrapper() throws Exception {
        final RunWrapper runWrapper = mock(RunWrapper.class);

        when(runWrapper.getFullProjectName()).thenReturn("multibranch-1/TEST-123-branch-name");
        when(runWrapper.getNumber()).thenReturn(1);
        when(runWrapper.getDisplayName()).thenReturn("#1");
        when(runWrapper.getAbsoluteUrl())
                .thenReturn("http://localhost:8080/jenkins/multibranch-1/job/TEST-123-branch-name");
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
        assertThat(jiraDeploymentInfo.getAssociations()).containsExactlyInAnyOrder(ISSUE_KEY_ASSOCIATION, SERVICE_ID_ASSOCIATION);
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
