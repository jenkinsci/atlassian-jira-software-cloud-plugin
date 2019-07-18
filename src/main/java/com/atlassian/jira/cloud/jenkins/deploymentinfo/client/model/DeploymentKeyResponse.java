package com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Unique combination of deployments (i.e pipeline ID, environment ID and deploymentSequenceNumber) that is used in representing both
 * accepted and rejected deployments
 */
public class DeploymentKeyResponse {

    private final String pipelineId;
    private final String environmentId;
    private final Integer deploymentSequenceNumber;

    @JsonCreator
    public DeploymentKeyResponse(
            @JsonProperty("pipelineId") final String pipelineId,
            @JsonProperty("environmentId") final String environmentId,
            @JsonProperty("deploymentSequenceNumber") final Integer deploymentSequenceNumber) {
        this.pipelineId = pipelineId;
        this.environmentId = environmentId;
        this.deploymentSequenceNumber = deploymentSequenceNumber;
    }

    public String getPipelineId() {
        return pipelineId;
    }

    public String getEnvironmentId() {
        return pipelineId;
    }

    public Integer getDeploymentSequenceNumber() {
        return deploymentSequenceNumber;
    }

    @Override
    public String toString() {
        return "DeploymentKeyResponse{"
                + "pipelineId='"
                + pipelineId
                + '\''
                + "environmentId='"
                + environmentId
                + '\''
                + ", deploymentSequenceNumber="
                + deploymentSequenceNumber
                + '}';
    }
}
