package com.atlassian.jira.cloud.jenkins.config;

import com.atlassian.jira.cloud.jenkins.auth.AccessTokenRetriever;
import com.atlassian.jira.cloud.jenkins.common.model.AppCredential;
import com.atlassian.jira.cloud.jenkins.tenantinfo.CloudIdResolver;
import com.atlassian.jira.cloud.jenkins.util.SecretRetriever;
import com.atlassian.jira.cloud.jenkins.util.SiteValidator;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.DoNotUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.inject.Inject;
import java.util.Optional;

import static com.atlassian.jira.cloud.jenkins.Config.ATLASSIAN_API_URL;
import static java.util.Objects.requireNonNull;

/**
 * This class encapsulates Jira Cloud site configuration to be used to send build information to
 * Jira.
 */
public class JiraCloudSiteConfig extends AbstractDescribableImpl<JiraCloudSiteConfig> {

    public static final String DEFAULT_SITE = "sitename.atlassian.net";

    private final String site;
    private final String clientId;
    private final String credentialsId;

    @DataBoundConstructor
    public JiraCloudSiteConfig(
            final String site, final String clientId, final String credentialsId) {
        this.site = requireNonNull(site);
        this.clientId = requireNonNull(clientId);
        this.credentialsId = requireNonNull(credentialsId);
    }

    public String getSite() {
        return site;
    }

    public String getClientId() {
        return clientId;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<JiraCloudSiteConfig> {

        private transient AccessTokenRetriever accessTokenRetriever;
        private transient SecretRetriever secretRetriever;
        private transient CloudIdResolver cloudIdResolver;

        @Inject
        public void setAccessTokenRetriever(final AccessTokenRetriever accessTokenRetriever) {
            this.accessTokenRetriever = accessTokenRetriever;
        }

        @Inject
        public void setSecretRetriever(final SecretRetriever secretRetriever) {
            this.secretRetriever = secretRetriever;
        }

        @Inject
        public void setCloudIdResolver(final CloudIdResolver cloudIdResolver) {
            this.cloudIdResolver = cloudIdResolver;
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

        public FormValidation doCheckClientId(@QueryParameter final String value) {
            if (StringUtils.isEmpty(value)) {
                return FormValidation.error(
                        "Client ID can’t be blank. Paste it from your OAuth credentials in Jira Cloud.");
            }
            return FormValidation.ok();
        }

        @SuppressWarnings("unused")
        public ListBoxModel doFillCredentialsIdItems(@QueryParameter final String credentialsId) {
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

        @RequirePOST
        @Restricted(DoNotUse.class) // WebOnly
        @SuppressWarnings("unused")
        public FormValidation doTestConnection(
                @QueryParameter final String site,
                @QueryParameter final String clientId,
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

            final AppCredential appCredential = new AppCredential(clientId, maybeSecret.get());
            final Optional<String> accessToken = accessTokenRetriever.getAccessToken(appCredential);

            if (!accessToken.isPresent()) {
                return FormValidation.error("Failed to validate site credentials");
            }

            return FormValidation.ok("Successfully validated site credentials");
        }

        @Override
        public String getDisplayName() {
            return "Jira Cloud Site";
        }
    }
}
