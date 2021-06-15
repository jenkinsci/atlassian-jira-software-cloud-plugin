package com.atlassian.jira.cloud.jenkins.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.tools.ant.Project;
import org.junit.Before;
import org.junit.Test;

import com.atlassian.jira.cloud.jenkins.common.service.FreestyleIssueKeyExtractor;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitSCM;

public class FreestyleBranchNameIssueKeyExtractorTest {

	private static final String BRANCH_NAME = "TEST-123-branch-name";

    private FreestyleIssueKeyExtractor classUnderTest;

    @Before
    public void setUp() throws Exception {
        classUnderTest = new FreestyleBranchNameIssueKeyExtractor();
    }
    
    @Test
    public void testExtractIssueKeys_whenScmRevisionActionNotNull() {
        // given
        final AbstractBuild build = mock(AbstractBuild.class);
        final BranchSpec branchSpec = new BranchSpec(BRANCH_NAME);
        List<BranchSpec> branchSpecs = new ArrayList<>();
        branchSpecs.add(branchSpec);
        final GitSCM gitSCM = mock(GitSCM.class);
        when(gitSCM.getBranches()).thenReturn(branchSpecs);
        final AbstractProject project = mock(AbstractProject.class);
        when(build.getProject()).thenReturn(project);
        when(build.getProject().getScm()).thenReturn(gitSCM);

        // when
        final Set<String> issueKeys = classUnderTest.extractIssueKeys(build);

        // then
        assertThat(issueKeys).containsExactlyInAnyOrder("TEST-123");
    }

    @Test
    public void testExtractIssueKeys_whenScmRevisionActionNull() {
        // given
    	final AbstractBuild build = mock(AbstractBuild.class);

        // when
        final Set<String> issueKeys = classUnderTest.extractIssueKeys(build);

        // then
        assertThat(issueKeys).isEmpty();
    }

}
