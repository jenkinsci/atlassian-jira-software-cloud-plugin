package com.atlassian.jira.cloud.jenkins.exceptions;

public class JiraConnectionFailedException extends Exception {
    public JiraConnectionFailedException(final String message) {
        super(message);
    }

    public JiraConnectionFailedException(final Throwable cause) {
        super(cause);
    }

    public JiraConnectionFailedException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
