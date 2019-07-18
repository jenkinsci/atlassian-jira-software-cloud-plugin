package com.atlassian.jira.cloud.jenkins.common.service;

import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.util.Set;

public interface IssueKeyExtractor {

    Integer ISSUE_KEY_MAX_LIMIT = 100;

    Set<String> extractIssueKeys(WorkflowRun workflowRun);
}
