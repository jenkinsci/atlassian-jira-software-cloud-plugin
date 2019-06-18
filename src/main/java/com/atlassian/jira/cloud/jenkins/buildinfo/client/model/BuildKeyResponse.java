package com.atlassian.jira.cloud.jenkins.buildinfo.client.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Unique combination of builds (i.e pipeline ID and build number) that is used in representing both
 * accepted and rejected builds
 */
public class BuildKeyResponse {

    private final String pipelineId;
    private final Integer buildNumber;

    @JsonCreator
    public BuildKeyResponse(
            @JsonProperty("pipelineId") final String pipelineId,
            @JsonProperty("buildNumber") final Integer buildNumber) {
        this.pipelineId = pipelineId;
        this.buildNumber = buildNumber;
    }

    public String getPipelineId() {
        return pipelineId;
    }

    public Integer getBuildNumber() {
        return buildNumber;
    }

    @Override
    public String toString() {
        return "BuildKeyResponse{"
                + "pipelineId='"
                + pipelineId
                + '\''
                + ", buildNumber="
                + buildNumber
                + '}';
    }
}
