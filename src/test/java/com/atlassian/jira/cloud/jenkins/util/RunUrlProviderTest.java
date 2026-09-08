package com.atlassian.jira.cloud.jenkins.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Job;
import hudson.model.Run;
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class RunUrlProviderTest {

    @Rule public JenkinsRule jRule = new JenkinsRule();

    @Test
    public void testGetRunUrl_withJenkinsRootUrl() throws Exception {
        final FreeStyleProject project = jRule.createFreeStyleProject("test-project");
        final FreeStyleBuild build = jRule.buildAndAssertSuccess(project);

        final RunWrapper runWrapper = mock(RunWrapper.class);
        doReturn(build).when(runWrapper).getRawBuild();

        final String url = RunUrlProvider.getRunUrl(runWrapper);

        final String expectedUrl = jRule.jenkins.getRootUrl() + build.getUrl();
        assertThat(url).isEqualTo(expectedUrl);
    }

    @Test
    public void testGetPipelineUrl_withJenkinsRootUrl() throws Exception {
        final FreeStyleProject project = jRule.createFreeStyleProject("test-pipeline");
        final FreeStyleBuild build = jRule.buildAndAssertSuccess(project);

        final RunWrapper runWrapper = mock(RunWrapper.class);
        doReturn(build).when(runWrapper).getRawBuild();

        final String url = RunUrlProvider.getPipelineUrl(runWrapper);

        final String expectedUrl = jRule.jenkins.getRootUrl() + project.getUrl();
        assertThat(url).isEqualTo(expectedUrl);
    }

    @Test
    public void testGetRunUrl_handlesNullSafely() {
        final String url = RunUrlProvider.getRunUrl(null);
        assertThat(url).isEmpty();
    }

    @Test
    public void testGetPipelineUrl_handlesNullSafely() {
        final String url = RunUrlProvider.getPipelineUrl(null);
        assertThat(url).isEmpty();
    }
}
