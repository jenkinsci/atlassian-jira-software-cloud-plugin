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

    public String getIpAddress() {
        return ipAddress;
    }

    public Boolean getAutoBuildEnabled() {
        return autoBuildsEnabled;
    }

    public String getAutoBuildRegex() {
        return autoBuildRegex;
    }

    public Boolean autoDeploymentsEnabled() {
        return autoDeploymentsEnabled;
    }

    public String autoDeploymentsRegex() {
        return autoDeploymentsRegex;
    }
}
