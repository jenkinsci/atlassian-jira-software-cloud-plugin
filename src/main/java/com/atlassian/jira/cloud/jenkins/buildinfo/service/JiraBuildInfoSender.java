package com.atlassian.jira.cloud.jenkins.buildinfo.service;

public interface JiraBuildInfoSender {

    JiraBuildInfoResponse sendBuildInfo(JiraBuildInfoRequest request);
}
