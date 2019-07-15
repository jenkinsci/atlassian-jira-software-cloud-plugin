package com.atlassian.jira.cloud.jenkins.deploymentinfo.service;

import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.util.Set;

public interface ChangeLogExtractor {

    Set<String> extractIssueKeys(WorkflowRun workflowRun);

}
