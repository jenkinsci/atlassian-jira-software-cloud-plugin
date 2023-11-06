package com.atlassian.jira.cloud.jenkins.pluginConfigApi;

import com.atlassian.jira.cloud.jenkins.common.client.JenkinsAppRequest;

/** A request that we can send to the Jenkins app to check the connection. */
public class JenkinsAppPluginConfigRequest extends JenkinsAppRequest {
    private final String ipAddress;
    private final Boolean autoBuildsEnabled;
    private final String autoBuildRegex;
    private final Boolean autoDeploymentsEnabled;
    private final String autoDeploymentsRegex;

    public JenkinsAppPluginConfigRequest(
            final String ipAddress,
            final boolean autoBuildsEnabled,
            final String autoBuildRegex,
            final boolean autoDeploymentsEnabled,
            final String autoDeploymentsRegex) {
        super(RequestType.PLUGIN_CONFIG);

        this.ipAddress = ipAddress;
        this.autoBuildsEnabled = autoBuildsEnabled;
        this.autoBuildRegex = autoBuildRegex;
        this.autoDeploymentsEnabled = autoDeploymentsEnabled;
        this.autoDeploymentsRegex = autoDeploymentsRegex;
    }

    public final String getIpAddress() {
        return ipAddress;
    }

    public final boolean isAutoBuildEnabled() {
        return autoBuildsEnabled;
    }

    public final String getAutoBuildRegex() {
        return autoBuildRegex;
    }

    public final boolean isAutoDeploymentsEnabled() {
        return autoDeploymentsEnabled;
    }

    public final String getAutoDeploymentsRegex() {
        return autoDeploymentsRegex;
    }
}
