package com.atlassian.jira.cloud.jenkins.checkgatingstatus.service;

import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

public interface JiraGatingStatusRetriever {

    JiraGatingStatusResponse getGatingStatus(
            final TaskListener taskListener,
            final String jiraSite,
            final String environmentId,
            final WorkflowRun run);
}
