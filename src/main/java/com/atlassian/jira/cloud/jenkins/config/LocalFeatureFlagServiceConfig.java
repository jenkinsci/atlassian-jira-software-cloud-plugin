package com.atlassian.jira.cloud.jenkins.config;

import com.atlassian.jira.cloud.jenkins.featureflagservice.BooleanFeatureFlag;
import com.atlassian.jira.cloud.jenkins.featureflagservice.SetFeatureFlag;
import com.launchdarkly.sdk.LDValue;
import com.launchdarkly.sdk.server.LDClient;
import com.launchdarkly.sdk.server.LDConfig;
import com.launchdarkly.sdk.server.integrations.TestData;
import com.launchdarkly.sdk.server.interfaces.LDClientInterface;
import lombok.Setter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;

@Configuration
@Setter
public class LocalFeatureFlagServiceConfig {
    @Bean
    public TestData testData(final FeatureFlagProperties properties) {
        TestData testData = TestData.dataSource();
        properties
                .getLocalSetting()
                .getInitialBooleanFeatureFlags()
                .forEach(
                        (key, value) -> {
                            if (!isSupported(key)) {
                                throw new UnsupportedOperationException(
                                        String.format("Feature flag %s is not defined", key));
                            }
                            testData.update(
                                    testData.flag(key).booleanFlag().variationForAllUsers(value));
                        });

        properties
                .getLocalSetting()
                .getInitialSetFeatureFlags()
                .forEach(
                        (key, value) -> {
                            if (!SetFeatureFlag.isSupported(key)) {
                                throw new UnsupportedOperationException(
                                        String.format("Feature flag %s is not defined", key));
                            }
                            var arrayBuilder = LDValue.buildArray();
                            Set<String> defaultValue =
                                    value == null
                                            ? Collections.emptySet()
                                            : Collections.unmodifiableSet(new HashSet<>(value));
                            defaultValue.forEach(arrayBuilder::add);
                            testData.update(
                                    testData.flag(key).valueForAllUsers(arrayBuilder.build()));
                        });

        return testData;
    }

    public static boolean isSupported(final String featureKey) {
        for (BooleanFeatureFlag booleanFeatureFlag : BooleanFeatureFlag.values()) {
            if (Objects.equals(booleanFeatureFlag.getFeatureKey(), featureKey)) {
                return true;
            }
        }
        return false;
    }

    @Bean("ldClient")
    public LDClientInterface localLDClient(final TestData testData) {
        LDConfig ldConfig = new LDConfig.Builder().dataSource(testData).build();

        return new LDClient("dummy", ldConfig);
    }
}
