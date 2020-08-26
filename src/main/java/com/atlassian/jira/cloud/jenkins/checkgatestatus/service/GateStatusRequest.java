package com.atlassian.jira.cloud.jenkins.checkgatestatus.service;

import jline.internal.Nullable;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import static java.util.Objects.requireNonNull;

public class GateStatusRequest {

    private final String site;
    private final String environmentId;
    private final WorkflowRun run;

    public GateStatusRequest(
            @Nullable final String site, final String environmentId, final WorkflowRun run) {
        this.site = site;
        this.environmentId = requireNonNull(environmentId);
        this.run = requireNonNull(run);
    }

    @Nullable
    public String getSite() {
        return site;
    }

    public String getEnvironmentId() {
        return environmentId;
    }

    public WorkflowRun getRun() {
        return run;
    }
}
