package com.atlassian.jira.cloud.jenkins.buildinfo.service;

import static java.util.Objects.requireNonNull;

import javax.annotation.Nullable;

import hudson.model.AbstractBuild;

public class FreestyleBuildInfoRequest {
    private final String site;
    private final String branch;
    private final AbstractBuild<?, ?> build;

    public FreestyleBuildInfoRequest(@Nullable final String site, @Nullable final String branch,
            final AbstractBuild<?, ?> build) {
        this.site = site;
        this.branch = branch;
        this.build = requireNonNull(build);
    }

    @Nullable
    public String getSite() {
        return site;
    }

    @Nullable
    public String getBranch() {
        return branch;
    }

    public AbstractBuild<?, ?> getBuild() {
        return build;
    }
}
