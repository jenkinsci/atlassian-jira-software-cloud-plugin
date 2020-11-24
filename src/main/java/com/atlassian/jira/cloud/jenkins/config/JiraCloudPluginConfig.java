package com.atlassian.jira.cloud.jenkins.config;

import com.atlassian.jira.cloud.jenkins.Messages;
import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Global configuration to store all Jira Software Cloud site settings (site name and the
 * corresponding credentials).
 */
@Extension
public class JiraCloudPluginConfig extends GlobalConfiguration {

    private static final Logger log = LoggerFactory.getLogger(JiraCloudPluginConfig.class);

    private static final String ATL_JSW_GLOBAL_CONFIGURATION_ID = "atl-jsw-global-configuration";

    private List<JiraCloudSiteConfig> sites = new ArrayList<>();

    public JiraCloudPluginConfig() {
        getConfigFile().getXStream().alias("atl-jsw-site-configuration", JiraCloudSiteConfig.class);
        load();
    }

    @Nullable
    public static JiraCloudPluginConfig get() {
        return GlobalConfiguration.all().get(JiraCloudPluginConfig.class);
    }

    @Override
    public boolean configure(final StaplerRequest req, final JSONObject json) throws FormException {
        try {
            // workaround to remove the last site from the list
            if (json != null && json.isEmpty()) {
                setSites(Collections.emptyList());
            }
            req.bindJSON(this, json);
        } catch (Exception e) {
            log.debug("Submitting form to JSW Plugin failed: ({})", e.getMessage(), e);
            if (log.isTraceEnabled()) {
                log.trace("JSW form data: {}", json.toString());
            }
            throw new FormException(
                    String.format("Incorrect JSW Configuration (%s)", e.getMessage()),
                    e,
                    "configs");
        }
        save();

        return true;
    }

    @Override
    public String getId() {
        return ATL_JSW_GLOBAL_CONFIGURATION_ID;
    }

    public List<JiraCloudSiteConfig> getSites() {
        return sites;
    }

    public void setSites(final List<JiraCloudSiteConfig> sites) {
        this.sites = sites;
    }

    public static Optional<JiraCloudSiteConfig> getJiraCloudSiteConfig(
            @Nullable final String site) {
        final Optional<String> userProvidedSite = Optional.ofNullable(site);
        return userProvidedSite
                .map(JiraCloudPluginConfig::filterFromConfig)
                .orElse(defaultFromConfig());
    }

    public static Optional<JiraCloudSiteConfig> filterFromConfig(final String site) {
        return JiraCloudPluginConfig.get()
                .getSites()
                .stream()
                .filter(s -> s.getSite().equals(site))
                .findFirst();
    }

    private static Optional<JiraCloudSiteConfig> defaultFromConfig() {
        final List<JiraCloudSiteConfig> allSites = JiraCloudPluginConfig.get().getSites();
        if (allSites.isEmpty()) {
            log.warn(Messages.JiraCommonResponse_FAILURE_NO_SITE_CONFIG_PRESENT());
            return Optional.empty();
        } else if (allSites.size() > 1) {
            log.warn(Messages.JiraCommonResponse_FAILURE_MULTIPLE_SITE_CONFIGS_PRESENT());
            return Optional.empty();
        } else {
            return Optional.of(allSites.get(0));
        }
    }
}
