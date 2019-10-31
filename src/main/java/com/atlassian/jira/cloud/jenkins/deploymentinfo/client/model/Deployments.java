package com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model;

import com.atlassian.jira.cloud.jenkins.common.client.JiraRequest;
import com.atlassian.jira.cloud.jenkins.common.client.model.Properties;
import com.atlassian.jira.cloud.jenkins.common.client.model.ProviderMetadata;

import java.util.Collections;
import java.util.List;

/** This represents the payload for the API request to submit list of deployments */
public class Deployments implements JiraRequest {
    private List<JiraDeploymentInfo> deployments;
    private Properties properties;
    private ProviderMetadata providerMetadata;

    public Deployments(final JiraDeploymentInfo jiraDeploymentInfo) {
        this.deployments = Collections.singletonList(jiraDeploymentInfo);
        this.properties = new Properties();
        this.providerMetadata = new ProviderMetadata();
    }

    public List<JiraDeploymentInfo> getDeployments() {
        return deployments;
    }

    public Properties getProperties() {
        return properties;
    }

    public ProviderMetadata getProviderMetadata() {
        return providerMetadata;
    }
}
