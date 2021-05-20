package com.atlassian.jira.cloud.jenkins.buildinfo.client;

import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.Builds;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.JiraBuildInfo;
import com.atlassian.jira.cloud.jenkins.util.JenkinsToJiraStatus;
import hudson.AbortException;
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper;

import java.time.Instant;
import java.util.Set;

public final class BuildPayloadBuilder {

    /**
     * Assembles a JiraBuildInfo with necessary parameters from the Jenkins context
     *
     * @param buildWrapper Jenkins context that provides project and build details
     * @param issueKeys Jira issue keys to associate the build info with
     * @return an assembled Builds payload
     */
    public static Builds getBuildPayload(
            final RunWrapper buildWrapper, final Set<String> issueKeys) {

        try {
            return new Builds(JiraBuildInfo.builder()
                    .withPipelineId(buildWrapper.getFullProjectName())
                    .withBuildNumber(buildWrapper.getNumber())
                    .withDisplayName(buildWrapper.getFullProjectName())
                    .withUpdateSequenceNumber(Instant.now().getEpochSecond())
                    .withLabel(buildWrapper.getDisplayName())
                    .withUrl(buildWrapper.getAbsoluteUrl())
                    .withState(JenkinsToJiraStatus.getStatus(buildWrapper.getCurrentResult()))
                    .withLastUpdated(Instant.now().toString())
                    .withIssueKeys(issueKeys)
                    .build());
        } catch (final AbortException e) {
            throw new RuntimeException(e);
        }
    }

}
