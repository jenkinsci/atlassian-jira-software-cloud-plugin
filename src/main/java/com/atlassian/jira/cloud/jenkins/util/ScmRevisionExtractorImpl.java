package com.atlassian.jira.cloud.jenkins.util;

import hudson.model.Run;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMRevisionAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Extract SCM revision which triggered the build. Important: the action is only available for
 * Multibranch Pipeline jobs. It's not injected for Pipeline (single branch) jobs.
 */
public class ScmRevisionExtractorImpl implements ScmRevisionExtractor {

    private static final Logger logger = LoggerFactory.getLogger(ScmRevisionExtractorImpl.class);

    @Override
    public Optional<ScmRevision> getScmRevision(final Run build) {
        // The action is only injected for Multibranch Pipeline jobs
        // The action is not injected for Pipeline (single branch) jobs
        final SCMRevisionAction scmAction = build.getAction(SCMRevisionAction.class);

        if (scmAction == null) {
            logger.debug("SCMRevisionAction is null");
            return Optional.empty();
        }

        final SCMRevision revision = scmAction.getRevision();
        final ScmRevision scmRevision = new ScmRevision(revision.getHead().getName());

        return Optional.of(scmRevision);
    }
}
