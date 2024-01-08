package com.atlassian.jira.cloud.jenkins.config;

import com.atlassian.jira.cloud.jenkins.common.client.BadRequestException;
import com.atlassian.jira.cloud.jenkins.exceptions.JiraConnectionFailedException;
import com.atlassian.jira.cloud.jenkins.logging.PipelineLogger;
import com.atlassian.jira.cloud.jenkins.ping.PingApi;
import com.atlassian.jira.cloud.jenkins.pluginConfigApi.PluginConfigApi;
import com.atlassian.jira.cloud.jenkins.tenantinfo.CloudIdResolver;
import com.atlassian.jira.cloud.jenkins.util.SecretRetriever;
import com.atlassian.jira.cloud.jenkins.util.SiteValidator;
import com.atlassian.jira.cloud.jenkins.util.WebhookUrlValidator;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.google.common.annotations.VisibleForTesting;
import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.ManagementLink;
import org.apache.commons.lang.StringUtils;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.inject.Inject;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.atlassian.jira.cloud.jenkins.util.IpAddressProvider.getIpAddress;
import static com.atlassian.jira.cloud.jenkins.Config.ATLASSIAN_API_URL;

@Extension
public class ConfigManagementLink extends ManagementLink
        implements Describable<ConfigManagementLink> {

    private static final Logger LOGGER = Logger.getLogger(ConfigManagementLink.class.getName());
    private transient PluginConfigApi pluginConfigApi;
    private transient SecretRetriever secretRetriever;
    private JiraCloudPluginConfig config;
    private Category category;
    private static final String AUTO_BUILDS = "autoBuilds";
    private static final String AUTO_DEPLOYMENTS = "autoDeployments";
    private static final String WEBHOOK_URL = "webhookUrl";
    private static final String CREDENTIALS_ID = "credentialsId";
    private static final String SITES = "sites";
    private static final String SITE = "site";
    private static final String ACTIVE = "active";

    @Inject
    public void setPluginConfigApi(final PluginConfigApi pluginConfigApi) {
        this.pluginConfigApi = pluginConfigApi;
    }

    @Inject
    public void setSecretRetriever(final SecretRetriever secretRetriever) {
        this.secretRetriever = secretRetriever;
    }

    @DataBoundConstructor
    public ConfigManagementLink() {
        super();
        this.config = JiraCloudPluginConfig.get();
        this.category = Category.CONFIGURATION;
    }

    @Override
    public Descriptor<ConfigManagementLink> getDescriptor() {
        return new DescriptorImpl();
    }

    public Category getCategory() {
        return category;
    }

    @Override
    public String getIconFileName() {
        return "notepad.png";
    }

    @Override
    public String getUrlName() {
        return "atlassian-jira-software-cloud";
    }

    @Override
    public String getDisplayName() {
        return "Atlassian Jira Software Cloud";
    }

    public void doIndex(final StaplerRequest req, final StaplerResponse res) {
        try {
            req.setAttribute("config", this.config);
            res.setContentType("text/html");
            req.getView(this, "/com/atlassian/jira/cloud/jenkins/configuration/config.jelly")
                    .forward(req, res);
        } catch (Exception e) {
            LOGGER.severe(e.getMessage());
        }
    }

    public String getRegexFromFormData(final JSONObject formData, final String key) {
        if (!formData.has(key)) {
            return "";
        }
        return formData.getJSONObject(key).optString(key + "Regex", "");
    }

    @VisibleForTesting
    private void sendConfigDataToJira(final JSONObject formData) throws JiraConnectionFailedException {
        JSONArray sitesArray = formData.getJSONArray(SITES);
        String ipAddress = getIpAddress();

        boolean autoBuildsEnabled = formData.has(AUTO_BUILDS);
        boolean autoDeploymentsEnabled = formData.has(AUTO_DEPLOYMENTS);
        String autoBuildsRegex = getRegexFromFormData(formData, AUTO_BUILDS);
        String autoDeploymentsRegex = getRegexFromFormData(formData, AUTO_DEPLOYMENTS);

        for (Object siteObject : sitesArray) {
            JSONObject site = (JSONObject) siteObject;
            String siteName = site.getString(SITE);
            String webhookUrl = site.getString(WEBHOOK_URL);
            String credentialsId = site.getString(CREDENTIALS_ID);

            final Optional<String> maybeSecret = secretRetriever.getSecretFor(credentialsId);
            try {
                this.pluginConfigApi.sendConnectionData(
                        webhookUrl,
                        maybeSecret.get(),
                        ipAddress,
                        autoBuildsEnabled,
                        autoBuildsRegex,
                        autoDeploymentsEnabled,
                        autoDeploymentsRegex,
                        PipelineLogger.noopInstance());
            } catch (Exception e) {
                String exceptionClass = e.getClass().getName();
                throw new JiraConnectionFailedException(
                        String.format(
                                "%s - Connection failed for site: %s", exceptionClass, siteName));
            }
        }
    }

    // Incomplete sites or Deleted sites are marked with active=false on the client side, we want to
    // remove them from the JSON object
    @VisibleForTesting
    JSONObject removeInvalidSites(final JSONObject formData) {
        if (formData.has(SITES)) {
            Object sites = formData.get(SITES);
            if (sites instanceof JSONArray) {
                JSONArray sitesArray = (JSONArray) sites;

                for (int i = 0; i < sitesArray.size(); i++) {
                    JSONObject siteObject = sitesArray.getJSONObject(i);
                    if (siteObject.has(ACTIVE) && siteObject.optString(ACTIVE).equals("false")) {
                        sitesArray.remove(i);
                        i--;
                    }
                }
                formData.put(SITES, sitesArray);
            }
        }
        return formData;
    }

    @RequirePOST
    public void doSaveConfiguration(final StaplerRequest req, final StaplerResponse res)
            throws ServletException, IOException, Descriptor.FormException {
        JSONObject formData = req.getSubmittedForm();
        JSONObject transformedFormData = removeInvalidSites(formData);

        // Save form data first to avoid losing data
        try {
            config.configure(req, transformedFormData);
            config.save();
        } catch (Exception e) {
            LOGGER.severe("Failed to submit form, please try again." + e.getMessage());
            req.setAttribute("error", e.getMessage());
            doIndex(req, res);
            return;
        }

        // validate connection data
        try {
            sendConfigDataToJira(transformedFormData);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Connection to Jira site failed", e);
            req.setAttribute("error", e.getMessage());
            doIndex(req, res);
            return;
        }
        res.sendRedirect("/jenkins/manage/");
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ConfigManagementLink> {
        private transient SecretRetriever secretRetriever;
        private transient PingApi pingApi;
        private transient CloudIdResolver cloudIdResolver;

        @Inject
        public void setSecretRetriever(final SecretRetriever secretRetriever) {
            this.secretRetriever = secretRetriever;
        }

        @Inject
        public void setPingApi(final PingApi pingApi) {
            this.pingApi = pingApi;
        }

        @Inject
        public void setCloudIdResolver(final CloudIdResolver cloudIdResolver) {
            this.cloudIdResolver = cloudIdResolver;
        }

        @RequirePOST
        public ListBoxModel doFillCredentialsIdItems(
                @AncestorInPath final Item item, @QueryParameter final String credentialsId) {
            Jenkins instance = Jenkins.get();
            if (!instance.hasPermission(Jenkins.ADMINISTER)) {
                return new StandardListBoxModel().includeCurrentValue(credentialsId);
            }

            return new StandardListBoxModel()
                    .includeEmptyValue()
                    .includeMatchingAs(
                            ACL.SYSTEM,
                            instance,
                            StringCredentials.class,
                            URIRequirementBuilder.fromUri(ATLASSIAN_API_URL).build(),
                            CredentialsMatchers.always());
        }

        public FormValidation doCheckSite(@QueryParameter final String value) {
            if (StringUtils.isEmpty(value)) {
                return FormValidation.error(
                        "Site name can't be empty. Paste your Jira Cloud site name here.");
            }

            if (!SiteValidator.isValid(value)) {
                return FormValidation.error(
                        "Site name is invalid. Paste a valid site name, e.g. sitename.atlassian.net.");
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckWebhookUrl(@QueryParameter final String value) {
            if (StringUtils.isEmpty(value)) {
                return FormValidation.error(
                        "Webhook URL can’t be blank. Paste it from the Jenkins app in Jira.");
            }

            if (!WebhookUrlValidator.isValid(value)) {
                return FormValidation.error("Webhook URL is not a valid URL.");
            }

            if (!WebhookUrlValidator.containsValidQueryParams(value)) {
                return FormValidation.error(
                        "Webhook URL needs to contain query parameter 'jenkins_server_uuid'.");
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckAutoBuildsRegex(@QueryParameter final String value) {
            try {
                Pattern.compile(value);
                return FormValidation.ok();
            } catch (PatternSyntaxException e) {
                return FormValidation.error("Invalid regular expression: " + e.getDescription());
            }
        }

        public FormValidation doCheckAutoDeploymentsRegex(@QueryParameter final String value) {
            if (StringUtils.isEmpty(value)) {
                return FormValidation.error("Deployment regex cannot be empty.");
            }

            try {
                Pattern.compile(value);
                return FormValidation.ok();
            } catch (PatternSyntaxException e) {
                return FormValidation.error("Invalid regular expression: " + e.getDescription());
            }
        }

        @RequirePOST
        public FormValidation doTestConnection(
                @QueryParameter final String site,
                @QueryParameter final String webhookUrl,
                @QueryParameter final String credentialsId) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);

            final Optional<String> maybeCloudId = cloudIdResolver.getCloudId("https://" + site);

            if (!maybeCloudId.isPresent()) {
                return FormValidation.error("Failed to resolve Jira Cloud site: " + site);
            }

            final Optional<String> maybeSecret = secretRetriever.getSecretFor(credentialsId);
            if (!maybeSecret.isPresent()) {
                return FormValidation.error("Failed to retrieve secret");
            }
            try {
                boolean pingSuccess =
                        pingApi.sendPing(
                                webhookUrl, maybeSecret.get(), PipelineLogger.noopInstance());
                if (!pingSuccess) {
                    return FormValidation.error(
                            "Connection could not be established. Is the secret correct?");
                }
            } catch (BadRequestException e) {
                return FormValidation.error(
                        String.format("Error message from Jira: %s", e.getMessage()));
            } catch (Exception e) {
                return FormValidation.error("Unexpected error when testing connection!");
            }
            return FormValidation.ok("Successfully validated site credentials");
        }
    }
}
