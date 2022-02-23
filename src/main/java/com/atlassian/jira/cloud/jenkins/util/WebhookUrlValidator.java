package com.atlassian.jira.cloud.jenkins.util;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

public final class WebhookUrlValidator {

    public static boolean isValid(final String webhookUrl) {
        try {
            new URL(webhookUrl).toURI();
            return true;
        } catch (URISyntaxException | MalformedURLException e) {
            return false;
        }
    }
}
