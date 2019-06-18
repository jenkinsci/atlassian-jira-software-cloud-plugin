package com.atlassian.jira.cloud.jenkins.util;

import org.apache.commons.lang.StringUtils;

import java.util.regex.Pattern;

public final class SiteValidator {

    private static final Pattern SITE_NAME_PATTERN =
            Pattern.compile("[a-z0-9\\-]{3,}\\.[a-z0-9\\-]+\\.[a-z]{2,}");

    /**
     * Validates Jira Cloud site name. A valid site name must be at least 3 characters, lowercase
     * letters and numbers only.
     *
     * @param site Jira Cloud site name, e.g. sitename.atlassian.net
     * @return true if site name is valid
     */
    public static boolean isValid(final String site) {
        if (StringUtils.isBlank(site)) {
            return false;
        }
        return SITE_NAME_PATTERN.matcher(site).matches();
    }
}
