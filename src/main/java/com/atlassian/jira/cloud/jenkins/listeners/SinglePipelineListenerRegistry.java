package com.atlassian.jira.cloud.jenkins.listeners;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

public class SinglePipelineListenerRegistry {

    private final Map<String, List<SinglePipelineListener>> buildUrlToSinglePipelineListeners;

    public void registerForBuild(String buildUrl, SinglePipelineListener listener) {
        List<SinglePipelineListener> existingListeners =
                buildUrlToSinglePipelineListeners.getOrDefault(buildUrl, new ArrayList<>());
        existingListeners.add(listener);
        buildUrlToSinglePipelineListeners.put(buildUrl, existingListeners);
    }

    public void unregister(String buildUrl) {
        buildUrlToSinglePipelineListeners.remove(buildUrl);
    }

    /**
     * Looks for earlier registered WorkflowRun using buildOrNodeUrl (nodeUrl is always a child path
     * to the buildUrl)
     */
    public Optional<List<SinglePipelineListener>> find(final String buildOrNodeUrl) {
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
}
