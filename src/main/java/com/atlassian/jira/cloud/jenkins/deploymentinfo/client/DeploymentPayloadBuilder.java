package com.atlassian.jira.cloud.jenkins.deploymentinfo.client;

import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.Deployments;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.Environment;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.JiraDeploymentInfo;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.Pipeline;
import com.atlassian.jira.cloud.jenkins.util.JenkinsToJiraStatus;
import hudson.AbortException;
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper;

import java.time.Instant;
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
            final Pipeline pipeline =
                    Pipeline.builder()
                            .withId(runWrapper.getFullProjectName())
                            .withDisplayName(runWrapper.getDisplayName())
                            .withUrl(runWrapper.getAbsoluteUrl())
                            .build();
            final long epochSecond = Instant.now().getEpochSecond();

            return new Deployments(
                    JiraDeploymentInfo.builder()
                    .withDeploymentSequenceNumber(epochSecond)
                    .withUpdateSequenceNumber(epochSecond)
                    .withIssueKeys(issueKeys)
                    .withDisplayName(runWrapper.getDisplayName())
                    .withUrl(runWrapper.getAbsoluteUrl())
                    .withDescription(runWrapper.getDisplayName())
                    .withLastUpdated(Instant.now().toString())
                    .withLabel(runWrapper.getDisplayName())
                    .withState(JenkinsToJiraStatus.getStatus(runWrapper.getCurrentResult()))
                    .withPipeline(pipeline)
                    .withEnvironment(environment)
                    .build());
        } catch (final AbortException e) {
            throw new RuntimeException(e);
        }
    }
}
