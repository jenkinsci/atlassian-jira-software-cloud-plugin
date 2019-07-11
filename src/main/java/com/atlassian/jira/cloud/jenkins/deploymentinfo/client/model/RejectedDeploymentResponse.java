package com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model;

import com.atlassian.jira.cloud.jenkins.common.model.ApiErrorResponse;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Any deployments that were rejected when submitting to the API. Includes the key of the deployment and the
 * reasons for rejection
 */
public class RejectedDeploymentResponse {
    private DeploymentKeyResponse key;
    private List<ApiErrorResponse> errors;

    @JsonCreator
    public RejectedDeploymentResponse(
            @JsonProperty("key") final DeploymentKeyResponse key,
            @JsonProperty("errors") final List<ApiErrorResponse> errors) {
        this.key = requireNonNull(key);
        this.errors = ImmutableList.copyOf(errors);
    }

    public DeploymentKeyResponse getKey() {
        return key;
    }

    public List<ApiErrorResponse> getErrors() {
        return errors;
    }

    @Override
    public String toString() {
        return "RejectedDeploymentResponse{" + "key=" + key + ", errors=" + errors + '}';
    }
}
