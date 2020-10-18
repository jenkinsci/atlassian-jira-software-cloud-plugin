package com.atlassian.jira.cloud.jenkins.checkgatingstatus.service;

public interface JiraGatingStatusRetriever {

    JiraGatingStatusResponse getGatingState(GatingStatusRequest request);
}
