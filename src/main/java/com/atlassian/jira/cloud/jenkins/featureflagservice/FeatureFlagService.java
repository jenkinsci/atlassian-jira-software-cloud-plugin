package com.atlassian.jira.cloud.jenkins.featureflagservice;

import com.launchdarkly.sdk.LDUser;
import com.launchdarkly.sdk.server.LDClient;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class FeatureFlagService {
    private static final String LAUNCH_DARKLY_SDK_KEY = "HOW-TO-DEFINE-ONE-SDK-KEY-ACROSS-ALL-PLUGIN-INSTALLATIONS?";
    private static final String PROJECT_KEY = "jenkins-for-jira";
    private final LDClient ldClient;

    public FeatureFlagService() {
        this.ldClient = new LDClient(LAUNCH_DARKLY_SDK_KEY);
    }

    public boolean getBooleanValue(final String featureFlagKey) {
        LDUser ldUser = new LDUser.Builder(PROJECT_KEY).build();
        return ldClient.boolVariation(featureFlagKey, ldUser, false);
    }
}
