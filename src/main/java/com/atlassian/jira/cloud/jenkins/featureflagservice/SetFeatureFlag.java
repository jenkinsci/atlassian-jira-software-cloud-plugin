package com.atlassian.jira.cloud.jenkins.featureflagservice;

import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

@Getter
public enum SetFeatureFlag implements FeatureFlag<Set<String>> {
    BACKEND_TEST("backend-test", Collections.emptySet());

    private final String featureKey;
    private final Set<String> defaultValue;

    SetFeatureFlag(final String featureKey, final Set<String> defaultValue) {
        this.featureKey = featureKey;
        this.defaultValue = defaultValue;
    }

    public static Set<String> getValues(final String featureKey) {
        for (SetFeatureFlag setFeatureFlag : SetFeatureFlag.values()) {
            if (Objects.equals(setFeatureFlag.getFeatureKey(), featureKey)) {
                return setFeatureFlag.getDefaultValue();
            }
        }
        return Collections.emptySet();
    }

    public static boolean isSupported(final String featureKey) {
        for (SetFeatureFlag setFeatureFlag : SetFeatureFlag.values()) {
            if (Objects.equals(setFeatureFlag.getFeatureKey(), featureKey)) {
                return true;
            }
        }
        return false;
    }
}
