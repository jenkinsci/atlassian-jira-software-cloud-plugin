package com.atlassian.jira.cloud.jenkins.deploymentinfo.service;

import com.atlassian.jira.cloud.jenkins.common.model.IssueKey;
import com.atlassian.jira.cloud.jenkins.common.service.IssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.logging.PipelineLogger;
import com.atlassian.jira.cloud.jenkins.util.IssueKeyStringExtractor;
import hudson.model.Result;
import hudson.model.Run;
import hudson.plugins.git.GitChangeSet;
import hudson.scm.ChangeLogSet;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import javax.annotation.CheckForNull;
import java.util.Arrays;
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

    public Set<String> extractIssueKeys(
            final WorkflowRun workflowRun, final PipelineLogger pipelineLogger) {

        final Set<IssueKey> allIssueKeys = new HashSet<>();
        final List<ChangeLogSet<? extends ChangeLogSet.Entry>> changeSets =
                new ArrayList<>(workflowRun.getChangeSets());

        if (changeSets.size() == 0) {
            // When promoting a deployment or deploying to another environment, the Git change set won't be available.
            // This is because Jenkins checks for Git changes since the last build, and there won't be any.
            // To address this, we traverse back to retrieve the last change so we have the changes to inspect for issue keys.
            addChangeSetsFromFailedPreviousBuilds(workflowRun, changeSets);
        }
        addFirstNonEmptyChangeSetFromPreviousBuilds(workflowRun, changeSets);

        for (ChangeLogSet<? extends ChangeLogSet.Entry> changeSet : changeSets) {
            final Object[] changeSetEntries = changeSet.getItems();
            for (Object item : changeSetEntries) {
                final ChangeLogSet.Entry changeSetEntry = (ChangeLogSet.Entry) item;

                if (changeSetEntry instanceof GitChangeSet) {
                    String comment = ((GitChangeSet) changeSetEntry).getComment();
                    Set<IssueKey> issueKeys = IssueKeyStringExtractor.extractIssueKeys(comment);
                    allIssueKeys.addAll(issueKeys);
                    pipelineLogger.debug(
                            String.format(
                                    "Extracted issue keys from GitChangeSet comment '%s': %s",
                                    comment, Arrays.toString(issueKeys.toArray())));
                }

                String message = changeSetEntry.getMsg();
                Set<IssueKey> issueKeys = IssueKeyStringExtractor.extractIssueKeys(message);
                allIssueKeys.addAll(issueKeys);
                pipelineLogger.debug(
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

    private void addFirstNonEmptyChangeSetFromPreviousBuilds(
            final WorkflowRun workflowRun,
            final List<ChangeLogSet<? extends ChangeLogSet.Entry>> changeSets) {
        WorkflowRun previous = workflowRun.getPreviousBuild();
        while (Objects.nonNull(previous) && changeSets.isEmpty()) {
            changeSets.addAll(previous.getChangeSets());
            previous = previous.getPreviousBuild();
        }
    }

    private void addChangeSetsFromFailedPreviousBuilds(
            final WorkflowRun workflowRun,
            final List<ChangeLogSet<? extends ChangeLogSet.Entry>> changeSets) {
        WorkflowRun previous = workflowRun.getPreviousBuild();
        while (Objects.nonNull(previous) && !isBuildSuccessful(previous)) {
            changeSets.addAll(previous.getChangeSets());
            previous = previous.getPreviousBuild();
        }
    }

    private boolean isBuildSuccessful(@CheckForNull final WorkflowRun workflowRun) {

        return Optional.ofNullable(workflowRun)
                .map(Run::getResult)
                .map(Result::toString)
                .map(Result.SUCCESS.toString()::equals)
                .orElse(true);
    }
}
