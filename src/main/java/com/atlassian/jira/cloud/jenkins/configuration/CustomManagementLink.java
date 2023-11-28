package com.atlassian.jira.cloud.jenkins.configuration;

import com.atlassian.jira.cloud.jenkins.common.client.BadRequestException;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudPluginConfig;
import com.atlassian.jira.cloud.jenkins.logging.PipelineLogger;
import com.atlassian.jira.cloud.jenkins.ping.PingApi;
import com.atlassian.jira.cloud.jenkins.pluginConfigApi.PluginConfigApi;
import com.atlassian.jira.cloud.jenkins.pluginConfigApi.PluginConfigResponse;
import com.atlassian.jira.cloud.jenkins.tenantinfo.CloudIdResolver;
import com.atlassian.jira.cloud.jenkins.util.SecretRetriever;
import com.atlassian.jira.cloud.jenkins.util.SiteValidator;
import com.atlassian.jira.cloud.jenkins.util.WebhookUrlValidator;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.model.ManagementLink;
import org.apache.commons.lang.StringUtils;
import org.springframework.security.core.userdetails.User;
import hudson.security.ACL;
import hudson.security.csrf.CrumbIssuer;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.acegisecurity.Authentication;
import org.acegisecurity.context.SecurityContextHolder;


import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static com.atlassian.jira.cloud.jenkins.util.IpAddressProvider.getIpAddress;
import static com.atlassian.jira.cloud.jenkins.Config.ATLASSIAN_API_URL;


@Extension
public class CustomManagementLink extends ManagementLink implements Describable<CustomManagementLink> {

    private static final Logger LOGGER = Logger.getLogger(CustomManagementLink.class.getName());
    private transient PluginConfigApi pluginConfigApi;
    private transient PingApi pingApi;
    private transient SecretRetriever secretRetriever;
    private JiraCloudPluginConfig config;
    private Category category;

    @Inject
    public void setPluginConfigApi(final PluginConfigApi pluginConfigApi) {
        this.pluginConfigApi = pluginConfigApi;
    }

//    @Inject
//    public void setPluginConfigApi(final PingApi pingApi) {
//        this.pingApi = pingApi;
//    }

    @Inject
    public void setSecretRetriever(final SecretRetriever secretRetriever) {
        this.secretRetriever = secretRetriever;
    }

    @DataBoundConstructor
    public CustomManagementLink() {
        super();
        this.config = JiraCloudPluginConfig.get();
        this.category = Category.MISC;
    }

    // TODO - Temporary null return to hide the button during testing
    @Override
    public String getIconFileName() {
        return null;
    }

    public Category getCategory() {
        return category;
    }

    @Override
    public Descriptor<CustomManagementLink> getDescriptor() {
        return new DescriptorImpl();
    }

    // Define the descriptor class
    @Extension
    public static class DescriptorImpl extends Descriptor<CustomManagementLink> {

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

        @Override
        public String getDisplayName() {
            return "Custom Management";
        }

