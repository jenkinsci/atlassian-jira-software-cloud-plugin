package com.atlassian.jira.cloud.jenkins.buildinfo.service;

import javax.annotation.Nullable;

public abstract class JiraBuildInfoRequest {

    private final String site;
    private final String branch;
    // private final WorkflowRun build;

    /**
     * @param site - when NULL, the build info will be sent to all connected Jiras
     * @param branch
     */
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
