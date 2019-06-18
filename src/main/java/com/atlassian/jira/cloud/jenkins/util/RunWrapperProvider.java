package com.atlassian.jira.cloud.jenkins.util;

import hudson.model.Run;
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper;

public interface RunWrapperProvider {

    RunWrapper getWrapper(Run build);
}
