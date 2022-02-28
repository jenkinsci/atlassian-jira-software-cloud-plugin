package com.atlassian.jira.cloud.jenkins.buildinfo.service;

import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.State;

import javax.annotation.Nullable;

public abstract class JiraBuildInfoRequest {

    private final String site; // NULL means "send to all connected sites"

    private final String branch;

    protected State jiraState;

    public JiraBuildInfoRequest(@Nullable final String site, @Nullable final String branch) {
        this.site = site;
        this.branch = branch;
    }

    @Nullable
    public String getSite() {
        return site;
    }

    @Nullable
    public String getBranch() {
        return branch;
    }

    public State getJiraState() {
        return this.jiraState;
    }
}
