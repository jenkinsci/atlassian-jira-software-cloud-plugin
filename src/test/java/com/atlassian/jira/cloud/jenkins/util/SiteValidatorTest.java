package com.atlassian.jira.cloud.jenkins.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class SiteValidatorTest {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[][] {
                    {"sitename.atlassian.net", true},
                    {"site-name.atlassian.net", true},
                    {"sitename0.atlassian.net", true},
                    {"1sitename.atlassian.net", true},
                    {"1site2name.atlassian.net", true},
                    {"site-name-0.atlassian.net", true},
                    {null, false},
                    {"", false},
                    {"fo.atlassain.net", false},
                    {"FOO.atlassain.net", false},
                    {"foo", false},
                    {"foo.bar", false},
                    {"dksdkhsdhdsj", false}
                });
    }

    private String input;
    private boolean expectedResult;

    public SiteValidatorTest(final String input, final boolean expectedResult) {
        this.input = input;
        this.expectedResult = expectedResult;
    }

    @Test
    public void testIsValid() {
        assertThat(SiteValidator.isValid(input)).isEqualTo(expectedResult);
    }
}
