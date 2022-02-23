package com.atlassian.jira.cloud.jenkins.common.config;

import com.atlassian.jira.cloud.jenkins.config.JiraCloudSiteConfig2;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public interface JiraSiteConfig2Retriever {

    Optional<JiraCloudSiteConfig2> getJiraSiteConfig(@Nullable String jiraSite);

    List<String> getAllJiraSites();
}
