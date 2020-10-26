package com.atlassian.jira.cloud.jenkins.checkgatingstatus.service;

public interface JiraGatingStatusRetriever {

    JiraGatingStatusResponse getGatingStatus(GatingStatusRequest request);
}
