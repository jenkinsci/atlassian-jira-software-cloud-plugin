package com.atlassian.jira.cloud.jenkins.buildinfo.client;

import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.JiraBuildInfo;
import hudson.AbortException;
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper;

import java.time.Instant;
import java.util.Set;

public final class BuildPayloadBuilder {

    private static final String STATUS_SUCCESSFUL = "successful";
    private static final String STATUS_FAILED = "failed";
    private static final String STATUS_UNKNOWN = "unknown";

    /**
     * Assembles a JiraBuildInfo with necessary parameters from the Jenkins context
     *
     * @param buildWrapper Jenkins context that provides project and build details
     * @param issueKeys Jira issue keys to associate the build info with
     * @return an assembled JiraBuildInfo
     */
    public static JiraBuildInfo getBuildInfo(
            final RunWrapper buildWrapper, final Set<String> issueKeys) {

        try {
            return JiraBuildInfo.builder()
                    .withPipelineId(buildWrapper.getFullProjectName())
                    .withBuildNumber(buildWrapper.getNumber())
                    .withDisplayName(buildWrapper.getDisplayName())
                    .withUpdateSequenceNumber(Instant.now().getEpochSecond())
                    .withLabel(buildWrapper.getDisplayName())
                    .withUrl(buildWrapper.getAbsoluteUrl())
                    .withState(getBuildStatus(buildWrapper.getCurrentResult()))
                    .withLastUpdated(Instant.now().toString())
                    .withIssueKeys(issueKeys)
                    .build();
        } catch (final AbortException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getBuildStatus(final String result) {
        if ("SUCCESS".equalsIgnoreCase(result)) {
            return STATUS_SUCCESSFUL;
        }

        if ("FAILURE".equalsIgnoreCase(result)) {
            return STATUS_FAILED;
        }

        return STATUS_UNKNOWN;
    }
}
