package com.atlassian.jira.cloud.jenkins.deploymentinfo.service;

import com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse;

public interface JiraDeploymentInfoSender {

    JiraSendInfoResponse sendDeploymentInfo(JiraDeploymentInfoRequest request);
}
