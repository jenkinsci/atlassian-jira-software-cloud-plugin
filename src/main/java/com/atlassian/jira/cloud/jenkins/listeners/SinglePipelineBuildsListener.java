package com.atlassian.jira.cloud.jenkins.listeners;

import com.atlassian.jira.cloud.jenkins.buildinfo.service.MultibranchBuildInfoRequest;
import com.atlassian.jira.cloud.jenkins.common.factory.JiraSenderFactory;
import com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse;
import com.atlassian.jira.cloud.jenkins.common.service.IssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.service.ChangeLogIssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.util.BranchNameIssueKeyExtractor;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SinglePipelineBuildsListener implements SinglePipelineListener {
    private final WorkflowRun build;
    private final boolean autoBuildsEnabled;
    private final String autoBuildsRegex;

    private final IssueKeyExtractor[] issueKeyExtractors;
    private final TaskListener taskListener;

    private boolean inProgressSent = false;

    private String endFlowNodeId = "";

    private static final Logger log = LoggerFactory.getLogger(SinglePipelineBuildsListener.class);

    public SinglePipelineBuildsListener(
            final WorkflowRun run,
            final TaskListener taskListener,
            final boolean autoBuildsEnabled,
            final String autoBuildsRegex) {
        this.build = run;
        this.taskListener = taskListener;
        this.autoBuildsEnabled = autoBuildsEnabled;
        this.autoBuildsRegex = autoBuildsRegex;
        issueKeyExtractors =
                new IssueKeyExtractor[] {
                    new BranchNameIssueKeyExtractor(), new ChangeLogIssueKeyExtractor()
                };
    }

    public String getBuildUrl() {
        return this.build.getUrl();
    }

    public void onCompleted() {
        if (!autoBuildsEnabled) {
            return;
        }
        if (autoBuildsRegex.trim().isEmpty()) {
            sendBuildStatusToJira();
        }
    }

    public void onNewHead(final FlowNode flowNode) {
        if (!autoBuildsEnabled) {
            return;
        }
        if (autoBuildsRegex.trim().isEmpty()) {
            if (!inProgressSent) {
                // We don't have issueKeys at the start of the execution of the pipeline, hence need
                // to check
                // periodically
                if (extractBuildKeys().size() > 0) {
                    inProgressSent = true;
                    sendBuildStatusToJira();
                }
            }
        }
        new StringBuilder();
        //        final StringBuilder debug = new StringBuilder();
        //        if (flowNode instanceof StepEndNode) {
        //            final String displayName = ((StepEndNode)
        // flowNode).getStartNode().getDisplayName();
        //            final BallColor iconColor = flowNode.getIconColor();
        //            String url;
        //            try {
        //                url = ((StepEndNode) flowNode).getStartNode().getUrl();
        //            } catch (IOException e) {
        //                e.printStackTrace();
        //            }
        //
        //            debug.append(displayName).append("
        // =").append(iconColor.toString()).append("\n");
        //        }
        //        new StringBuilder();
    }

    private void sendBuildStatusToJira() {
        final List<JiraSendInfoResponse> allResponses =
                JiraSenderFactory.getInstance()
                        .getJiraBuildInfoSender()
                        .sendBuildInfo(new MultibranchBuildInfoRequest(null, "", build));
        allResponses.forEach(
                response -> {
                    final String message = response.getStatus() + ": " + response.getMessage();
                    if (response.getStatus().isFailure) {
                        log.warn(message);
                        taskListener.getLogger().println("[WARN] " + message);
                    } else {
                        log.info(message);
                        taskListener.getLogger().println("[INFO] " + message);
                    }
                });
    }

    private Set<String> extractBuildKeys() {
        return Arrays.stream(issueKeyExtractors)
                .flatMap(issueKeyExtractor -> issueKeyExtractor.extractIssueKeys(build).stream())
                .collect(Collectors.toSet());
    }
}
