package com.atlassian.jira.cloud.jenkins.deploymentinfo.service;

import java.util.Set;

import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

public class JiraDeploymentInfoRequest {

    private final String site;
    private final String environmentId;
    private final String environmentName;
    private final String environmentType;
    private final String state;
    private final Set<String> serviceIds;
    private final WorkflowRun deployment;

    public JiraDeploymentInfoRequest(
            @Nullable final String site,
            final String environmentId,
            final String environmentName,
            final String environmentType,
            @Nullable final String state,
            final Set<String> serviceId,
            final WorkflowRun deployment) {
        this.site = site;
        this.environmentId = environmentId;
        this.environmentName = environmentName;
        this.environmentType = environmentType;
        this.state = state;
        this.serviceIds = serviceId;
        this.deployment = requireNonNull(deployment);
    }

    @Nullable
    public String getSite() {
        return site;
    }

    public WorkflowRun getDeployment() {
        return deployment;
    }

    public String getEnvironmentId() {
        return environmentId;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    public String getEnvironmentType() {
        return environmentType;
    }

    public Set<String> getServiceIds() {
        return serviceIds;
    }

    @Nullable
    public String getState() {
        return state;
    }
}
