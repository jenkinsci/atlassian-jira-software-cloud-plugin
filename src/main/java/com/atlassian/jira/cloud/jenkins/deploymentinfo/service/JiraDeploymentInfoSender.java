package com.atlassian.jira.cloud.jenkins.deploymentinfo.service;

import com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse;
import com.atlassian.jira.cloud.jenkins.logging.PipelineLogger;

import java.util.List;

public interface JiraDeploymentInfoSender {

    List<JiraSendInfoResponse> sendDeploymentInfo(
            JiraDeploymentInfoRequest request, PipelineLogger pipelineLogger);
}
