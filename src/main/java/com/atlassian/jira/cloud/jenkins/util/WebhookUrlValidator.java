package com.atlassian.jira.cloud.jenkins.util;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Pattern;

public final class WebhookUrlValidator {

    private static final Pattern QUERY_PATTERN = Pattern.compile("^jenkins_server_uuid=.+");

    public static boolean isValid(final String webhookUrl) {
        try {
            new URL(webhookUrl).toURI();
            return true;
        } catch (URISyntaxException | MalformedURLException e) {
            return false;
        }
    }

    public static boolean containsValidQueryParams(final String webhookUrl) {
        try {
            String query = new URL(webhookUrl).toURI().getQuery();
            if (query == null) {
                return false;
            }
            return QUERY_PATTERN.matcher(query).matches();
        } catch (URISyntaxException | MalformedURLException e) {
            return false;
        }
    }
}
