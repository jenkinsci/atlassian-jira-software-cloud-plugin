package com.atlassian.jira.cloud.jenkins.featureflagservice;

import lombok.Getter;

import java.util.Objects;

@Getter
public enum BooleanFeatureFlag {

    // Permanent feature flags
    TEST_FLAG("test-flag", false);
    private final String featureKey;
    private final Boolean defaultValue;

    BooleanFeatureFlag(final String featureKey, final boolean defaultValue) {
        this.featureKey = featureKey;
        this.defaultValue = defaultValue;
    }
}
