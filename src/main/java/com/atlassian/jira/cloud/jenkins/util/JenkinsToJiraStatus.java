package com.atlassian.jira.cloud.jenkins.util;

public final class JenkinsToJiraStatus {

    private static final String STATUS_SUCCESSFUL = "successful";
    private static final String STATUS_FAILED = "failed";
    private static final String STATUS_UNKNOWN = "unknown";

    /**
     * Maps a Jenkins Build status to Jira API build/deployment status 
     * @param jenkinsBuildStatus Status from Jenkins context (Run)
     * @return Status for Jira API payload
     */
    public static String getStatus(final String jenkinsBuildStatus) {
        if ("SUCCESS".equalsIgnoreCase(jenkinsBuildStatus)) {
            return STATUS_SUCCESSFUL;
        }

        if ("FAILURE".equalsIgnoreCase(jenkinsBuildStatus)) {
            return STATUS_FAILED;
        }

        return STATUS_UNKNOWN;
    }
}