        @RequirePOST
        public ListBoxModel doFillCredentialsIdItems(
                @AncestorInPath final Item item,
                @QueryParameter final String credentialsId
        ) {
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
                        this.pingApi.sendPing(webhookUrl, maybeSecret.get(), PipelineLogger.noopInstance());
                if (!pingSuccess) {
                    return FormValidation.error(
                            "Connection could not be established. Is the secret correct?");
                }
            } catch (BadRequestException e) {
                return FormValidation.error(
                        String.format("Error message from Jira: %s", e.getMessage()));
            } catch (Exception e) {
                LOGGER.severe("Unexpected error when testing connection!");
                LOGGER.severe(e.toString());
                return FormValidation.error("Unexpected error when testing connection!");
            }
            return FormValidation.ok("Successfully validated site credentials");
        }
    }

    @Override
    public String getUrlName() {
        return "customManagement";
    }

    @Override
    public String getDisplayName() {
        return "Custom Management";
    }

    private String generateCrumb(final StaplerRequest req) {
        try {
            Jenkins jenkins = Jenkins.getInstanceOrNull();
            if (jenkins == null) {
                return null;
            }

            CrumbIssuer crumbIssuer = jenkins.getCrumbIssuer();
            if (crumbIssuer == null) {
                return null;
            }

            return crumbIssuer.getCrumb(req);
        } catch (Exception e) {
            LOGGER.warning("Error generating Crumb");
            return null;
        }
    }

    public void doIndex(final StaplerRequest req, final StaplerResponse res) {
        String crumb = generateCrumb(req);
        HttpSession session = req.getSession(true);

        if (crumb != null && session != null) {
            try {
                // Store the crumb value in the session or request attribute
                session.setAttribute("Jenkins-Crumb", crumb); // todo do we need both of these?
                req.setAttribute("crumbValue", crumb); // todo do we need both of these?
                // pass through the config data to access on client side
                req.setAttribute("config", this.config);
                res.setContentType("text/html");
                req.getView(this, "/com/atlassian/jira/cloud/jenkins/configuration/config.jelly").forward(req, res);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void SendConfigDataToJira(final JSONObject formData) {
        // TODO COULD BE AND OBJECT IF SINGLE ITEM
        JSONArray sitesArray = formData.getJSONArray("sites");

        String ipAddress = getIpAddress();

        boolean autoBuildsEnabled = formData.has("autoBuilds");
        String autoBuildsRegex = autoBuildsEnabled ?
                formData.getJSONObject("autoBuilds").optString("autoBuildsRegex", "") :
                "";

        boolean autoDeploymentsEnabled = formData.has("autoDeployments");
        String autoDeploymentsRegex = autoDeploymentsEnabled ?
                formData.getJSONObject("autoDeployments").optString("autoDeploymentsRegex", "") :
                "";

        for (int i = 0; i < sitesArray.size(); i++) {
            JSONObject site = sitesArray.getJSONObject(i);
            String webhookUrl = site.getString("webhookUrl");
            String credentialsId = site.getString("credentialsId");

            final Optional<String> maybeSecret = secretRetriever.getSecretFor(credentialsId);

            try {
                PluginConfigResponse response = this.pluginConfigApi.sendConnectionData(webhookUrl, maybeSecret.get(),
                        ipAddress, autoBuildsEnabled, autoBuildsRegex, autoDeploymentsEnabled, autoDeploymentsRegex,
                        PipelineLogger.noopInstance());

                LOGGER.warning("OH YESSSS");
                LOGGER.warning(response.toString());
            } catch (Exception e) {
                LOGGER.warning("OH NOOOOOOOOOO");
            }
        }
    }

    private void removeInvalidSites(final JSONObject formData) {
        if (formData.has("sites")) {
            Object sites = formData.get("sites");
            if (sites instanceof JSONArray) {
                JSONArray sitesArray = (JSONArray) sites;

                for (int i = 0; i < sitesArray.size(); i++) {
                    JSONObject siteObject = sitesArray.getJSONObject(i);
                    if (siteObject.has("active") && siteObject.optString("active").equals("false")) {
                        sitesArray.remove(i);
                        i--;
                    }
                }
                formData.put("sites", sitesArray);
            }
        }
    }

    @RequirePOST
    public void doSaveConfiguration(final StaplerRequest req, final StaplerResponse res) throws ServletException, IOException, Descriptor.FormException {
        LOGGER.info("SAVE CONFIG HAS BEEN CALLED HURRAY");

        JSONObject formData = req.getSubmittedForm();
        LOGGER.info("PRE TRANSFORM");
        LOGGER.info(formData.toString());
        removeInvalidSites(formData);
        LOGGER.info("POST TRANSFORM");
        LOGGER.info(formData.toString());

//        SendConfigDataToJira(formData);
        CompletableFuture.runAsync(() -> SendConfigDataToJira(formData));

        config.configure(req, formData);
        config.save();

        StaplerResponse response = Stapler.getCurrentResponse();
        response.sendRedirect("/jenkins/manage/customManagement/");
    }

}