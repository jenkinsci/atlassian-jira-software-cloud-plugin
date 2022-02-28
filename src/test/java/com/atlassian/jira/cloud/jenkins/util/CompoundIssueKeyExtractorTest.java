package com.atlassian.jira.cloud.jenkins.util;

import com.atlassian.jira.cloud.jenkins.common.service.IssueKeyExtractor;
import org.assertj.core.util.Sets;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Test;

import java.util.Arrays;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;

public class CompoundIssueKeyExtractorTest {

    @Test
    public void test() {
        WorkflowRun run = mock(WorkflowRun.class);
        IssueKeyExtractor extractor1 =
                workflowRun -> Sets.newHashSet(Arrays.asList("TEST-1", "TEST-2"));
        IssueKeyExtractor extractor2 =
                workflowRun -> Sets.newHashSet(Arrays.asList("TEST-3", "TEST-4"));
        CompoundIssueKeyExtractor compoundExtractor =
                new CompoundIssueKeyExtractor(extractor1, extractor2);
        assertThat(compoundExtractor.extractIssueKeys(run).toArray())
                .contains("TEST-1", "TEST-2", "TEST-3", "TEST-4");
    }
}
