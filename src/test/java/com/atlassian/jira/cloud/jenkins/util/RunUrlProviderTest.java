package com.atlassian.jira.cloud.jenkins.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import hudson.model.Job;
import hudson.model.Run;
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper;
import org.junit.Test;

public class RunUrlProviderTest {

    @Test
    public void testGetRunUrl_fallsBackToAbsoluteUrlWhenJenkinsNull() throws Exception {
        final RunWrapper runWrapper = mock(RunWrapper.class);
        when(runWrapper.getAbsoluteUrl())
                .thenReturn("http://localhost:8080/job/test-repo/job/bugfix%252FSAL-1/1/");

        final String url = RunUrlProvider.getRunUrl(runWrapper);

        assertThat(url).isEqualTo("http://localhost:8080/job/test-repo/job/bugfix%252FSAL-1/1/");
    }

    @Test
    public void testGetRunUrl_handlesNullSafely() {
        final String url = RunUrlProvider.getRunUrl(null);
        assertThat(url).isEmpty();
    }

    @Test
    public void testGetPipelineUrl_fallsBackToParentAbsoluteUrl() throws Exception {
        final RunWrapper runWrapper = mock(RunWrapper.class);
        final Run run = mock(Run.class);
        final Job job = mock(Job.class);

        when(runWrapper.getRawBuild()).thenReturn(run);
        when(run.getParent()).thenReturn(job);
        when(job.getAbsoluteUrl())
                .thenReturn("http://localhost:8080/job/test-repo/job/bugfix%252FSAL-1/");

        final String url = RunUrlProvider.getPipelineUrl(runWrapper);

        assertThat(url).isEqualTo("http://localhost:8080/job/test-repo/job/bugfix%252FSAL-1/");
    }

    @Test
    public void testGetPipelineUrl_handlesNullSafely() {
        final String url = RunUrlProvider.getPipelineUrl(null);
        assertThat(url).isEmpty();
    }
}
