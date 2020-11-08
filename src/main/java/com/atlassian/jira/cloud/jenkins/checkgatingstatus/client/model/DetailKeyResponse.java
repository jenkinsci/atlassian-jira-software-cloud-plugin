package com.atlassian.jira.cloud.jenkins.checkgatingstatus.client.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DetailKeyResponse {
    private final String type;
    private final String issueKey;
    private final String issueLink;

    @JsonCreator
    public DetailKeyResponse(
            @JsonProperty("type") final String type,
            @JsonProperty("issueKey") final String issueKey,
            @JsonProperty("issueLink") final String issueLink) {
        this.type = type;
        this.issueKey = issueKey;
        this.issueLink = issueLink;
    }

    public String getType() {
        return type;
    }

    public String getIssueKey() {
        return issueKey;
    }

    public String getIssueLink() {
        return issueLink;
    }
}
