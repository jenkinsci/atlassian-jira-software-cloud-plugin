package com.atlassian.jira.cloud.jenkins.deploymentinfo.service;

import com.atlassian.jira.cloud.jenkins.common.model.IssueKey;
import com.atlassian.jira.cloud.jenkins.util.IssueKeyExtractor;
import hudson.plugins.git.GitChangeSet;
import hudson.scm.ChangeLogSet;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.atlassian.jira.cloud.jenkins.util.IssueKeyExtractor.ISSUE_KEY_MAX_LIMIT;

public final class ChangeLogExtractorImpl implements ChangeLogExtractor {

    public Set<String> extractIssueKeys(final WorkflowRun workflowRun) {

        final Set<IssueKey> allIssueKeys = new HashSet<>();
        final List<ChangeLogSet<? extends ChangeLogSet.Entry>> changeSets =
                workflowRun.getChangeSets();

        for (ChangeLogSet<? extends ChangeLogSet.Entry> changeSet : changeSets) {
            final Object[] changeSetEntries = changeSet.getItems();
            for (Object item : changeSetEntries) {
                final ChangeLogSet.Entry changeSetEntry = (ChangeLogSet.Entry) item;

                if (changeSetEntry instanceof GitChangeSet) {
                    allIssueKeys.addAll(IssueKeyExtractor.extractIssueKeys(((GitChangeSet) changeSetEntry).getComment()));
                }

                allIssueKeys.addAll(IssueKeyExtractor.extractIssueKeys(changeSetEntry.getMsg()));
                allIssueKeys.addAll(
                        IssueKeyExtractor.extractIssueKeys(changeSetEntry.getMsgAnnotated()));

                if (allIssueKeys.size() >= ISSUE_KEY_MAX_LIMIT) {
                    break;
                }
            }
        }

        return allIssueKeys
                .stream()
                .limit(ISSUE_KEY_MAX_LIMIT)
                .map(IssueKey::toString)
                .collect(Collectors.toSet());
    }
}
