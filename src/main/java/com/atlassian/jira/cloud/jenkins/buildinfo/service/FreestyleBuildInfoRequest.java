package com.atlassian.jira.cloud.jenkins.buildinfo.service;

import static java.util.Objects.requireNonNull;

import javax.annotation.Nullable;

import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.State;
import com.atlassian.jira.cloud.jenkins.util.JenkinsToJiraStatus;
import hudson.model.AbstractBuild;

public class FreestyleBuildInfoRequest extends JiraBuildInfoRequest {
    private final AbstractBuild<?, ?> build;

    public FreestyleBuildInfoRequest(
            @Nullable final String site,
            @Nullable final String branch,
            final AbstractBuild<?, ?> build) {
        super(site, branch);
        this.build = requireNonNull(build);
        this.jiraState = JenkinsToJiraStatus.getState(build.getResult());
    }

    public AbstractBuild<?, ?> getBuild() {
        return build;
    }
}
