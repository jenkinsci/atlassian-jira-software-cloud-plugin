package com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model;

import java.util.Collections;
import java.util.List;

/** This represents the payload for the API request to submit list of deployments */
public class Deployments {
    private List<JiraDeploymentInfo> deployments;

    public Deployments(final JiraDeploymentInfo jiraDeploymentInfo) {
        this.deployments = Collections.singletonList(jiraDeploymentInfo);
    }

    public List<JiraDeploymentInfo> getDeployments() {
        return deployments;
    }
}
