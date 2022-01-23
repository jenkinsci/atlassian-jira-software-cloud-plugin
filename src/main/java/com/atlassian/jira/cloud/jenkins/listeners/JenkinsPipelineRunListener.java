package com.atlassian.jira.cloud.jenkins.listeners;

import com.atlassian.jira.cloud.jenkins.config.JiraCloudPluginConfig;
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

    @Override
    public void onStarted(final Run r, final TaskListener taskListener) {
        if (r instanceof WorkflowRun) {
            final WorkflowRun workflowRun = (WorkflowRun) r;
            final JiraCloudPluginConfig config = JiraCloudPluginConfig.get();
            singlePipelineListenerRegistry.registerForBuild(
                    new SinglePipelineBuildsListener(
                            workflowRun,
                            taskListener,
                            config.getAutoBuildsEnabled(),
                            config.getAutoBuildsRegex()),
                    new SinglePipelineDeploymentsListener(
                            workflowRun,
                            taskListener,
                            config.getAutoDeploymentsEnabled(),
                            config.getAutoDeploymentsRegex()));
        } else {
            final String message =
                    "Not a WorkflowRun, automatic builds and deployments won't work.";
            log.warn(message);
            taskListener.getLogger().println("[WARN] " + message);
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
                                Arrays.stream(listeners)
                                        .forEach(SinglePipelineListener::onCompleted);
                                singlePipelineListenerRegistry.unregister(listeners);
                                return true;
                            })
                    .orElseGet(
                            () -> {
                                final String message =
                                        "Could not find listeners for " + workflowRun.getUrl();
                                log.warn(message);
                                taskListener.getLogger().println("[WARN] " + message);
                                return false;
                            });
        } else {
            final String message =
                    "Not a WorkflowRun, onCompleted won't be propagated to listeners";
            log.warn(message);
            taskListener.getLogger().println("[WARN] " + message);
        }
    }
}
