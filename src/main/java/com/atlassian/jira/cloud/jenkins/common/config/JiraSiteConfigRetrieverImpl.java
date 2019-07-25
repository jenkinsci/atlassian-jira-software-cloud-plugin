package com.atlassian.jira.cloud.jenkins.common.config;

import com.atlassian.jira.cloud.jenkins.config.JiraCloudPluginConfig;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudSiteConfig;

import javax.annotation.Nullable;
import java.util.Optional;

public class JiraSiteConfigRetrieverImpl implements JiraSiteConfigRetriever {

    @Override
    public Optional<JiraCloudSiteConfig> getJiraSiteConfig(@Nullable final String jiraSite) {
        return JiraCloudPluginConfig.getJiraCloudSiteConfig(jiraSite);
    }
}
