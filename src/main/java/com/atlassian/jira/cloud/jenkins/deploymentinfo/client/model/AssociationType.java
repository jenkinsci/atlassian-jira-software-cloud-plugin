package com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model;

import com.fasterxml.jackson.annotation.JsonValue;

/** Available association types */
public enum AssociationType {
    SERVICE_ID_OR_KEYS("serviceIdOrKeys"),
    ISSUE_KEYS("issueKeys");

    private final String value;

    AssociationType(final String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
