package com.atlassian.jira.cloud.jenkins.buildinfo.service;

import com.atlassian.jira.cloud.jenkins.auth.AccessTokenRetriever;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.BuildPayloadBuilder;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.BuildApiResponse;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.Builds;
import com.atlassian.jira.cloud.jenkins.common.client.JiraApi;
import com.atlassian.jira.cloud.jenkins.common.config.JiraSiteConfigRetriever;
import com.atlassian.jira.cloud.jenkins.common.model.AppCredential;
import com.atlassian.jira.cloud.jenkins.common.response.JiraCommonResponse;
import com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse;
import com.atlassian.jira.cloud.jenkins.common.service.IssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudSiteConfig;
import com.atlassian.jira.cloud.jenkins.tenantinfo.CloudIdResolver;
import com.atlassian.jira.cloud.jenkins.util.RunWrapperProvider;
import com.atlassian.jira.cloud.jenkins.util.SecretRetriever;
import hudson.model.Run;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of JiraBuildInfoSender to send build updates to Jira by building the payload,
 * generating the access token, sending the request and parsing the response.
 */
public class JiraBuildInfoSenderImpl implements JiraBuildInfoSender {

    private static final Logger log = LoggerFactory.getLogger(JiraBuildInfoSenderImpl.class);

    private static final String HTTPS_PROTOCOL = "https://";

    private final JiraSiteConfigRetriever siteConfigRetriever;
    private final SecretRetriever secretRetriever;
    private final IssueKeyExtractor issueKeyExtractor;
    private final CloudIdResolver cloudIdResolver;
    private final AccessTokenRetriever accessTokenRetriever;
    private final JiraApi buildsApi;
    private final RunWrapperProvider runWrapperProvider;

    public JiraBuildInfoSenderImpl(
            final JiraSiteConfigRetriever siteConfigRetriever,
            final SecretRetriever secretRetriever,
            final IssueKeyExtractor issueKeyExtractor,
            final CloudIdResolver cloudIdResolver,
            final AccessTokenRetriever accessTokenRetriever,
            final JiraApi buildsApi,
            final RunWrapperProvider runWrapperProvider) {
        this.siteConfigRetriever = requireNonNull(siteConfigRetriever);
        this.secretRetriever = requireNonNull(secretRetriever);
        this.issueKeyExtractor = requireNonNull(issueKeyExtractor);
        this.cloudIdResolver = requireNonNull(cloudIdResolver);
        this.accessTokenRetriever = requireNonNull(accessTokenRetriever);
        this.buildsApi = requireNonNull(buildsApi);
        this.runWrapperProvider = requireNonNull(runWrapperProvider);
    }

    @Override
    public JiraSendInfoResponse sendBuildInfo(final JiraBuildInfoRequest request) {
        final String jiraSite = request.getSite();
        final WorkflowRun build = request.getBuild();

        final Optional<JiraCloudSiteConfig> maybeSiteConfig = getSiteConfigFor(jiraSite);

        if (!maybeSiteConfig.isPresent()) {
            return JiraCommonResponse.failureSiteConfigNotFound(jiraSite);
        }

        final JiraCloudSiteConfig siteConfig = maybeSiteConfig.get();
        final Optional<String> maybeSecret = getSecretFor(siteConfig.getCredentialsId());

        if (!maybeSecret.isPresent()) {
            return JiraCommonResponse.failureSecretNotFound(jiraSite);
        }

        final Set<String> issueKeys = issueKeyExtractor.extractIssueKeys(build);

        if (issueKeys.isEmpty()) {
            return JiraBuildInfoResponse.skippedIssueKeysNotFound();
        }

        final Optional<String> maybeCloudId = getCloudIdFor(jiraSite);

        if (!maybeCloudId.isPresent()) {
            return JiraCommonResponse.failureSiteNotFound(jiraSite);
        }

        final Optional<String> maybeAccessToken = getAccessTokenFor(siteConfig, maybeSecret.get());

        if (!maybeAccessToken.isPresent()) {
            return JiraCommonResponse.failureAccessToken(jiraSite);
        }

        final Builds buildInfo = createJiraBuildInfo(build, issueKeys);

        return sendBuildInfo(maybeCloudId.get(), maybeAccessToken.get(), jiraSite, buildInfo)
                .map(response -> handleBuildApiResponse(jiraSite, response))
                .orElseGet(() -> handleBuildApiError(jiraSite));
    }

    private Optional<JiraCloudSiteConfig> getSiteConfigFor(final String jiraSite) {
        return siteConfigRetriever.getJiraSiteConfig(jiraSite);
    }

    private Optional<String> getCloudIdFor(final String jiraSite) {
        final String jiraSiteUrl = HTTPS_PROTOCOL + jiraSite;
        return cloudIdResolver.getCloudId(jiraSiteUrl);
    }

    private Optional<String> getAccessTokenFor(
            final JiraCloudSiteConfig siteConfig, final String secret) {
        final AppCredential appCredential = new AppCredential(siteConfig.getClientId(), secret);
        return accessTokenRetriever.getAccessToken(appCredential);
    }

    private Optional<String> getSecretFor(final String credentialsId) {
        return secretRetriever.getSecretFor(credentialsId);
    }

    private Builds createJiraBuildInfo(final Run build, final Set<String> issueKeys) {
        final RunWrapper buildWrapper = runWrapperProvider.getWrapper(build);

        return BuildPayloadBuilder.getBuildPayload(buildWrapper, issueKeys);
    }

    private Optional<BuildApiResponse> sendBuildInfo(
            final String cloudId,
            final String accessToken,
            final String jiraSite,
            final Builds buildInfo) {
        return buildsApi.postUpdate(
                cloudId, accessToken, jiraSite, buildInfo, BuildApiResponse.class);
    }

    private JiraBuildInfoResponse handleBuildApiResponse(
            final String jiraSite, final BuildApiResponse response) {
        if (!response.getAcceptedBuilds().isEmpty()) {
            return JiraBuildInfoResponse.successBuildAccepted(jiraSite, response);
        }

        if (!response.getRejectedBuilds().isEmpty()) {
            return JiraBuildInfoResponse.failureBuildRejected(jiraSite, response);
        }

        if (!response.getUnknownIssueKeys().isEmpty()) {
            return JiraBuildInfoResponse.failureUnknownIssueKeys(jiraSite, response);
        }

        return JiraBuildInfoResponse.failureUnexpectedResponse();
    }

    private JiraBuildInfoResponse handleBuildApiError(final String jiraSite) {
        return JiraBuildInfoResponse.failureBuildsApiResponse(jiraSite);
    }
}
