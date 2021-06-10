package com.atlassian.jira.cloud.jenkins.buildinfo.service;

import static java.util.Objects.requireNonNull;

import javax.annotation.Nullable;

import hudson.model.AbstractBuild;

public class FreestyleBuildInfoRequest extends JiraBuildInfoRequest {
    private final AbstractBuild<?, ?> build;

    public FreestyleBuildInfoRequest(
            @Nullable final String site,
            @Nullable final String branch,
            final AbstractBuild<?, ?> build) {
        super(site, branch);
        this.build = requireNonNull(build);
    }

    public AbstractBuild<?, ?> getBuild() {
        return build;
    }
}
