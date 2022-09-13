package com.atlassian.jira.cloud.jenkins.util;

import com.atlassian.jira.cloud.jenkins.common.model.IssueKey;
import com.atlassian.jira.cloud.jenkins.common.service.IssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.logging.PipelineLogger;
import jenkins.scm.api.SCMRevision;
import jenkins.scm.api.SCMRevisionAction;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.util.HashSet;
import java.util.Map;
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

        final Set<String> issueKeys = new HashSet<>();

        if (scmAction != null) {
            final SCMRevision revision = scmAction.getRevision();
            final ScmRevision scmRevision = new ScmRevision(revision.getHead().getName());
            issueKeys.addAll(extractIssueKeys(scmRevision.getHead()));

            pipelineLogger.debug(
                    String.format(
                            "Extracted issue keys from scmRevision.getHead() (%s): %s",
                            scmRevision.getHead(), issueKeys));
        } else {
            pipelineLogger.debug(
                    "Not extracting issue keys from scmRevision.getHead() because it's not set");
        }

        final Map<String, String> envVars = build.getEnvVars();

        // You can get an overview of a Jenkins server's environment variables by opening
        // http://your-jenkins-server/env-vars.html

        // For a multibranch project, this will be set to the name of the branch being built, for example in case you
        // wish to deploy to production from master but not from feature branches; if corresponding to some kind of
        // change request, the name is generally arbitrary
        final String branchNameEnvVar = envVars.get("BRANCH_NAME");
        if (branchNameEnvVar != null) {
            Set<String> extractedIssueKeys = extractIssueKeys(branchNameEnvVar);
            issueKeys.addAll(extractedIssueKeys);
            pipelineLogger.debug(
                    String.format(
                            "Extracted issue keys from env var BRANCH_NAME (%s): %s",
                            branchNameEnvVar, extractedIssueKeys));
        } else {
            pipelineLogger.debug(
                    "Not extracting issue keys from environment variable BRANCH_NAME because it's not set");
        }

        // For a multibranch project corresponding to some kind of change request, this will be set to the name of the
        // actual head on the source control system which may or may not be different from BRANCH_NAME. For example
        // in GitHub or Bitbucket this would have the name of the origin branch whereas BRANCH_NAME would be
        // something like PR-24.
        final String changeBranchEnvVar = envVars.get("CHANGE_BRANCH");
        if (changeBranchEnvVar != null) {
            Set<String> extractedIssueKeys = extractIssueKeys(changeBranchEnvVar);
            issueKeys.addAll(extractedIssueKeys);
            pipelineLogger.debug(
                    String.format(
                            "Extracted issue keys from env var CHANGE_BRANCH (%s): %s",
                            changeBranchEnvVar, extractedIssueKeys));
        } else {
            pipelineLogger.debug(
                    "Not extracting issue keys from environment variable CHANGE_BRANCH because it's not set");
        }

        pipelineLogger.debug(
                String.format(
                        "Extracted the following issue keys out of branch name: %s",
                        issueKeys));

        return issueKeys;
    }

    private Set<String> extractIssueKeys(final String stringWithIssueKeys) {
        return IssueKeyStringExtractor.extractIssueKeys(stringWithIssueKeys)
                .stream()
                .map(IssueKey::toString)
                .collect(Collectors.toSet());
    }
}
