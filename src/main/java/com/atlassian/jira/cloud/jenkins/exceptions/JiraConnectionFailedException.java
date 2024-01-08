package com.atlassian.jira.cloud.jenkins.exceptions;

public class JiraConnectionFailedException extends Exception {
    public JiraConnectionFailedException(final String message) {
        super(message);
    }
}
