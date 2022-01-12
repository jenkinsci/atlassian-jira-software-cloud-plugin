package com.atlassian.jira.cloud.jenkins.buildinfo.service;

import com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse;

import java.util.List;

public interface JiraBuildInfoSender {

    List<JiraSendInfoResponse> sendBuildInfo(JiraBuildInfoRequest request);
}
