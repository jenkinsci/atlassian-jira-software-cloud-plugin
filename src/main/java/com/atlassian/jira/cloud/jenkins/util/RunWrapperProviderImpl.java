package com.atlassian.jira.cloud.jenkins.util;

import hudson.model.Run;
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper;

public class RunWrapperProviderImpl implements RunWrapperProvider {

    @Override
    public RunWrapper getWrapper(final Run build) {
        return new RunWrapper(build, true);
    }
}
