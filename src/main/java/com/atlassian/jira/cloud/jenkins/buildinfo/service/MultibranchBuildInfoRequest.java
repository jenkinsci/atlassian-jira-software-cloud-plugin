package com.atlassian.jira.cloud.jenkins.buildinfo.service;

import com.atlassian.jira.cloud.jenkins.util.JenkinsToJiraStatus;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import javax.annotation.Nullable;
import java.util.Optional;

public class MultibranchBuildInfoRequest extends JiraBuildInfoRequest {

    private final WorkflowRun build;
    private final Optional<FlowNode>
            statusFlowNode; // when provided, the status should be extracted from this node
    // rather than from WorkflowRun::getResult()

    public WorkflowRun getBuild() {
        return build;
    }

    public Optional<FlowNode> getStatusFlowNode() {
        return statusFlowNode;
    }

    public MultibranchBuildInfoRequest(
            @Nullable final String site,
            final String branch,
            final WorkflowRun build,
            final Optional<FlowNode> statusFlowNode) {
        super(site, branch);
        this.build = build;
        this.statusFlowNode = statusFlowNode;
        this.jiraState =
                this.statusFlowNode
                        .map(JenkinsToJiraStatus::getState)
                        .orElseGet(() -> JenkinsToJiraStatus.getState(build.getResult()));
    }
}
