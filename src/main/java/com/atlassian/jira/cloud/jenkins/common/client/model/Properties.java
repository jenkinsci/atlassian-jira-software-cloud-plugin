package com.atlassian.jira.cloud.jenkins.common.client.model;

/**
 * Metadata properties that can be attached to a build or a deployment
 * when submitting to Jira. This can be used to bulk delete items when needed.
 */
public class Properties {

    public String getSource() {
        return "jenkins";
    }

}
