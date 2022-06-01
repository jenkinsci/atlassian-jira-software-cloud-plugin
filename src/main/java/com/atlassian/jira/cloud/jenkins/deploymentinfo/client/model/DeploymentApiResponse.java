package com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * When a deployment is submitted via the API, the response includes the list of accepted
 * deployments (which will appear in the respective Jira issue view), rejected deployments (with
 * appropriated reason on why it was not accepted), unknown associations (which are not present in
 * the configured Jira Cloud site)
 */
public class DeploymentApiResponse {
    private final List<DeploymentKeyResponse> acceptedDeployments;
    private final List<RejectedDeploymentResponse> rejectedDeployments;
    private final List<Association> unknownAssociations;

    @JsonCreator
    public DeploymentApiResponse(
            @JsonProperty("acceptedDeployments")
            @Nullable final List<DeploymentKeyResponse> acceptedDeployments,
            @JsonProperty("rejectedDeployments")
            @Nullable final List<RejectedDeploymentResponse> rejectedDeployments,
            @Nullable
            @JsonProperty("unknownAssociations") final List<Association> unknownAssociations) {
        this.acceptedDeployments =
                Optional.ofNullable(acceptedDeployments).orElse(Collections.emptyList());
        this.rejectedDeployments =
                Optional.ofNullable(rejectedDeployments).orElse(Collections.emptyList());
        this.unknownAssociations =
                Optional.ofNullable(unknownAssociations).orElse(Collections.emptyList());
    }

    @Nullable
    public List<DeploymentKeyResponse> getAcceptedDeployments() {
        return acceptedDeployments;
    }

    @Nullable
    public List<RejectedDeploymentResponse> getRejectedDeployments() {
        return rejectedDeployments;
    }

    @Nullable
    public List<Association> getUnknownAssociations() {
        return unknownAssociations;
    }
}
