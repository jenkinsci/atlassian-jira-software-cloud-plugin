package com.atlassian.jira.cloud.jenkins.deploymentinfo.service;

import hudson.model.Result;
import hudson.scm.ChangeLogSet;
import junit.framework.TestCase;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Test;
import org.jvnet.hudson.test.FakeChangeLogSCM.EntryImpl;
import org.jvnet.hudson.test.FakeChangeLogSCM.FakeChangeLogSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;

public class ChangeLogIssueKeyExtractorTest extends TestCase {

    @Test
    public void testExtractIssueKeys() {

        WorkflowRun current = mock(WorkflowRun.class);
        WorkflowRun previous1 = mock(WorkflowRun.class);
        WorkflowRun previous2 = mock(WorkflowRun.class);

        when(current.getPreviousBuild()).thenReturn(previous1);
        when(previous1.getPreviousBuild()).thenReturn(previous2);
        when(previous2.getPreviousBuild()).thenReturn(null);

        EntryImpl e1 = new EntryImpl().withMsg("TEST-1 Some message");
        EntryImpl e2 = new EntryImpl().withMsg("TEST-2 Another message");
        EntryImpl e3 = new EntryImpl().withMsg("TEST-3 One more message");

        List<EntryImpl> entryListCurrent = new ArrayList<>();
        entryListCurrent.add(e1);
        List<EntryImpl> entryListPrevious1 = new ArrayList<>();
        entryListPrevious1.add(e2);
        List<EntryImpl> entryListPrevious2 = new ArrayList<>();
        entryListPrevious2.add(e3);

        FakeChangeLogSet changeLogSetCurrent =
                new FakeChangeLogSet(current, entryListCurrent);
        FakeChangeLogSet changeLogSetPrevious1 =
                new FakeChangeLogSet(previous1, entryListPrevious1);
        FakeChangeLogSet changeLogSetPrevious2 =
                new FakeChangeLogSet(previous2, entryListPrevious2);

        List<ChangeLogSet<? extends ChangeLogSet.Entry>> changeSetsCurrent =
                new ArrayList<>();
        changeSetsCurrent.add(changeLogSetCurrent);

        List<ChangeLogSet<? extends ChangeLogSet.Entry>> changeSetsPrevious1 =
                new ArrayList<>();
        changeSetsPrevious1.add(changeLogSetPrevious1);

        List<ChangeLogSet<? extends ChangeLogSet.Entry>> changeSetsPrevious2 =
                new ArrayList<>();
        changeSetsPrevious2.add(changeLogSetPrevious2);

        when(current.getChangeSets()).thenReturn(changeSetsCurrent);
        when(previous1.getChangeSets()).thenReturn(changeSetsPrevious1);
        when(previous2.getChangeSets()).thenReturn(changeSetsPrevious2);

        when(previous1.getResult()).thenReturn(Result.FAILURE);
        when(previous2.getResult()).thenReturn(Result.SUCCESS);

        ChangeLogIssueKeyExtractor extractor = new ChangeLogIssueKeyExtractor();
        Set<String> issueKeys = extractor.extractIssueKeys(current);

        verify(previous1).getChangeSets();
        verify(previous2, never()).getChangeSets();

        assertTrue(issueKeys.contains("TEST-1"));
        assertTrue(issueKeys.contains("TEST-2"));
        assertFalse(issueKeys.contains("TEST-3"));
    }
}