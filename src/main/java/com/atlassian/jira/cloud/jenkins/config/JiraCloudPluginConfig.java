package com.atlassian.jira.cloud.jenkins.config;

import com.atlassian.jira.cloud.jenkins.Messages;
import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONArray;
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

    public static final String FIELD_NAME_AUTO_BUILDS = "autoBuilds";
    public static final String FIELD_NAME_AUTO_DEPLOYMENTS = "autoDeployments";
    public static final String FIELD_NAME_SITES = "sites";
    public static final String FIELD_NAME_AUTO_BUILDS_REGEX = "autoBuildsRegex";
    public static final String FIELD_NAME_AUTO_DEPLOYMENTS_REGEX = "autoDeploymentsRegex";

    public static final String FIELD_NAME_DEBUG_LOGGING = "debugLogging";

    private static final Logger log = LoggerFactory.getLogger(JiraCloudPluginConfig.class);

    private static final String ATL_JSW_GLOBAL_CONFIGURATION_ID = "atl-jsw-global-configuration";

    private List<JiraCloudSiteConfig> sites = new ArrayList<>();

    private Boolean autoBuildsEnabled;
    private String autoBuildsRegex;

    private Boolean debugLogging = Boolean.FALSE;

    private Boolean autoDeploymentsEnabled;
    private String autoDeploymentsRegex = "^deploy to (?<envName>.*)$";

    public JiraCloudPluginConfig() {
        getConfigFile().getXStream().alias("atl-jsw-site-configuration", JiraCloudSiteConfig.class);
        load();
    }

    // Only for testing
    JiraCloudPluginConfig(final String testName) {
        getConfigFile().getXStream().alias(testName, JiraCloudSiteConfig.class);
        load();
    }

    @Nullable
    public static JiraCloudPluginConfig get() {
        return GlobalConfiguration.all().get(JiraCloudPluginConfig.class);
    }

    public static boolean isDebugLoggingEnabled() {
        JiraCloudPluginConfig config = get();
        if (config == null) {
            return false;
        }
        return config.getDebugLogging();
    }

    @Override
    public boolean configure(final StaplerRequest req, final JSONObject json) throws FormException {
        try {
            setSites(Collections.emptyList());

            if (json.containsKey(FIELD_NAME_SITES)) {

                Object sites = json.get(FIELD_NAME_SITES);

                // we have a single site
                if (sites instanceof JSONObject) {
                    this.sites =
                            Collections.singletonList(
                                    req.bindJSON(JiraCloudSiteConfig.class, (JSONObject) sites));
                }

                // we have multiple sites
                if (sites instanceof JSONArray) {
                    this.sites = req.bindJSONToList(JiraCloudSiteConfig.class, sites);
                }
            }

            this.autoBuildsEnabled = json.containsKey(FIELD_NAME_AUTO_BUILDS);
            if (this.autoBuildsEnabled) {
                this.autoBuildsRegex =
                        json.getJSONObject(FIELD_NAME_AUTO_BUILDS)
                                .getString(FIELD_NAME_AUTO_BUILDS_REGEX);
            }

            this.autoDeploymentsEnabled = json.containsKey(FIELD_NAME_AUTO_DEPLOYMENTS);
            if (this.autoDeploymentsEnabled) {
                this.autoDeploymentsRegex =
                        json.getJSONObject(FIELD_NAME_AUTO_DEPLOYMENTS)
                                .getString(FIELD_NAME_AUTO_DEPLOYMENTS_REGEX);
                if (this.autoDeploymentsRegex == null
                        || this.autoDeploymentsRegex.trim().isEmpty()) {
                    throw new FormException(
                            "Deployments RegEx must be provided!",
                            FIELD_NAME_AUTO_DEPLOYMENTS_REGEX);
                }
            }

            if (json.containsKey(FIELD_NAME_DEBUG_LOGGING)) {
                this.debugLogging = json.getBoolean(FIELD_NAME_DEBUG_LOGGING);
            }

        } catch (Exception e) {
            log.debug("Submitting form to Atlassian Cloud plugin failed: ({})", e.getMessage(), e);
            if (log.isTraceEnabled()) {
                log.trace("Atlassian Cloud plugin data: {}", json.toString());
            }
            throw new FormException(
                    String.format(
                            "Incorrect Atlassian Cloud plugin configuration (%s)", e.getMessage()),
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

    public void setAutoBuildsEnabled(final boolean autoBuildsEnabled) {
        this.autoBuildsEnabled = autoBuildsEnabled;
    }

    public void setAutoBuildsRegex(@Nullable final String autoBuildsRegex) {
        this.autoBuildsRegex = autoBuildsRegex;
    }

    public Boolean getDebugLogging() {
        return debugLogging;
    }

    public void setDebugLogging(final Boolean debugLogging) {
        this.debugLogging = debugLogging;
    }

    public void setAutoDeploymentsRegex(final String autoDeploymentsRegex) {
        this.autoDeploymentsRegex = autoDeploymentsRegex;
    }

    public void setAutoDeploymentsEnabled(final boolean autoDeploymentsEnabled) {
        this.autoDeploymentsEnabled = autoDeploymentsEnabled;
    }

    public static Optional<JiraCloudSiteConfig> getJiraCloudSiteConfig(
            @Nullable final String site) {
        final Optional<String> userProvidedSite = Optional.ofNullable(site);
        return userProvidedSite
                .map(JiraCloudPluginConfig::filterFromConfig)
                .orElseGet(JiraCloudPluginConfig::defaultFromConfig);
    }

    public static Optional<JiraCloudSiteConfig> filterFromConfig(final String site) {
        return JiraCloudPluginConfig.get()
                .getSites()
                .stream()
                .filter(s -> s.getSite().equals(site))
                .findFirst();
    }

    public static List<JiraCloudSiteConfig> getAllSites() {
        return JiraCloudPluginConfig.get().getSites();
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

    public boolean getAutoBuildsEnabled() {
        return Optional.ofNullable(autoBuildsEnabled).orElse(false);
    }

    public String getAutoBuildsRegex() {
        return Optional.ofNullable(autoBuildsRegex).orElse("");
    }

    public boolean getAutoDeploymentsEnabled() {
        return Optional.ofNullable(autoDeploymentsEnabled).orElse(false);
    }

    public String getAutoDeploymentsRegex() {
        return Optional.ofNullable(autoDeploymentsRegex).orElse("");
    }
}
