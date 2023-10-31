package com.atlassian.jira.cloud.jenkins.config;

import com.atlassian.jira.cloud.jenkins.properties.FeatureFlagProperties;
import com.launchdarkly.sdk.server.LDClient;
import com.launchdarkly.sdk.server.interfaces.LDClientInterface;
import lombok.Setter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Setter
public class FeatureFlagServiceConfig {
    @Bean("ldClient")
    public LDClientInterface ldClient(final FeatureFlagProperties properties) {
        return new LDClient(properties.getApiKey());
    }
}
