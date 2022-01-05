package com.atlassian.jira.cloud.jenkins.buildinfo.service;

import static java.util.Objects.requireNonNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.cloud.jenkins.auth.AccessTokenRetriever;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.BuildApiResponse;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.Builds;
import com.atlassian.jira.cloud.jenkins.common.client.JiraApi;
import com.atlassian.jira.cloud.jenkins.common.client.PostUpdateResult;
import com.atlassian.jira.cloud.jenkins.common.config.JiraSiteConfigRetriever;
import com.atlassian.jira.cloud.jenkins.common.model.AppCredential;
import com.atlassian.jira.cloud.jenkins.common.response.JiraCommonResponse;
import com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudSiteConfig;
import com.atlassian.jira.cloud.jenkins.tenantinfo.CloudIdResolver;
import com.atlassian.jira.cloud.jenkins.util.Constants;
import com.atlassian.jira.cloud.jenkins.util.RunWrapperProvider;
import com.atlassian.jira.cloud.jenkins.util.SecretRetriever;

/**
 * Implementation of JiraBuildInfoSender to send build updates to Jira by building the payload,
 * generating the access token, sending the request and parsing the response.
 */
public abstract class JiraBuildInfoSenderImpl implements JiraBuildInfoSender {

    private static final Logger log = LoggerFactory.getLogger(JiraBuildInfoSenderImpl.class);

    private final JiraSiteConfigRetriever siteConfigRetriever;
    private final SecretRetriever secretRetriever;

    private final CloudIdResolver cloudIdResolver;
    private final AccessTokenRetriever accessTokenRetriever;
    private final JiraApi buildsApi;
    protected final RunWrapperProvider runWrapperProvider;

    public JiraBuildInfoSenderImpl(
            final JiraSiteConfigRetriever siteConfigRetriever,
            final SecretRetriever secretRetriever,
            final CloudIdResolver cloudIdResolver,
            final AccessTokenRetriever accessTokenRetriever,
            final JiraApi buildsApi,
            final RunWrapperProvider runWrapperProvider) {
        this.siteConfigRetriever = requireNonNull(siteConfigRetriever);
        this.secretRetriever = requireNonNull(secretRetriever);
        this.cloudIdResolver = requireNonNull(cloudIdResolver);
        this.accessTokenRetriever = requireNonNull(accessTokenRetriever);
        this.buildsApi = requireNonNull(buildsApi);
        this.runWrapperProvider = requireNonNull(runWrapperProvider);
    }

    @Override
    public List<JiraSendInfoResponse> sendBuildInfo(final JiraBuildInfoRequest request) {
        final List<JiraSendInfoResponse> responses = new LinkedList<>();
        if (request.getSite() == null) {
            List<String> jiraSites = siteConfigRetriever.getAllJiraSites();
            for (final String jiraSite : jiraSites) {
                final Optional<JiraCloudSiteConfig> maybeSiteConfig = getSiteConfigFor(jiraSite);

                responses.add(
                        maybeSiteConfig
                                .map(siteConfig -> sendBuildInfoToJiraSite(siteConfig, request))
                                .orElse(JiraCommonResponse.failureSiteConfigNotFound(jiraSite))
                );
            }
        } else {
            final Optional<JiraCloudSiteConfig> maybeSiteConfig = getSiteConfigFor(request.getSite());
            responses.add(
                    maybeSiteConfig
                            .map(siteConfig -> sendBuildInfoToJiraSite(siteConfig, request))
                            .orElse(JiraCommonResponse.failureSiteConfigNotFound(request.getSite()))
            );
        }
        return responses;
    }

    /**
     * Sends build data to the Jira site.
     *
     * @param siteConfig - Jira to send data to
     * @param request - JiraBuildInfoRequest::site is ignored and jiraSite is used instead
     */
    public JiraSendInfoResponse sendBuildInfoToJiraSite(
            @Nonnull final JiraCloudSiteConfig siteConfig, @Nonnull final JiraBuildInfoRequest request) {

        final String resolvedSiteConfig = siteConfig.getSite();

        final Optional<String> maybeSecret = getSecretFor(siteConfig.getCredentialsId());

        if (!maybeSecret.isPresent()) {
            return JiraCommonResponse.failureSecretNotFound(resolvedSiteConfig);
        }

        final Set<String> issueKeys = getIssueKeys(request);

        if (issueKeys.isEmpty()) {
            return JiraBuildInfoResponse.skippedIssueKeysNotFound(siteConfig.getSite());
        }

        final Optional<String> maybeCloudId = getCloudIdFor(resolvedSiteConfig);

        if (!maybeCloudId.isPresent()) {
            return JiraCommonResponse.failureSiteNotFound(resolvedSiteConfig);
        }

        final Optional<String> maybeAccessToken = getAccessTokenFor(siteConfig, maybeSecret.get());

        if (!maybeAccessToken.isPresent()) {
            return JiraCommonResponse.failureAccessToken(resolvedSiteConfig);
        }

        final Builds buildInfo = createJiraBuildInfo(request, issueKeys);

        final PostUpdateResult<BuildApiResponse> postUpdateResult =
                sendBuildInfo(
                        maybeCloudId.get(), maybeAccessToken.get(), resolvedSiteConfig, buildInfo);

        if (postUpdateResult.getResponseEntity().isPresent()) {
            return handleBuildApiResponse(
                    resolvedSiteConfig, postUpdateResult.getResponseEntity().get());
        } else {
            final String errorMessage = postUpdateResult.getErrorMessage().orElse("");
            return handleBuildApiError(resolvedSiteConfig, errorMessage);
        }
    }

    protected abstract Set<String> getIssueKeys(final JiraBuildInfoRequest request);

    private Optional<JiraCloudSiteConfig> getSiteConfigFor(@Nullable final String jiraSite) {
        return siteConfigRetriever.getJiraSiteConfig(jiraSite);
    }

    private Optional<String> getCloudIdFor(final String jiraSite) {
        final String jiraSiteUrl = Constants.HTTPS_PROTOCOL + jiraSite;
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

    protected abstract Builds createJiraBuildInfo(
            final JiraBuildInfoRequest request, final Set<String> issueKeys);

    private PostUpdateResult<BuildApiResponse> sendBuildInfo(
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

        return JiraBuildInfoResponse.failureUnexpectedResponse(jiraSite);
    }

    private JiraBuildInfoResponse handleBuildApiError(
            final String jiraSite, final String errorMessage) {
        return JiraBuildInfoResponse.failureBuildsApiResponse(jiraSite, errorMessage);
    }
}
