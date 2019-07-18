package com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * When a deployment is submitted via the API, the response includes the list of accepted
 * deployments (which will appear in the respective Jira issue view), rejected deployments (with
 * appropriated reason on why it was not accepted), unknown issue keys (which are not present in the
 * configured Jira Cloud site)
 */
public class DeploymentApiResponse {
    private final List<DeploymentKeyResponse> acceptedDeployments;
    private final List<RejectedDeploymentResponse> rejectedDeployments;
    private final List<String> unknownIssueKeys;

    @JsonCreator
    public DeploymentApiResponse(
            @JsonProperty("acceptedDeployments")
                    final List<DeploymentKeyResponse> acceptedDeployments,
            @JsonProperty("rejectedDeployments")
                    final List<RejectedDeploymentResponse> rejectedDeployments,
            @JsonProperty("unknownIssueKeys") final List<String> unknownIssueKeys) {
        this.acceptedDeployments = acceptedDeployments;
        this.rejectedDeployments = rejectedDeployments;
        this.unknownIssueKeys = unknownIssueKeys;
    }

    public List<DeploymentKeyResponse> getAcceptedDeployments() {
        return acceptedDeployments;
    }

    public List<RejectedDeploymentResponse> getRejectedDeployments() {
        return rejectedDeployments;
    }

    public List<String> getUnknownIssueKeys() {
        return unknownIssueKeys;
    }
}
