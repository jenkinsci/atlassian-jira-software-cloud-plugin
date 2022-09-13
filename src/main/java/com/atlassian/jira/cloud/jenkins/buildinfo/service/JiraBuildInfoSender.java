package com.atlassian.jira.cloud.jenkins.buildinfo.service;

import com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse;
import com.atlassian.jira.cloud.jenkins.logging.PipelineLogger;

import java.util.List;

public interface JiraBuildInfoSender {

    List<JiraSendInfoResponse> sendBuildInfo(
            JiraBuildInfoRequest request, PipelineLogger pipelineLogger);
}
