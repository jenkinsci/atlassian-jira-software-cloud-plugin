package com.atlassian.jira.cloud.jenkins.listeners;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class SinglePipelineListenerRegistry {

    private final Map<String, SinglePipelineListener[]> buildUrlToSinglePipelineListeners;

    /** @param listeners - must all share the same URL! */
    public void registerForBuild(final SinglePipelineListener... listeners) {
        verifySameUrl(listeners);
        buildUrlToSinglePipelineListeners.put(listeners[0].getBuildUrl(), listeners);
    }

    public void unregister(final SinglePipelineListener... listeners) {
        verifySameUrl(listeners);
        buildUrlToSinglePipelineListeners.remove(listeners[0].getBuildUrl());
    }

    /**
     * Looks for earlier registered WorkflowRun using buildOrNodeUrl (nodeUrl is always a child path
     * to the buildUrl)
     */
    public Optional<SinglePipelineListener[]> find(final String buildOrNodeUrl) {
        Optional<String> maybeRunUrl =
                buildUrlToSinglePipelineListeners
                        .keySet()
                        .stream()
                        .filter(buildOrNodeUrl::startsWith)
                        .findFirst();
        return maybeRunUrl.map(buildUrlToSinglePipelineListeners::get);
    }

    SinglePipelineListenerRegistry() {
        // Had to do this here because otherwise checkstyle complains about the empty ctor
        buildUrlToSinglePipelineListeners = new ConcurrentHashMap<>();
    }

    private static final SinglePipelineListenerRegistry instance =
            new SinglePipelineListenerRegistry();

    public static SinglePipelineListenerRegistry get() {
        return instance;
    }

    private void verifySameUrl(final SinglePipelineListener[] listeners) {
        if (Arrays.stream(listeners)
                        .map(SinglePipelineListener::getBuildUrl)
                        .collect(Collectors.toSet())
                        .size()
                > 1) {
            throw new IllegalArgumentException("All listeners must belong to the same build!");
        }
    }
}
