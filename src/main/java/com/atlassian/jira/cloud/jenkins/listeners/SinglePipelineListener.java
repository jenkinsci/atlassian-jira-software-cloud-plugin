package com.atlassian.jira.cloud.jenkins.listeners;

import org.jenkinsci.plugins.workflow.graph.FlowNode;

/**
 * "Single" means that a single instance of a class that implements this interface will be handling
 * events that belong only to a single pipeline.
 *
 * <p>This is done to handle a situation when the Jenkins server runs multiple pipelines at the same
 * time
 */
public interface SinglePipelineListener {
    String getBuildUrl();

    void onCompleted();

    void onNewHead(final FlowNode flowNode);
}
