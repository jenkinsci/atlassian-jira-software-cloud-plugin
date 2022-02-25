package com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model;

import com.atlassian.jira.cloud.jenkins.common.client.JiraRequest;
import com.atlassian.jira.cloud.jenkins.common.client.model.Properties;
import com.atlassian.jira.cloud.jenkins.common.client.model.ProviderMetadata;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

/**
 * This represents the payload for the API request to submit list of deployments
 */
public class Deployments implements JiraRequest {
    @JsonIgnore
    private JiraDeploymentInfo deployment;
    private Properties properties;
    private ProviderMetadata providerMetadata;

    public Deployments(final JiraDeploymentInfo jiraDeploymentInfo) {
        this.deployment = jiraDeploymentInfo;
        this.properties = new Properties();
        this.providerMetadata = new ProviderMetadata();
    }

    public JiraDeploymentInfo getDeployment() {
        return deployment;
    }

    @JsonProperty("deployments")
    public List<JiraDeploymentInfo> getDeployments() {
        return Collections.singletonList(deployment);
    }

    public Properties getProperties() {
        return properties;
    }

    public ProviderMetadata getProviderMetadata() {
        return providerMetadata;
    }
}
