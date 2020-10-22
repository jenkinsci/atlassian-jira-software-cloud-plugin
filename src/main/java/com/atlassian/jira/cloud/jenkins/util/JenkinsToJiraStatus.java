package com.atlassian.jira.cloud.jenkins.util;

import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.State;

public final class JenkinsToJiraStatus {
    /**
     * Maps a Jenkins Build status to Jira API build/deployment status 
     * @param jenkinsBuildStatus Status from Jenkins context (Run)
     * @return Status for Jira API payload
     */
    public static String getStatus(final String jenkinsBuildStatus) {
        if ("SUCCESS".equalsIgnoreCase(jenkinsBuildStatus)) {
            return State.SUCCESSFUL.value;
        }

        if ("FAILURE".equalsIgnoreCase(jenkinsBuildStatus)) {
            return State.FAILED.value;
        }

        return State.UNKNOWN.value;
    }
}
