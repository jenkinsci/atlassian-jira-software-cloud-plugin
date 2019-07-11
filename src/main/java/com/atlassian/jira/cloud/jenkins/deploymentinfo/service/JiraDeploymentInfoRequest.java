package com.atlassian.jira.cloud.jenkins.deploymentinfo.service;

import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import static java.util.Objects.requireNonNull;

public class JiraDeploymentInfoRequest {

    private final String site;
    private final String environment;
    private final String environmentType;
    private final WorkflowRun deployment;

    public JiraDeploymentInfoRequest(
            final String site,
            final String environment,
            final String environmentType,
            final WorkflowRun deployment) {
        this.site = requireNonNull(site);
        this.environment = environment;
        this.environmentType = environmentType;
        this.deployment = requireNonNull(deployment);
    }

    public String getSite() {
        return site;
    }

    public WorkflowRun getDeployment() {
        return deployment;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getEnvironmentType() {
        return environmentType;
    }
}
