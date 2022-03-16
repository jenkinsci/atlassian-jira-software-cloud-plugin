package com.atlassian.jira.cloud.jenkins.buildinfo.client.model;

import com.atlassian.jira.cloud.jenkins.common.client.JiraRequest;
import com.atlassian.jira.cloud.jenkins.common.client.model.Properties;
import com.atlassian.jira.cloud.jenkins.common.client.model.ProviderMetadata;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

/** This represents the payload for the API request to submit list of builds */
public class Builds implements JiraRequest {
    @JsonIgnore private JiraBuildInfo build;
    private Properties properties;
    private ProviderMetadata providerMetadata;

    public Builds(final JiraBuildInfo jiraBuildInfo) {
        this.build = jiraBuildInfo;
        this.properties = new Properties();
        this.providerMetadata = new ProviderMetadata();
    }

    public JiraBuildInfo getBuild() {
        return build;
    }

    @JsonProperty("builds")
    public List<JiraBuildInfo> getBuilds() {
        return Collections.singletonList(build);
    }

    public Properties getProperties() {
        return properties;
    }

    public ProviderMetadata getProviderMetadata() {
        return providerMetadata;
    }
}
