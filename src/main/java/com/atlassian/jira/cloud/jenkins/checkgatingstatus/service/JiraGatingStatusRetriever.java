package com.atlassian.jira.cloud.jenkins.checkgatingstatus.service;

import org.jenkinsci.plugins.workflow.job.WorkflowRun;

public interface JiraGatingStatusRetriever {

    JiraGatingStatusResponse getGatingStatus(
            final String jiraSite, final String environmentId, final WorkflowRun run);
}
