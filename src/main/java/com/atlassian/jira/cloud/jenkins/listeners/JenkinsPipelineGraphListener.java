package com.atlassian.jira.cloud.jenkins.listeners;

import hudson.Extension;
import org.jenkinsci.plugins.workflow.flow.GraphListener;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;

@Extension
public class JenkinsPipelineGraphListener implements GraphListener {

    private static final Logger log = LoggerFactory.getLogger(JenkinsPipelineGraphListener.class);

    private final SinglePipelineListenerRegistry singlePipelineListenerRegistry =
            SinglePipelineListenerRegistry.get();

    @Override
    public void onNewHead(final FlowNode flowNode) {
        final String nodeUrl;

        try {
            nodeUrl = flowNode.getUrl();
        } catch (final IOException e) {
            log.error("Cannot get URL of a node", e);
            return;
        }

        singlePipelineListenerRegistry
                .find(nodeUrl)
                .map(
                        (listeners) -> {
                            listeners.forEach(listener -> listener.onNewHead(flowNode));
                            return true;
                        });
    }
}
