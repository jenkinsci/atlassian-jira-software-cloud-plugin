package com.atlassian.jira.cloud.jenkins.ping;

import com.atlassian.jira.cloud.jenkins.common.client.JenkinsAppRequest;

import com.atlassian.jira.cloud.jenkins.config.JiraCloudPluginConfig;
// import jenkins.model.Jenkins;
// import hudson.model.Computer;

/** A request that we can send to the Jenkins app to check the connection. */
public class JenkinsAppPingRequest extends JenkinsAppRequest {
    private final String ipAddress;
    private final Boolean autoBuildsEnabled;
    private final String autoBuildRegex;

    public JenkinsAppPingRequest(final String ipAddress, final boolean bauto, final String sstuff) {
        super(RequestType.PING);

        // TODO - MOVE ALL THIS TO A NEW EVENT
        // TODO - CHECK CONFIG EXISTS AND SEND WITHOUTIT, MAYBE AN ERROR?
        // TODO - GET IP
        // TODO - TESTS

        this.ipAddress = ipAddress;
        this.autoBuildsEnabled = bauto;
        this.autoBuildRegex = sstuff;
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
}
