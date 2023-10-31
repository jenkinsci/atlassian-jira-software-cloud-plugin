package com.atlassian.jira.cloud.jenkins.featureflagservice;

public interface FeatureFlag<T> {
    String getFeatureKey();

    T getDefaultValue();
}
