package com.atlassian.jira.cloud.jenkins.common.model;

/**
 * Represents an issue key from Jira (eg. TEST-123)
 */
public class IssueKey {
    private String value;

    public IssueKey(final String value) {
        this.value = value.toUpperCase();
    }

    @Override
    public String toString() {
        return value;
    }
}
