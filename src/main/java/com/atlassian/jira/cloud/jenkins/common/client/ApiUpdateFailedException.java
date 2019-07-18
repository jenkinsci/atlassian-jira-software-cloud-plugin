package com.atlassian.jira.cloud.jenkins.common.client;

public class ApiUpdateFailedException extends RuntimeException {
    public ApiUpdateFailedException(final String errorMessage) {
        super(errorMessage);
    }
}
