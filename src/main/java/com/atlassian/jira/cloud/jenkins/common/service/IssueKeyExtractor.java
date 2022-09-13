package com.atlassian.jira.cloud.jenkins.common.service;

import com.atlassian.jira.cloud.jenkins.logging.PipelineLogger;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.util.Set;

public interface IssueKeyExtractor {

    Integer ISSUE_KEY_MAX_LIMIT = 500;

    Set<String> extractIssueKeys(WorkflowRun workflowRun, final PipelineLogger pipelineLogger);
}
