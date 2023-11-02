package com.atlassian.jira.cloud.jenkins.config;

import com.atlassian.jira.cloud.jenkins.featureflagservice.SetFeatureFlag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
@Data
@RequiredArgsConstructor
public class FeatureFlagProperties {
    private String apiKey;
    private LocalSetting localSetting;

    @Data
    @RequiredArgsConstructor
    public static class LocalSetting {
        private Map<String, Boolean> initialBooleanFeatureFlags;
        private Map<String, Set<String>> initialSetFeatureFlags;
    }
}
