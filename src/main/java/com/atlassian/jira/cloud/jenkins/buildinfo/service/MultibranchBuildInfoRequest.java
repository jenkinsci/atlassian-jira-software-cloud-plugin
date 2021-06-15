package com.atlassian.jira.cloud.jenkins.buildinfo.service;

import org.jenkinsci.plugins.workflow.job.WorkflowRun;

public class MultibranchBuildInfoRequest extends JiraBuildInfoRequest{
    
    private final WorkflowRun build;

    public WorkflowRun getBuild() {
        return build;
    }

    public MultibranchBuildInfoRequest(final String site, final String branch, final WorkflowRun build) {
        super(site, branch);
        this.build = build;
    }

}
