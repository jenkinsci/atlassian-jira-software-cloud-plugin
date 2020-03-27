package com.atlassian.jira.cloud.jenkins.deploymentinfo.client;

import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.Association;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.AssociationType;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.Deployments;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.Environment;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.JiraDeploymentInfo;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.Pipeline;
import com.atlassian.jira.cloud.jenkins.util.JenkinsToJiraStatus;
import hudson.AbortException;
import hudson.model.Run;
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

public final class DeploymentPayloadBuilder {

    /**
     * Assembles a JiraDeploymentInfo with necessary parameters from the Jenkins context
     *
     * @param runWrapper Jenkins context that provides project and build details
     * @param environment Deployment environment
     * @param issueKeys Jira issue keys to associate the build info with
     * @param state deployment state
     * @return an assembled Deployments payload
     */
    public static Deployments getDeploymentInfo(
            final RunWrapper runWrapper,
            final Environment environment,
            final Set<String> issueKeys,
            final String state) {
        try {
            return new Deployments(
                    JiraDeploymentInfo.builder()
                    .withDeploymentSequenceNumber(runWrapper.getNumber())
                    .withUpdateSequenceNumber(Instant.now().getEpochSecond())
                    .withAssociations(getAssociations(issueKeys))
                    .withDisplayName(runWrapper.getDisplayName())
                    .withUrl(runWrapper.getAbsoluteUrl())
                    .withDescription(runWrapper.getDisplayName())
                    .withLastUpdated(Instant.now().toString())
                    .withLabel(runWrapper.getDisplayName())
                    .withState(state)
                    .withPipeline(getPipeline(runWrapper))
                    .withEnvironment(environment)
                    .build());
        } catch (final AbortException e) {
            throw new RuntimeException(e);
        }
    }

    private static Set<Association> getAssociations(final Set<String> issueKeys) {
        return Collections.singleton(Association.builder()
            .withAssociationType(AssociationType.ISSUE_KEYS)
            .withValues(issueKeys)
            .build());
    }

    private static Pipeline getPipeline(final RunWrapper runWrapper) throws AbortException {
        final Optional<? extends Run<?, ?>> build = Optional.ofNullable(runWrapper.getRawBuild());

        return Pipeline.builder()
                .withId(runWrapper.getFullProjectName())
                .withDisplayName(runWrapper.getFullProjectName())
                .withUrl(build.map(b -> b.getParent().getAbsoluteUrl()).orElse(runWrapper.getAbsoluteUrl()))
                .build();
    }
}
