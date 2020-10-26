package com.atlassian.jira.cloud.jenkins.checkgatingstatus.client.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class GatingStatusResponse {
    private final String updatedTimestamp;
    private final GatingStatus gatingStatus;
    private final List<DetailKeyResponse> detailKeyResponse;
    private final String pipelineId;
    private final String environmentId;
    private final Integer deploymentSequenceNumber;

    @JsonCreator
    public GatingStatusResponse(
            @JsonProperty("updatedTimestamp") final String updatedTimestamp,
            @JsonProperty("gatingStatus") final GatingStatus gatingStatus,
            @JsonProperty("details") final List<DetailKeyResponse> detailKeyResponse,
            @JsonProperty("pipelineId") final String pipelineId,
            @JsonProperty("environmentId") final String environmentId,
            @JsonProperty("deploymentSequenceNumber") final Integer deploymentSequenceNumber) {
        this.updatedTimestamp = updatedTimestamp;
        this.gatingStatus = gatingStatus;
        this.detailKeyResponse = detailKeyResponse;
        this.pipelineId = pipelineId;
        this.environmentId = environmentId;
        this.deploymentSequenceNumber = deploymentSequenceNumber;
    }

    public GatingStatus getStatus() {
        return gatingStatus;
    }

    public List<DetailKeyResponse> getDetailKeyResponse() {
        return detailKeyResponse;
    }

    public String getUpdatedTimestamp() {
        return updatedTimestamp;
    }

    public GatingStatus getGatingStatus() {
        return gatingStatus;
    }

    public String getPipelineId() {
        return pipelineId;
    }

    public String getEnvironmentId() {
        return environmentId;
    }

    public Integer getDeploymentSequenceNumber() {
        return deploymentSequenceNumber;
    }
}
