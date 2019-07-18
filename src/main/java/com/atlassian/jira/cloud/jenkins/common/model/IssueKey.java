package com.atlassian.jira.cloud.jenkins.common.model;

import java.util.Objects;

/** Represents an issue key from Jira (eg. TEST-123) */
public class IssueKey {
    private String value;

    public IssueKey(final String value) {
        this.value = value.toUpperCase();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        IssueKey issueKey = (IssueKey) o;
        return value.equals(issueKey.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
