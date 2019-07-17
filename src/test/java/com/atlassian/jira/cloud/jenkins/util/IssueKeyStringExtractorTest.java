package com.atlassian.jira.cloud.jenkins.util;

import com.atlassian.jira.cloud.jenkins.common.model.IssueKey;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class IssueKeyStringExtractorTest {

    @Test
    public void testOneIssueKey() {
        final String input = "TEST-123-add-an-awesome-feature";

        final Set<IssueKey> issuesKeys = IssueKeyStringExtractor.extractIssueKeys(input);

        assertThat(issuesKeys).hasSize(1);
        assertThat(issuesKeys).extracting(IssueKey::toString).containsExactlyInAnyOrder("TEST-123");
    }

    @Test
    public void testMultipleIssueKeys() {
        final String input = "TEST-123 TEST-789 Add two awesome features";

        final Set<IssueKey> issuesKeys = IssueKeyStringExtractor.extractIssueKeys(input);

        assertThat(issuesKeys).hasSize(2);
        assertThat(issuesKeys)
                .extracting(IssueKey::toString)
                .containsExactlyInAnyOrder("TEST-123", "TEST-789");
    }

    @Test
    public void testIssueKeysInTheMiddle() {
        final String input = "This commit fixes TEST-89 and TEST-55.";

        Set<IssueKey> issuesKeys = IssueKeyStringExtractor.extractIssueKeys(input);

        assertThat(issuesKeys).hasSize(2);
        assertThat(issuesKeys)
                .extracting(IssueKey::toString)
                .containsExactlyInAnyOrder("TEST-55", "TEST-89");
    }

    @Test
    public void testNoIssueKeys() {
        final String input = "input without any issue key";

        final Set<IssueKey> issuesKeys = IssueKeyStringExtractor.extractIssueKeys(input);

        assertThat(issuesKeys).hasSize(0);
    }

    @Ignore("TODO")
    @Test
    public void testMaxIssueKeysLimit() {}
}
