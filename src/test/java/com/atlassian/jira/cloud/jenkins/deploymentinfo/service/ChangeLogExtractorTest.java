package com.atlassian.jira.cloud.jenkins.deploymentinfo.service;

import com.google.common.collect.ImmutableList;
import hudson.scm.ChangeLogSet;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ChangeLogExtractorTest {

    private ChangeLogExtractor changeLogExtractor;

    @Before
    public void setUp() {
        changeLogExtractor = new ChangeLogExtractorImpl();
    }

    @Test
    public void testExtractIssueKeys_forNoChangeSets() {
        // given
        final WorkflowRun workflowRun = workflowRunWithNoChangeSets();

        // when
        final Set<String> issueKeys = changeLogExtractor.extractIssueKeys(workflowRun);

        // then
        assertThat(issueKeys).isEmpty();
    }

    @Test
    public void testExtractIssueKeys_forOneChangeSetEntry() {
        // given
        final WorkflowRun workflowRun = changeSetWithOneChangeSetEntry();

        // when
        final Set<String> issueKeys = changeLogExtractor.extractIssueKeys(workflowRun);

        // then
        assertThat(issueKeys).containsExactlyInAnyOrder("TEST-123");
    }

    @Test
    public void testExtractIssueKeys_forMultipleChangeSetEntries() {
        // given
        final WorkflowRun workflowRun = changeSetWithMultipleChangeSetEntries();

        // when
        final Set<String> issueKeys = changeLogExtractor.extractIssueKeys(workflowRun);

        // then
        assertThat(issueKeys).containsExactlyInAnyOrder("TEST-123", "TEST-456");
    }

    @Test
    public void testExtractIssueKeys_forMultipleChangeSets() {
        // given
        final WorkflowRun workflowRun = workflowRunWithMultipleChangeSets();

        // when
        final Set<String> issueKeys = changeLogExtractor.extractIssueKeys(workflowRun);

        // then
        assertThat(issueKeys).containsExactlyInAnyOrder("TEST-123", "TEST-456");
    }

    @Test
    public void testExtractIssueKeys_forIssuesAboveLimit() {
        // given
        final WorkflowRun workflowRun = workflowRunWithIssuesAboveLimit();

        // when
        final Set<String> issueKeys = changeLogExtractor.extractIssueKeys(workflowRun);

        // then
        assertThat(issueKeys).hasSize(100);
    }

    private WorkflowRun workflowRunWithNoChangeSets() {
        final WorkflowRun workflowRun = mock(WorkflowRun.class);

        when(workflowRun.getChangeSets()).thenReturn(Collections.emptyList());
        return workflowRun;
    }

    private WorkflowRun changeSetWithOneChangeSetEntry() {
        final ChangeLogSet.Entry entry = mock(ChangeLogSet.Entry.class);
        when(entry.getMsg()).thenReturn("TEST-123 Commit message");
        final ChangeLogSet changeLogSet = mock(ChangeLogSet.class);
        when(changeLogSet.getItems()).thenReturn(new Object[]{entry});
        final WorkflowRun workflowRun = mock(WorkflowRun.class);

        when(workflowRun.getChangeSets()).thenReturn(ImmutableList.of(changeLogSet));
        return workflowRun;
    }

    private WorkflowRun changeSetWithMultipleChangeSetEntries() {
        final ChangeLogSet.Entry entry1 = mock(ChangeLogSet.Entry.class);
        final ChangeLogSet.Entry entry2 = mock(ChangeLogSet.Entry.class);
        when(entry1.getMsg()).thenReturn("TEST-123 Commit message");
        when(entry2.getMsg()).thenReturn("TEST-456 Commit message");
        final ChangeLogSet changeLogSet = mock(ChangeLogSet.class);
        when(changeLogSet.getItems()).thenReturn(new Object[]{entry1, entry2});
        final WorkflowRun workflowRun = mock(WorkflowRun.class);

        when(workflowRun.getChangeSets()).thenReturn(ImmutableList.of(changeLogSet));
        return workflowRun;
    }

    private WorkflowRun workflowRunWithMultipleChangeSets() {
        final ChangeLogSet.Entry entry1 = mock(ChangeLogSet.Entry.class);
        final ChangeLogSet.Entry entry2 = mock(ChangeLogSet.Entry.class);
        when(entry1.getMsg()).thenReturn("TEST-123 Commit message");
        when(entry2.getMsg()).thenReturn("TEST-456 Commit message");
        final ChangeLogSet changeLogSet1 = mock(ChangeLogSet.class);
        final ChangeLogSet changeLogSet2 = mock(ChangeLogSet.class);
        when(changeLogSet1.getItems()).thenReturn(new Object[]{entry1});
        when(changeLogSet2.getItems()).thenReturn(new Object[]{entry2});
        final WorkflowRun workflowRun = mock(WorkflowRun.class);

        when(workflowRun.getChangeSets()).thenReturn(ImmutableList.of(changeLogSet1, changeLogSet2));
        return workflowRun;
    }

    private WorkflowRun workflowRunWithIssuesAboveLimit() {
        int count = 105;
        Object[] changeSetEntries = new Object[count];
        for (int i = 0; i < count; i ++) {
            final ChangeLogSet.Entry entry = mock(ChangeLogSet.Entry.class);
            when(entry.getMsg()).thenReturn(String.format("TEST-%d Commit message for %d", i, i));
            changeSetEntries[i] = entry;
        }
        final ChangeLogSet changeLogSet = mock(ChangeLogSet.class);

        when(changeLogSet.getItems()).thenReturn(changeSetEntries);
        final WorkflowRun workflowRun = mock(WorkflowRun.class);

        when(workflowRun.getChangeSets()).thenReturn(ImmutableList.of(changeLogSet));
        return workflowRun;
    }

}
