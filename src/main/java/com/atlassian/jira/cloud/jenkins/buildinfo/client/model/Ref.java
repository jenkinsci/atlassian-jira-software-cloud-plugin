package com.atlassian.jira.cloud.jenkins.buildinfo.client.model;

/**
 * The reference to the repository the commits are contained
 */
public class Ref {
    private String name;
    private String uri;

    public String getName() {
        return name;
    }

    public String getUri() {
        return uri;
    }

    public Ref setUri(final String uri) {
        this.uri = uri;
        return this;
    }

    public Ref setName(final String name) {
        this.name = name;
        return this;
    }
}
