package com.atlassian.jira.cloud.jenkins.common.client.model;

/** This object represents provider metadata, i.e. the source/system the data is coming from. */
public class ProviderMetadata {

    public String getProduct() {
        return "jenkins";
    }
}
