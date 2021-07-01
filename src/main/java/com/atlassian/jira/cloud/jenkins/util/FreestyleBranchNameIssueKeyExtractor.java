package com.atlassian.jira.cloud.jenkins.util;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.cloud.jenkins.common.model.IssueKey;
import com.atlassian.jira.cloud.jenkins.common.service.FreestyleIssueKeyExtractor;

import hudson.model.AbstractBuild;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitSCM;
import hudson.scm.SCM;

public class FreestyleBranchNameIssueKeyExtractor implements FreestyleIssueKeyExtractor {
    private static final Logger logger = LoggerFactory.getLogger(FreestyleBranchNameIssueKeyExtractor.class);

    @Override
    public Set<String> extractIssueKeys(final AbstractBuild<?, ?> freestyleBuild) {
        // The action is only injected for Freestyle jobs
        if (null != freestyleBuild.getProject() && null != freestyleBuild.getProject().getScm()) {
            SCM scm = freestyleBuild.getProject().getScm();
            GitSCM scm1 = (GitSCM) scm;
            List<BranchSpec> branches = scm1.getBranches();
            Set<String> sets = IssueKeyStringExtractor.extractIssueKeys(branches.get(0).getName()).stream()
                    .map(IssueKey::toString).collect(Collectors.toSet());

            return sets;
        }
        logger.debug("branch details are not present");
        return Collections.emptySet();

    }
}
