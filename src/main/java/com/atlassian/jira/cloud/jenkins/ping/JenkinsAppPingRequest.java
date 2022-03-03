package com.atlassian.jira.cloud.jenkins.ping;

import com.atlassian.jira.cloud.jenkins.common.client.JenkinsAppRequest;

/** A request that we can send to the Jenkins app to check the connection. */
public class JenkinsAppPingRequest extends JenkinsAppRequest {

    public JenkinsAppPingRequest() {
        super(RequestType.PING);
    }
}
