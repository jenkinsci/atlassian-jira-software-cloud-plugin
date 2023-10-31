package com.atlassian.jira.cloud.jenkins.featureflagservice;

import com.launchdarkly.sdk.LDUser;
import com.launchdarkly.sdk.LDValue;
import com.launchdarkly.sdk.server.interfaces.LDClientInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

@Service
public class FeatureFlagService {
    private final LDClientInterface ldClient;
    private final String launchDarklyAppName;
    private final String launchDarklyApiKey;

    @Autowired
    public FeatureFlagService(
            final LDClientInterface ldClient,
            final String launchDarklyAppName,
            final String launchDarklyApiKey) {
        this.ldClient = ldClient;
        this.launchDarklyAppName = launchDarklyAppName;
        this.launchDarklyApiKey = launchDarklyApiKey;
    }

    public boolean getBooleanValue(final String featureFlagKey, final String projectKey) {
        LDUser ldUser = new LDUser.Builder(projectKey).build();
        return ldClient.boolVariation(featureFlagKey, ldUser, false);
    }

    public Set<String> getSetOfStrings(final String featureFlagKey, final String projectKey) {
        LDUser ldUser = new LDUser.Builder(projectKey).build();
        LDValue defaultValue = getDefaultLDValue(Collections.emptySet());
        LDValue actualValue = ldClient.jsonValueVariation(featureFlagKey, ldUser, defaultValue);

        Spliterator<LDValue> spliterator =
                Spliterators.spliteratorUnknownSize(
                        actualValue.values().iterator(), Spliterator.ORDERED);
        return StreamSupport.stream(spliterator, false)
                .map(LDValue::stringValue)
                .collect(Collectors.toSet());
    }

    private LDValue getDefaultLDValue(final Set<String> defaultValue) {
        String jsonArray =
                "["
                        + defaultValue
                                .stream()
                                .map(item -> "\"" + item + "\"")
                                .collect(Collectors.joining(","))
                        + "]";
        return LDValue.parse(jsonArray);
    }
}
