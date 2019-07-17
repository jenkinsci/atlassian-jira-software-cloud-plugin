package com.atlassian.jira.cloud.jenkins.common.service;

import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.util.Set;

public interface IssueKeyExtractor {

    Set<String> extractIssueKeys(WorkflowRun workflowRun);

}
