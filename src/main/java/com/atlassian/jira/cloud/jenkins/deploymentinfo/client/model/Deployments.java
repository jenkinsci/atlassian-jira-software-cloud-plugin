package com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model;

import com.atlassian.jira.cloud.jenkins.common.client.JiraRequest;

import java.util.Collections;
import java.util.List;

/** This represents the payload for the API request to submit list of deployments */
public class Deployments implements JiraRequest {
    private List<JiraDeploymentInfo> deployments;

    public Deployments(final JiraDeploymentInfo jiraDeploymentInfo) {
        this.deployments = Collections.singletonList(jiraDeploymentInfo);
    }

    public List<JiraDeploymentInfo> getDeployments() {
        return deployments;
    }
}
