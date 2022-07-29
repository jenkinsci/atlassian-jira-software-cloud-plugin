package com.atlassian.jira.cloud.jenkins.deploymentinfo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

import com.atlassian.jira.cloud.jenkins.logging.PipelineLogger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.atlassian.jira.cloud.jenkins.common.service.FreestyleIssueKeyExtractor;
import com.google.common.collect.ImmutableList;

import hudson.model.AbstractBuild;
import hudson.plugins.git.GitChangeSet;
import hudson.scm.ChangeLogSet;

@RunWith(MockitoJUnitRunner.class)
public class FreestyleChangeLogIssueKeyExtractorTest {

    private FreestyleIssueKeyExtractor changeLogExtractor;

    @Before
    public void setUp() {
        changeLogExtractor = new FreestyleChangeLogIssueKeyExtractor();
    }

    @Test
    public void testExtractIssueKeys_forNoChangeSets() {
        // given
        final AbstractBuild build = buildWithNoChangeSets();

        // when
        final Set<String> issueKeys =
                changeLogExtractor.extractIssueKeys(build, PipelineLogger.noopInstance());

        // then
        assertThat(issueKeys).isEmpty();
    }

    @Test
    public void testExtractIssueKeys_forOneChangeSetEntry() {
        // given
        final AbstractBuild build = changeSetWithOneChangeSetEntry();

        // when
        final Set<String> issueKeys =
                changeLogExtractor.extractIssueKeys(build, PipelineLogger.noopInstance());

        // then
        assertThat(issueKeys).containsExactlyInAnyOrder("TEST-123");
    }

    @Test
    public void testExtractIssueKeys_forMultipleChangeSetEntries() {
        // given
        final AbstractBuild build = changeSetWithMultipleChangeSetEntries();

        // when
        final Set<String> issueKeys =
                changeLogExtractor.extractIssueKeys(build, PipelineLogger.noopInstance());

        // then
        assertThat(issueKeys).containsExactlyInAnyOrder("TEST-123", "TEST-456");
    }

    @Test
    public void testExtractIssueKeys_forSquashedCommits() {
        // given
        final AbstractBuild build = changeSetWithSquashedCommitsInComment();

        // when
        final Set<String> issueKeys =
                changeLogExtractor.extractIssueKeys(build, PipelineLogger.noopInstance());

        // then
        assertThat(issueKeys).containsExactlyInAnyOrder("TEST-3", "TEST-4");
    }

    @Test
    public void testExtractIssueKeys_forIssuesAboveLimit() {
        // given
        final AbstractBuild build = buildWithIssuesAboveLimit();

        // when
        final Set<String> issueKeys =
                changeLogExtractor.extractIssueKeys(build, PipelineLogger.noopInstance());

        // then
        assertThat(issueKeys).hasSize(100);
    }

    private AbstractBuild buildWithIssuesAboveLimit() {
        int count = 105;
        Object[] changeSetEntries = new Object[count];
        for (int i = 0; i < count; i++) {
            final ChangeLogSet.Entry entry = mock(ChangeLogSet.Entry.class);
            when(entry.getMsg()).thenReturn(String.format("TEST-%d Commit message for %d", i, i));
            changeSetEntries[i] = entry;
        }
        final ChangeLogSet changeLogSet = mock(ChangeLogSet.class);

        when(changeLogSet.getItems()).thenReturn(changeSetEntries);
        final AbstractBuild build = mock(AbstractBuild.class);

        when(build.getChangeSets()).thenReturn(ImmutableList.of(changeLogSet));
        return build;
    }

    private AbstractBuild changeSetWithOneChangeSetEntry() {
        final ChangeLogSet.Entry entry = mock(ChangeLogSet.Entry.class);
        when(entry.getMsg()).thenReturn("TEST-123 Commit message");
        final ChangeLogSet changeLogSet = mock(ChangeLogSet.class);
        when(changeLogSet.getItems()).thenReturn(new Object[] {entry});
        final AbstractBuild build = mock(AbstractBuild.class);

        when(build.getChangeSets()).thenReturn(ImmutableList.of(changeLogSet));
        return build;
    }

    private AbstractBuild changeSetWithMultipleChangeSetEntries() {
        final ChangeLogSet.Entry entry1 = mock(ChangeLogSet.Entry.class);
        final ChangeLogSet.Entry entry2 = mock(ChangeLogSet.Entry.class);
        when(entry1.getMsg()).thenReturn("TEST-123 Commit message");
        when(entry2.getMsg()).thenReturn("TEST-456 Commit message");
        final ChangeLogSet changeLogSet = mock(ChangeLogSet.class);
        when(changeLogSet.getItems()).thenReturn(new Object[] {entry1, entry2});
        final AbstractBuild build = mock(AbstractBuild.class);

        when(build.getChangeSets()).thenReturn(ImmutableList.of(changeLogSet));
        return build;
    }

    private AbstractBuild buildWithNoChangeSets() {
        final AbstractBuild build = mock(AbstractBuild.class);

        when(build.getChangeSets()).thenReturn(Collections.emptyList());
        return build;
    }

    private AbstractBuild changeSetWithSquashedCommitsInComment() {
        final GitChangeSet entry = mock(GitChangeSet.class);
        when(entry.getComment())
                .thenReturn(
                        "Squashed Commit title (#12)\n"
                                + "* TEST-3 Fix bug #1\n"
                                + "\n"
                                + "* TEST-4 Fix bug #2\n");
        final ChangeLogSet changeLogSet = mock(ChangeLogSet.class);
        when(changeLogSet.getItems()).thenReturn(new Object[] {entry});
        final AbstractBuild build = mock(AbstractBuild.class);

        when(build.getChangeSets()).thenReturn(ImmutableList.of(changeLogSet));
        return build;
    }
}
