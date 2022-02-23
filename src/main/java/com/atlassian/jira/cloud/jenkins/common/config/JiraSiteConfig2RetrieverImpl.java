package com.atlassian.jira.cloud.jenkins.common.config;

import com.atlassian.jira.cloud.jenkins.config.JiraCloudPluginConfig;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudSiteConfig;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudSiteConfig2;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class JiraSiteConfig2RetrieverImpl implements JiraSiteConfig2Retriever {

    @Override
    public Optional<JiraCloudSiteConfig2> getJiraSiteConfig(@Nullable final String jiraSite) {
        return JiraCloudPluginConfig.getJiraCloudSiteConfig2(jiraSite);
    }

    @Override
    public List<String> getAllJiraSites() {
        return JiraCloudPluginConfig.getAllSites2()
                .stream()
                .map(JiraCloudSiteConfig2::getSite)
                .collect(Collectors.toList());
    }
}
