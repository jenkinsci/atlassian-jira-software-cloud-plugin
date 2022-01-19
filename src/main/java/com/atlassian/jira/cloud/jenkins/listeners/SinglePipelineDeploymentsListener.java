package com.atlassian.jira.cloud.jenkins.listeners;

import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

public class SinglePipelineDeploymentsListener implements SinglePipelineListener {
    private final WorkflowRun build;
    private final boolean autoDeploymentsEnabled;
    private final String autoDeploymentsRegex;
    private final TaskListener taskListener;

    public SinglePipelineDeploymentsListener(
            final WorkflowRun run,
            final TaskListener taskListener,
            final boolean autoDeploymentsEnabled,
            final String autoDeploymentsRegex) {
        this.build = run;
        this.taskListener = taskListener;
        this.autoDeploymentsEnabled = autoDeploymentsEnabled;
        this.autoDeploymentsRegex = autoDeploymentsRegex;
    }

    public String getBuildUrl() {
        return this.build.getUrl();
    }

    public void onCompleted() {
        if (!autoDeploymentsEnabled) {
            return;
        }
        // TODO: implement me similarly to SinglePipelineBuildsListener
    }

    public void onNewHead(final FlowNode flowNode) {
        if (!autoDeploymentsEnabled) {
            return;
        }
        // TODO: implement me similarly to SinglePipelineBuildsListener
    }
}
