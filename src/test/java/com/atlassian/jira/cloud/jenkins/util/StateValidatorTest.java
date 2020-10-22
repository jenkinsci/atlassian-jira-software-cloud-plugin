package com.atlassian.jira.cloud.jenkins.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class StateValidatorTest {

    private static final String ERROR_MESSAGE =
            "The parameter state is not valid. Allowed values are: "
                    + "[unknown, pending, in_progress, cancelled, failed, rolled_back, successful]";

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[][] {
                    {"unknown", Collections.emptyList()},
                    {"pending", Collections.emptyList()},
                    {"in_progress", Collections.emptyList()},
                    {"cancelled", Collections.emptyList()},
                    {"failed", Collections.emptyList()},
                    {"rolled_back", Collections.emptyList()},
                    {"successful", Collections.emptyList()},
                    {"bad input", Collections.singletonList(ERROR_MESSAGE)},
                });
    }

    private String input;
    private List<String> expectedResult;

    public StateValidatorTest(final String input, final List<String> expectedResult) {
        this.input = input;
        this.expectedResult = expectedResult;
    }

    @Test
    public void testIsValid() {
        assertThat(StateValidator.validate(input)).isEqualTo(expectedResult);
    }
}
