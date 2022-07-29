package com.atlassian.jira.cloud.jenkins.deploymentinfo.service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.atlassian.jira.cloud.jenkins.common.model.IssueKey;
import com.atlassian.jira.cloud.jenkins.common.service.FreestyleIssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.logging.PipelineLogger;
import com.atlassian.jira.cloud.jenkins.util.IssueKeyStringExtractor;

import hudson.model.AbstractBuild;
import hudson.plugins.git.GitChangeSet;
import hudson.scm.ChangeLogSet;

public class FreestyleChangeLogIssueKeyExtractor implements FreestyleIssueKeyExtractor {
    @Override
    public Set<String> extractIssueKeys(
            final AbstractBuild<?, ?> freestyleBuild, final PipelineLogger pipelineLogger) {

        final Set<IssueKey> allIssueKeys = new HashSet<>();
        final List<ChangeLogSet<? extends ChangeLogSet.Entry>> changeSets =
                freestyleBuild.getChangeSets();
        for (ChangeLogSet<? extends ChangeLogSet.Entry> changeSet : changeSets) {
            final Object[] changeSetEntries = changeSet.getItems();
            for (Object item : changeSetEntries) {
                final ChangeLogSet.Entry changeSetEntry = (ChangeLogSet.Entry) item;
                if (changeSetEntry instanceof GitChangeSet) {
                    String comment = ((GitChangeSet) changeSetEntry).getComment();
                    Set<IssueKey> issueKeys = IssueKeyStringExtractor.extractIssueKeys(comment);
                    allIssueKeys.addAll(issueKeys);
                    pipelineLogger.info(
                            String.format(
                                    "Extracted issue keys from GitChangeSet comment '%s': %s",
                                    comment, Arrays.toString(issueKeys.toArray())));
                }

                String message = changeSetEntry.getMsg();
                Set<IssueKey> issueKeys = IssueKeyStringExtractor.extractIssueKeys(message);
                allIssueKeys.addAll(issueKeys);
                pipelineLogger.info(
                        String.format(
                                "Extracted issue keys from ChangeLogSet message '%s': %s",
                                message, Arrays.toString(issueKeys.toArray())));

                if (allIssueKeys.size() >= ISSUE_KEY_MAX_LIMIT) {
                    pipelineLogger.warn(
                            String.format(
                                    "Not extracting any more issues as the maximum of %d has been reached!",
                                    ISSUE_KEY_MAX_LIMIT));
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
