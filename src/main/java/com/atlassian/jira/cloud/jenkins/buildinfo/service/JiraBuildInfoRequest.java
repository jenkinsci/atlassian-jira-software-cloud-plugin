package com.atlassian.jira.cloud.jenkins.buildinfo.service;

import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

public class JiraBuildInfoRequest {

    private final String site;
    private final WorkflowRun build;

    public JiraBuildInfoRequest(@Nullable final String site, final WorkflowRun build) {
        this.site = site;
        this.build = requireNonNull(build);
    }

    @Nullable
    public String getSite() {
        return site;
    }

    public WorkflowRun getBuild() {
        return build;
    }
}
