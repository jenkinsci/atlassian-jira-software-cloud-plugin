package com.atlassian.jira.cloud.jenkins.util;

import com.google.common.collect.ImmutableSet;
import com.atlassian.jira.cloud.jenkins.common.model.IssueKey;
import org.apache.commons.lang.StringUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts issue keys (eg. TEST-123) of any number of instances from a given string. Input can be a
 * commit message or a branch name.
 */
public final class IssueKeyExtractor {

    private IssueKeyExtractor() {
        // empty
    }

    private static final String SEPARATOR = "[\\s\\p{Punct}]";
    // zero-width positive lookbehind
    private static final String KEY_PREFIX_REGEX = "(?:(?<=" + SEPARATOR + ")|^)";
    // max of 256 chars in Issue Key project name and 100 for the issue number
    private static final String KEY_BODY_REGEX =
            "(\\p{Lu}[\\p{Lu}\\p{Digit}_]{1,255}-\\p{Digit}{1,100})";
    // zero-width positive lookahead
    private static final String KEY_POSTFIX_REGEX = "(?:(?=" + SEPARATOR + ")|$)";

    private static final String ISSUE_KEY_REGEX =
            KEY_PREFIX_REGEX + KEY_BODY_REGEX + KEY_POSTFIX_REGEX;
    private static final Pattern PROJECT_KEY_PATTERN = Pattern.compile(ISSUE_KEY_REGEX);

    public static final Integer ISSUE_KEY_MAX_LIMIT = 100;

    public static Set<IssueKey> extractIssueKeys(final String text) {
        final Set<IssueKey> matches = new HashSet<>();

        if (StringUtils.isBlank(text)) {
            return Collections.emptySet();
        }

        final Matcher match = PROJECT_KEY_PATTERN.matcher(text);

        while (match.find()) {
            for (int i = 1; i <= match.groupCount(); i++) {
                final String issueKey = match.group(i);
                matches.add(new IssueKey(issueKey));

                if (matches.size() >= ISSUE_KEY_MAX_LIMIT) {
                    return ImmutableSet.copyOf(matches);
                }
            }
        }

        return ImmutableSet.copyOf(matches);
    }
}
