package com.atlassian.jira.cloud.jenkins.util;

import com.atlassian.jira.cloud.jenkins.common.model.IssueKey;
import com.atlassian.jira.cloud.jenkins.common.service.IssueKeyExtractor;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMRevisionAction;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * Extract SCM revision which triggered the build. Important: the action is only available for
 * Multibranch Pipeline jobs. It's not injected for Pipeline (single branch) jobs.
 */
public class BranchNameIssueKeyExtractor implements IssueKeyExtractor {

    private static final Logger logger = LoggerFactory.getLogger(BranchNameIssueKeyExtractor.class);

    @Override
    public Set<String> extractIssueKeys(final WorkflowRun build) {
        // The action is only injected for Multibranch Pipeline jobs
        // The action is not injected for Pipeline (single branch) jobs
        final SCMRevisionAction scmAction = build.getAction(SCMRevisionAction.class);

        if (scmAction == null) {
            logger.debug("SCMRevisionAction is null");
            return new HashSet<String>();
        }

        final SCMRevision revision = scmAction.getRevision();
        final ScmRevision scmRevision = new ScmRevision(revision.getHead().getName());

        return extractIssueKeys(scmRevision);
    }

    private Set<String> extractIssueKeys(final ScmRevision scmRevision) {
        return IssueKeyStringExtractor.extractIssueKeys(scmRevision.getHead())
                .stream()
                .map(IssueKey::toString)
                .collect(Collectors.toSet());
    }
}
