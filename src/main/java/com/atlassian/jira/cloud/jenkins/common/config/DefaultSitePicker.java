package com.atlassian.jira.cloud.jenkins.common.config;

import com.atlassian.jira.cloud.jenkins.Messages;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudPluginConfig;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudSiteConfig;

import javax.annotation.Nullable;
import java.io.PrintStream;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public interface DefaultSitePicker {

    @Nullable
    PrintStream getLogger();

    default Optional<String> pickDefaultSite(@Nullable String siteFromJenkinsfile) {
        final Optional<String> userProvidedSite = Optional.ofNullable(siteFromJenkinsfile);
        return Optional.ofNullable(userProvidedSite.orElseGet(siteFromConfig()));
    }

    default Supplier<String> siteFromConfig() {
        return () -> {
            final List<JiraCloudSiteConfig> allSites = JiraCloudPluginConfig.get().getSites();

            if (allSites.isEmpty()) {
                getLogger().println(Messages.JiraCommonResponse_FAILURE_NO_SITE_CONFIG_PRESENT());
                return null;
            }

            if (allSites.size() > 1) {
                getLogger()
                        .println(Messages.JiraCommonResponse_FAILURE_MULTIPLE_SITE_CONFIGS_PRESENT());
                return null;
            }

            return allSites.get(0).getSite();
        };
    }
}
