package com.atlassian.jira.cloud.jenkins.util;

import hudson.model.Run;

import java.util.Optional;

public interface ScmRevisionExtractor {

    Optional<ScmRevision> getScmRevision(Run build);
}
