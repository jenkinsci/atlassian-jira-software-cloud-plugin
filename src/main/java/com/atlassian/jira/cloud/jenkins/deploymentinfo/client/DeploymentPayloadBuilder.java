package com.atlassian.jira.cloud.jenkins.deploymentinfo.client;

import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.Deployments;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.Environment;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.JiraDeploymentInfo;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.Pipeline;
import com.atlassian.jira.cloud.jenkins.util.JenkinsToJiraStatus;
import hudson.AbortException;
import hudson.model.Run;
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

public final class DeploymentPayloadBuilder {

    /**
     * Assembles a JiraDeploymentInfo with necessary parameters from the Jenkins context
     *
     * @param runWrapper Jenkins context that provides project and build details
     * @param environment Deployment environment
     * @param issueKeys Jira issue keys to associate the build info with
     * @return an assembled JiraBuildInfo
     */
    public static Deployments getDeploymentInfo(
            final RunWrapper runWrapper,
            final Environment environment,
            final Set<String> issueKeys) {
        try {
            return new Deployments(
                    JiraDeploymentInfo.builder()
                    .withDeploymentSequenceNumber(runWrapper.getNumber())
                    .withUpdateSequenceNumber(Instant.now().getEpochSecond())
                    .withIssueKeys(issueKeys)
                    .withDisplayName(runWrapper.getDisplayName())
                    .withUrl(runWrapper.getAbsoluteUrl())
                    .withDescription(runWrapper.getDisplayName())
                    .withLastUpdated(Instant.now().toString())
                    .withLabel(runWrapper.getDisplayName())
                    .withState(JenkinsToJiraStatus.getStatus(runWrapper.getCurrentResult()))
                    .withPipeline(getPipeline(runWrapper))
                    .withEnvironment(environment)
                    .build());
        } catch (final AbortException e) {
            throw new RuntimeException(e);
        }
    }

    private static Pipeline getPipeline(RunWrapper runWrapper) throws AbortException {
        final Optional<? extends Run<?, ?>> build = Optional.ofNullable(runWrapper.getRawBuild());

        return Pipeline.builder()
                .withId(runWrapper.getFullProjectName())
                .withDisplayName(runWrapper.getFullProjectName())
                .withUrl(build.map(b -> b.getParent().getUrl()).orElse(runWrapper.getAbsoluteUrl()))
                .build();
    }
}
