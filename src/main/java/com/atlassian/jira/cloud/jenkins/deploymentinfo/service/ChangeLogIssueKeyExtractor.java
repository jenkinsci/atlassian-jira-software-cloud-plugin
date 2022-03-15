package com.atlassian.jira.cloud.jenkins.deploymentinfo.service;

import com.atlassian.jira.cloud.jenkins.common.model.IssueKey;
import com.atlassian.jira.cloud.jenkins.common.service.IssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.util.IssueKeyStringExtractor;
import hudson.model.Result;
import hudson.model.Run;
import hudson.plugins.git.GitChangeSet;
import hudson.scm.ChangeLogSet;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import javax.annotation.CheckForNull;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Parses the change log from the current build and extracts the issue keys from the commit
 * messages. It also tries to extract from squashed commits.
 */
public final class ChangeLogIssueKeyExtractor implements IssueKeyExtractor {

    public Set<String> extractIssueKeys(final WorkflowRun workflowRun) {

        final Set<IssueKey> allIssueKeys = new HashSet<>();
        final List<ChangeLogSet<? extends ChangeLogSet.Entry>> changeSets =
                new ArrayList<>(workflowRun.getChangeSets());

        WorkflowRun previous = workflowRun.getPreviousBuild();
        while (Objects.nonNull(previous) && !isBuildSuccessful(previous)) {
            changeSets.addAll(previous.getChangeSets());
            previous = previous.getPreviousBuild();
        }

        for (ChangeLogSet<? extends ChangeLogSet.Entry> changeSet : changeSets) {
            final Object[] changeSetEntries = changeSet.getItems();
            for (Object item : changeSetEntries) {
                final ChangeLogSet.Entry changeSetEntry = (ChangeLogSet.Entry) item;

                if (changeSetEntry instanceof GitChangeSet) {
                    allIssueKeys.addAll(
                            IssueKeyStringExtractor.extractIssueKeys(
                                    ((GitChangeSet) changeSetEntry).getComment()));
                }
                allIssueKeys.addAll(
                        IssueKeyStringExtractor.extractIssueKeys(changeSetEntry.getMsg()));

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

    private boolean isBuildSuccessful(@CheckForNull final WorkflowRun workflowRun) {

        return Optional.ofNullable(workflowRun)
                .map(Run::getResult)
                .map(Result::toString)
                .map(Result.SUCCESS.toString()::equals)
                .orElse(true);
    }
}
