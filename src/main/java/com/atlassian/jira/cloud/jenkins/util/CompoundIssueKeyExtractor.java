package com.atlassian.jira.cloud.jenkins.util;

import com.atlassian.jira.cloud.jenkins.common.service.IssueKeyExtractor;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/** Combines multiple issue key extractors. */
public class CompoundIssueKeyExtractor implements IssueKeyExtractor {

    private final IssueKeyExtractor[] extractors;

    public CompoundIssueKeyExtractor(final IssueKeyExtractor... extractors) {
        this.extractors = extractors;
    }

    @Override
    public Set<String> extractIssueKeys(final WorkflowRun workflowRun) {
        return Arrays.stream(this.extractors)
                .map(extractor -> extractor.extractIssueKeys(workflowRun))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }
}
