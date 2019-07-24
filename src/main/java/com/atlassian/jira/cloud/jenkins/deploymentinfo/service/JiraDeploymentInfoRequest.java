package com.atlassian.jira.cloud.jenkins.deploymentinfo.service;

import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

public class JiraDeploymentInfoRequest {

    private final String site;
    private final String environmentId;
    private final String environmentName;
    private final String environmentType;
    private final WorkflowRun deployment;

    public JiraDeploymentInfoRequest(
            @Nullable final String site,
            final String environmentId,
            final String environmentName,
            final String environmentType,
            final WorkflowRun deployment) {
        this.site = site;
        this.environmentId = environmentId;
        this.environmentName = environmentName;
        this.environmentType = environmentType;
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
}
