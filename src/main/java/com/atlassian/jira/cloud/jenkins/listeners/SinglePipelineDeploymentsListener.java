package com.atlassian.jira.cloud.jenkins.listeners;

import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.cps.nodes.StepStartNode;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SinglePipelineDeploymentsListener implements SinglePipelineListener {
    private final WorkflowRun build;
    private final boolean autoDeploymentsEnabled;
    private final String autoDeploymentsRegex;
    private final List<SinglePipelineSingleDeploymentListener> deploymentListeners =
            new LinkedList<>();
    private final PrintStream pipelineLogger;

    private static final Logger systemLogger =
            LoggerFactory.getLogger(SinglePipelineDeploymentsListener.class);

    public SinglePipelineDeploymentsListener(
            final WorkflowRun run,
            final TaskListener taskListener,
            final boolean autoDeploymentsEnabled,
            final String autoDeploymentsRegex) {
        this.build = run;
        this.autoDeploymentsEnabled = autoDeploymentsEnabled;
        this.autoDeploymentsRegex = autoDeploymentsRegex;

        this.pipelineLogger = taskListener.getLogger();
    }

    public String getBuildUrl() {
        return this.build.getUrl();
    }

    public void onCompleted() {
        if (!autoDeploymentsEnabled) {
            return;
        }
        deploymentListeners.forEach(SinglePipelineSingleDeploymentListener::onCompleted);
    }

    public void onNewHead(final FlowNode flowNode) {
        if (!autoDeploymentsEnabled) {
            return;
        }

        if (flowNode instanceof StepStartNode) {
            try {
                final Matcher matcher =
                        Pattern.compile(autoDeploymentsRegex).matcher(flowNode.getDisplayName());
                if (matcher.matches()) {
                    final String envName = matcher.group("envName");
                    pipelineLogger.println(
                            "[INFO] deployment node was determined, envName=" + envName);
                    deploymentListeners.add(
                            new SinglePipelineSingleDeploymentListener(
                                    build, pipelineLogger, flowNode.getId(), envName));
                }
            } catch (final IllegalArgumentException ex) {
                final String message = ex.getClass().getSimpleName() + ": " + ex.getMessage();
                pipelineLogger.println("[WARN] " + message);
                systemLogger.warn(message, ex);
            }
        }

        deploymentListeners.forEach(listener -> listener.onNewHead(flowNode));
    }
}
