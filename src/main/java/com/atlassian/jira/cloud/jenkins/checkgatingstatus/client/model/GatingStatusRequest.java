package com.atlassian.jira.cloud.jenkins.checkgatingstatus.client.model;

import com.atlassian.jira.cloud.jenkins.common.client.JenkinsAppRequest;

public class GatingStatusRequest extends JenkinsAppRequest {

    private final String cloudId;
    private final String deploymentId;
    private final String pipelineId;
    private final String environmentId;

    public GatingStatusRequest(
            final String cloudId,
            final String deploymentId,
            final String pipelineId,
            final String environmentId) {
        super(JenkinsAppRequest.RequestType.GATING_STATUS);
        this.cloudId = cloudId;
        this.deploymentId = deploymentId;
        this.pipelineId = pipelineId;
        this.environmentId = environmentId;
    }

    public String getCloudId() {
        return cloudId;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public String getPipelineId() {
        return pipelineId;
    }

    public String getEnvironmentId() {
        return environmentId;
    }
}
