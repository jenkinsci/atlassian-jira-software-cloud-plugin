package com.atlassian.jira.cloud.jenkins.util;

import com.atlassian.jira.cloud.jenkins.common.model.IssueKey;
import com.atlassian.jira.cloud.jenkins.common.service.IssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.logging.PipelineLogger;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMRevisionAction;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Extract SCM revision which triggered the build. Important: the action is only available for
 * Multibranch Pipeline jobs. It's not injected for Pipeline (single branch) jobs.
 */
public class BranchNameIssueKeyExtractor implements IssueKeyExtractor {

    @Override
    public Set<String> extractIssueKeys(
            final WorkflowRun build, final PipelineLogger pipelineLogger) {
        // The action is only injected for Multibranch Pipeline jobs
        // The action is not injected for Pipeline (single branch) jobs
        final SCMRevisionAction scmAction = build.getAction(SCMRevisionAction.class);

        if (scmAction == null) {
            pipelineLogger.debug(
                    String.format(
                            "Could not extract issue keys from branch name of build %s due to: SCMRevisionAction is null",
                            build.number));
            return Collections.emptySet();
        }

        final SCMRevision revision = scmAction.getRevision();
        final ScmRevision scmRevision = new ScmRevision(revision.getHead().getName());

        Set<String> issueKeys = extractIssueKeys(scmRevision);

        pipelineLogger.debug(
                String.format(
                        "Extracted the following issue keys out of branch name '%s': %s",
                        scmRevision.getHead(), Arrays.toString(issueKeys.toArray())));

        return issueKeys;
    }

    private Set<String> extractIssueKeys(final ScmRevision scmRevision) {
        return IssueKeyStringExtractor.extractIssueKeys(scmRevision.getHead())
                .stream()
                .map(IssueKey::toString)
                .collect(Collectors.toSet());
    }
}
