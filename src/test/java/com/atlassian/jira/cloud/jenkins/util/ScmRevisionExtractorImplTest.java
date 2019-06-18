package com.atlassian.jira.cloud.jenkins.util;

import hudson.model.Run;
import jenkins.plugins.git.GitBranchSCMHead;
import jenkins.plugins.git.GitBranchSCMRevision;
import jenkins.plugins.git.GitSCMSource;
import jenkins.scm.api.SCMRevisionAction;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ScmRevisionExtractorImplTest {

    private static final String BRANCH_NAME = "TEST-123-branch-name";

    private ScmRevisionExtractor classUnderTest;

    @Before
    public void setUp() throws Exception {
        classUnderTest = new ScmRevisionExtractorImpl();
    }

    @Test
    public void testGetScmRevision_whenScmRevisionActionNotNull() {
        // given
        final Run mockRun = mock(Run.class);
        final GitBranchSCMHead head = new GitBranchSCMHead(BRANCH_NAME);
        final SCMRevisionAction scmRevisionAction =
                new SCMRevisionAction(new GitSCMSource(""), new GitBranchSCMRevision(head, ""));
        when(mockRun.getAction(SCMRevisionAction.class)).thenReturn(scmRevisionAction);

        // when
        final Optional<ScmRevision> scmRevision = classUnderTest.getScmRevision(mockRun);

        // then
        assertThat(scmRevision.get().getHead()).isEqualTo(BRANCH_NAME);
    }

    @Test
    public void testGetScmRevision_whenScmRevisionActionNull() {
        // given
        final Run mockRun = mock(Run.class);

        // when
        final Optional<ScmRevision> scmRevision = classUnderTest.getScmRevision(mockRun);

        // then
        assertThat(scmRevision.isPresent()).isEqualTo(false);
    }
}
