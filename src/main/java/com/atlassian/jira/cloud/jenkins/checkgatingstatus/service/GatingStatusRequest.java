package com.atlassian.jira.cloud.jenkins.checkgatingstatus.service;

import jline.internal.Nullable;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import static java.util.Objects.requireNonNull;

public class GatingStatusRequest {

    private final String site;
    private final String environmentId;
    private final WorkflowRun run;

    public GatingStatusRequest(
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
