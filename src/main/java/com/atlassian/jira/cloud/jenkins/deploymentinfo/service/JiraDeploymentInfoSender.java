package com.atlassian.jira.cloud.jenkins.deploymentinfo.service;

import com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse;

import java.util.List;

public interface JiraDeploymentInfoSender {

    List<JiraSendInfoResponse> sendDeploymentInfo(JiraDeploymentInfoRequest request);
}
