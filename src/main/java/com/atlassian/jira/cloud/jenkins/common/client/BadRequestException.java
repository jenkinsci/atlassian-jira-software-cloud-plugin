package com.atlassian.jira.cloud.jenkins.common.client;

public class BadRequestException extends ApiUpdateFailedException {
    public BadRequestException(final String errorMessage) {
        super(errorMessage);
    }

    public BadRequestException(final String errorMessage, final Throwable cause) {
        super(errorMessage, cause);
    }
}
