package com.atlassian.jira.cloud.jenkins.listeners;

import com.atlassian.jira.cloud.jenkins.common.service.IssueKeyExtractor;
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

public class AutoDeploymentsListener implements SinglePipelineListener {
    private final WorkflowRun build;
    private final String autoDeploymentsRegex;
    private final List<SinglePipelineSingleDeploymentListener> deploymentListeners =
            new LinkedList<>();
    private final PrintStream pipelineLogger;
    private final IssueKeyExtractor issueKeyExtractor;

    private static final Logger systemLogger =
            LoggerFactory.getLogger(AutoDeploymentsListener.class);

    public AutoDeploymentsListener(
            final WorkflowRun run,
            final PrintStream logger,
            final String autoDeploymentsRegex,
            final IssueKeyExtractor issueKeyExtractor) {
        this.build = run;
        this.autoDeploymentsRegex = autoDeploymentsRegex;
        this.pipelineLogger = logger;
        this.issueKeyExtractor = issueKeyExtractor;
    }

    public String getBuildUrl() {
        return this.build.getUrl();
    }

    public void onCompleted() {
        deploymentListeners.forEach(SinglePipelineSingleDeploymentListener::onCompleted);
    }

    public void onNewHead(final FlowNode flowNode) {

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
                                    build,
                                    pipelineLogger,
                                    flowNode.getId(),
                                    envName,
                                    issueKeyExtractor));
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
