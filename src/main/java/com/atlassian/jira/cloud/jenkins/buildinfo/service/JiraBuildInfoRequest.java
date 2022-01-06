package com.atlassian.jira.cloud.jenkins.buildinfo.service;

import javax.annotation.Nullable;

public abstract class JiraBuildInfoRequest {

    private final String site; // NULL means "send to all connected sites"

    private final String branch;
    // private final WorkflowRun build;

    public JiraBuildInfoRequest(@Nullable final String site, @Nullable final String branch) {
        this.site = site;
        this.branch = branch;
        // this.build = requireNonNull(build);
    }

    @Nullable
    public String getSite() {
        return site;
    }

    @Nullable
    public String getBranch() {
        return branch;
    }

    /*public WorkflowRun getBuild() {
        return build;
    }*/
}
