package com.atlassian.jira.cloud.jenkins.buildinfo.service;

import com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse;

public interface FreestyleBuildInfoSender {

    JiraSendInfoResponse sendBuildInfo(FreestyleBuildInfoRequest request);
}
