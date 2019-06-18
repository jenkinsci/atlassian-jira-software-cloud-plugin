package com.atlassian.jira.cloud.jenkins.buildinfo.client;

public class BuildUpdateFailedException extends RuntimeException {
    public BuildUpdateFailedException(final String errorMessage) {
        super(errorMessage);
    }
}
