package com.atlassian.jira.cloud.jenkins.listeners;

import org.jenkinsci.plugins.workflow.graph.FlowNode;

public interface SinglePipelineListener {
    String getBuildUrl();

    void onCompleted();

    void onNewHead(final FlowNode flowNode);
}
