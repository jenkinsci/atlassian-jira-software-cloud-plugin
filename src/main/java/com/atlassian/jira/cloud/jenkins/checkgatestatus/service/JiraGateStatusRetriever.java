package com.atlassian.jira.cloud.jenkins.checkgatestatus.service;

public interface JiraGateStatusRetriever {

    JiraGateStatusResponse getGateState(GateStatusRequest request);
}
