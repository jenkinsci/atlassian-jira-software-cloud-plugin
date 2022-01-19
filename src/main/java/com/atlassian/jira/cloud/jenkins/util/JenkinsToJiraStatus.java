package com.atlassian.jira.cloud.jenkins.util;

import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.State;

import javax.annotation.Nullable;

public final class JenkinsToJiraStatus {
    /**
     * Maps a Jenkins Build status to Jira API build/deployment status
     *
     * @param jenkinsBuildStatus Status from Jenkins context (Run)
     * @return Status for Jira API payload
     */
    public static String getStatus(@Nullable final String jenkinsBuildStatus) {
        if (jenkinsBuildStatus == null) {
            return State.IN_PROGRESS.value;
        }

        if ("SUCCESS".equalsIgnoreCase(jenkinsBuildStatus)) {
            return State.SUCCESSFUL.value;
        }

        if ("FAILURE".equalsIgnoreCase(jenkinsBuildStatus)) {
            return State.FAILED.value;
        }

        if ("ABORTED".equalsIgnoreCase(jenkinsBuildStatus)) {
            return State.CANCELLED.value;
        }

        return State.UNKNOWN.value;
    }
}
