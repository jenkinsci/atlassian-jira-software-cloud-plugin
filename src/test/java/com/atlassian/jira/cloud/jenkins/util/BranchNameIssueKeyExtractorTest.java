package com.atlassian.jira.cloud.jenkins.util;

import com.atlassian.jira.cloud.jenkins.common.service.IssueKeyExtractor;
import jenkins.plugins.git.GitBranchSCMHead;
import jenkins.plugins.git.GitBranchSCMRevision;
import jenkins.plugins.git.GitSCMSource;
import jenkins.scm.api.SCMRevisionAction;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BranchNameIssueKeyExtractorTest {

    private static final String BRANCH_NAME = "TEST-123-branch-name";

    private IssueKeyExtractor classUnderTest;

    @Before
    public void setUp() throws Exception {
        classUnderTest = new BranchNameIssueKeyExtractor();
    }

    @Test
    public void testExtractIssueKeys_whenScmRevisionActionNotNull() {
        // given
        final WorkflowRun mockWorkflowRun = mock(WorkflowRun.class);
        final GitBranchSCMHead head = new GitBranchSCMHead(BRANCH_NAME);
        final SCMRevisionAction scmRevisionAction =
                new SCMRevisionAction(new GitSCMSource(""), new GitBranchSCMRevision(head, ""));
        when(mockWorkflowRun.getAction(SCMRevisionAction.class)).thenReturn(scmRevisionAction);

        // when
        final Set<String> issueKeys = classUnderTest.extractIssueKeys(mockWorkflowRun);

        // then
        assertThat(issueKeys).containsExactlyInAnyOrder("TEST-123");
    }

    @Test
    public void testExtractIssueKeys_whenScmRevisionActionNull() {
        // given
        final WorkflowRun mockWorkflowRun = mock(WorkflowRun.class);

        // when
        final Set<String> issueKeys = classUnderTest.extractIssueKeys(mockWorkflowRun);

        // then
        assertThat(issueKeys).isEmpty();
    }
}
