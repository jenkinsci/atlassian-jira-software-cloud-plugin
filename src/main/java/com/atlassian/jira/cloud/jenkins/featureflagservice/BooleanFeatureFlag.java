package com.atlassian.jira.cloud.jenkins.featureflagservice;

import lombok.Getter;

import java.util.Objects;

@Getter
public enum BooleanFeatureFlag {
    BACKEND_TEST("backend-test", false);
    private final String featureKey;
    private final Boolean defaultValue;

    BooleanFeatureFlag(final String featureKey, final boolean defaultValue) {
        this.featureKey = featureKey;
        this.defaultValue = defaultValue;
    }
}
