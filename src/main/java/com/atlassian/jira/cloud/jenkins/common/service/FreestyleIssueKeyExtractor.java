package com.atlassian.jira.cloud.jenkins.common.service;

import java.util.Set;

import hudson.model.AbstractBuild;

public interface FreestyleIssueKeyExtractor {
    Integer ISSUE_KEY_MAX_LIMIT = 100;

    Set<String> extractIssueKeys(AbstractBuild<?, ?> freestyleBuild);
}
