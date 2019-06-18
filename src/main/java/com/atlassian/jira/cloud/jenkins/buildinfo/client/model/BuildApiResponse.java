package com.atlassian.jira.cloud.jenkins.buildinfo.client.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * When a build is submitted via the API, the response includes the list of
 * accepted builds (which will appear in the respective Jira issue view),
 * rejected builds (with appropriated reason on why it was not accepted),
 * unknown issue keys (which are not present in the configured Jira Cloud site)
 */
public class BuildApiResponse {
    private final List<BuildKeyResponse> acceptedBuilds;
    private final List<RejectedBuildResponse> rejectedBuilds;
    private final List<String> unknownIssueKeys;

    @JsonCreator
    public BuildApiResponse(
            @JsonProperty("acceptedBuilds") final List<BuildKeyResponse> acceptedBuilds,
            @JsonProperty("rejectedBuilds") final List<RejectedBuildResponse> rejectedBuilds,
            @JsonProperty("unknownIssueKeys") final List<String> unknownIssueKeys) {
        this.acceptedBuilds = acceptedBuilds;
        this.rejectedBuilds = rejectedBuilds;
        this.unknownIssueKeys = unknownIssueKeys;
    }

    public List<BuildKeyResponse> getAcceptedBuilds() {
        return acceptedBuilds;
    }

    public List<RejectedBuildResponse> getRejectedBuilds() {
        return rejectedBuilds;
    }

    public List<String> getUnknownIssueKeys() {
        return unknownIssueKeys;
    }
}
