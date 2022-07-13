package com.atlassian.jira.cloud.jenkins.listeners;

import com.atlassian.jira.cloud.jenkins.common.service.IssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudPluginConfig;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.service.ChangeLogIssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.util.BranchNameIssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.util.CompoundIssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.util.PipelineLogger;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

@Extension
public class JenkinsPipelineRunListener extends RunListener<Run> {

    private static final Logger log = LoggerFactory.getLogger(JenkinsPipelineRunListener.class);
    private final SinglePipelineListenerRegistry singlePipelineListenerRegistry =
            SinglePipelineListenerRegistry.get();
    private final IssueKeyExtractor issueKeyExtractor;

    public JenkinsPipelineRunListener() {
        this.issueKeyExtractor =
                new CompoundIssueKeyExtractor(
                        new BranchNameIssueKeyExtractor(), new ChangeLogIssueKeyExtractor());
    }

    public JenkinsPipelineRunListener(final IssueKeyExtractor issueKeyExtractor) {
        this.issueKeyExtractor = issueKeyExtractor;
    }

    @Override
    public void onStarted(final Run r, final TaskListener taskListener) {
        if (!(r instanceof WorkflowRun)) {
            final String message =
                    "Not a WorkflowRun, automatic builds and deployments won't work.";
            log.warn(message);
            taskListener.getLogger().println("[WARN] " + message);
            return;
        }

        final JiraCloudPluginConfig config = JiraCloudPluginConfig.get();
        if (config == null) {
            final String message =
                    "Atlassian cloud plugin config is null. Please configure the plugin to support auto-detection of build and deployment events";
            log.warn(message);
            taskListener.getLogger().println("[WARN] " + message);
            return;
        }

        final WorkflowRun workflowRun = (WorkflowRun) r;

        if (config.getAutoBuildsEnabled()) {
            singlePipelineListenerRegistry.registerForBuild(
                    workflowRun.getUrl(),
                    new AutoBuildsListener(
                            workflowRun,
                            new PipelineLogger(taskListener.getLogger()),
                            config.getAutoBuildsRegex(),
                            this.issueKeyExtractor));
        }

        if (config.getAutoDeploymentsEnabled()) {
            singlePipelineListenerRegistry.registerForBuild(
                    workflowRun.getUrl(),
                    new AutoDeploymentsListener(
                            workflowRun,
                            new PipelineLogger(taskListener.getLogger()),
                            config.getAutoDeploymentsRegex(),
                            this.issueKeyExtractor));
        }
    }

    @Override
    public void onCompleted(final Run r, final TaskListener taskListener) {
        if (r instanceof WorkflowRun) {
            final WorkflowRun workflowRun = (WorkflowRun) r;
            singlePipelineListenerRegistry
                    .find(workflowRun.getUrl())
                    .map(
                            listeners -> {
                                listeners.forEach(SinglePipelineListener::onCompleted);
                                return true;
                            });
            singlePipelineListenerRegistry.unregister(workflowRun.getUrl());
        } else {
            final String message =
                    "Not a WorkflowRun, onCompleted won't be propagated to listeners";
            log.warn(message);
            taskListener.getLogger().println("[WARN] " + message);
        }
    }
}
