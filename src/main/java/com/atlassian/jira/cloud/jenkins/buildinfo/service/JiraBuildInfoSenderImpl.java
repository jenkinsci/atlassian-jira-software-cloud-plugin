package com.atlassian.jira.cloud.jenkins.buildinfo.service;

import com.atlassian.jira.cloud.jenkins.buildinfo.client.BuildsApi;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.BuildApiResponse;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.Builds;
import com.atlassian.jira.cloud.jenkins.common.client.ApiUpdateFailedException;
import com.atlassian.jira.cloud.jenkins.common.config.JiraSiteConfigRetriever;
import com.atlassian.jira.cloud.jenkins.common.response.JiraCommonResponse;
import com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudSiteConfig;
import com.atlassian.jira.cloud.jenkins.logging.PipelineLogger;
import com.atlassian.jira.cloud.jenkins.tenantinfo.CloudIdResolver;
import com.atlassian.jira.cloud.jenkins.util.Constants;
import com.atlassian.jira.cloud.jenkins.util.RunWrapperProvider;
import com.atlassian.jira.cloud.jenkins.util.SecretRetriever;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of JiraBuildInfoSender to send build updates to Jira by building the payload,
 * generating the access token, sending the request and parsing the response.
 */
public abstract class JiraBuildInfoSenderImpl implements JiraBuildInfoSender {

    private final JiraSiteConfigRetriever siteConfigRetriever;
    private final SecretRetriever secretRetriever;

    private final CloudIdResolver cloudIdResolver;
    private final BuildsApi buildsApi;
    protected final RunWrapperProvider runWrapperProvider;

    public JiraBuildInfoSenderImpl(
            final JiraSiteConfigRetriever siteConfigRetriever,
            final SecretRetriever secretRetriever,
            final CloudIdResolver cloudIdResolver,
            final BuildsApi buildsApi,
            final RunWrapperProvider runWrapperProvider) {
        this.siteConfigRetriever = requireNonNull(siteConfigRetriever);
        this.secretRetriever = requireNonNull(secretRetriever);
        this.cloudIdResolver = requireNonNull(cloudIdResolver);
        this.buildsApi = requireNonNull(buildsApi);
        this.runWrapperProvider = requireNonNull(runWrapperProvider);
    }

    @Override
    public List<JiraSendInfoResponse> sendBuildInfo(
            final JiraBuildInfoRequest request, final PipelineLogger pipelineLogger) {
        final List<JiraSendInfoResponse> responses = new LinkedList<>();
        if (request.getSite() == null) {
            List<String> jiraSites = siteConfigRetriever.getAllJiraSites();
            for (final String jiraSite : jiraSites) {
                final Optional<JiraCloudSiteConfig> maybeSiteConfig = getSiteConfigFor(jiraSite);

                responses.add(
                        maybeSiteConfig
                                .map(
                                        siteConfig ->
                                                sendBuildInfoToJiraSite(
                                                        siteConfig, request, pipelineLogger))
                                .orElse(JiraCommonResponse.failureSiteConfigNotFound(jiraSite)));
            }
        } else {
            final Optional<JiraCloudSiteConfig> maybeSiteConfig =
                    getSiteConfigFor(request.getSite());
            responses.add(
                    maybeSiteConfig
                            .map(
                                    siteConfig ->
                                            sendBuildInfoToJiraSite(
                                                    siteConfig, request, pipelineLogger))
                            .orElse(
                                    JiraCommonResponse.failureSiteConfigNotFound(
                                            request.getSite())));
        }
        return responses;
    }

    /**
     * Sends build data to a Jira site.
     *
     * @param siteConfig - Jira to send data to
     * @param request - JiraBuildInfoRequest::site is ignored and jiraSite is used instead
     */
    public JiraSendInfoResponse sendBuildInfoToJiraSite(
            @Nonnull final JiraCloudSiteConfig siteConfig,
            @Nonnull final JiraBuildInfoRequest request,
            @Nonnull final PipelineLogger pipelineLogger) {

        final String jiraSite = siteConfig.getSite();

        final Optional<String> maybeSecret = getSecretFor(siteConfig.getCredentialsId());

        if (!maybeSecret.isPresent()) {
            return JiraCommonResponse.failureSecretNotFound(jiraSite);
        }

        final Set<String> issueKeys = getIssueKeys(request, pipelineLogger);

        if (issueKeys.isEmpty()) {
            return JiraBuildInfoResponse.skippedIssueKeysNotFound(siteConfig.getSite());
        }

        final Optional<String> maybeCloudId = getCloudIdFor(jiraSite);

        if (!maybeCloudId.isPresent()) {
            return JiraCommonResponse.failureSiteNotFound(jiraSite);
        }

        final Builds buildInfo = createJiraBuildInfo(request, issueKeys);

        try {
            return handleBuildApiResponse(
                    jiraSite,
                    buildsApi.sendBuildAsJwt(
                            siteConfig.getWebhookUrl(),
                            buildInfo,
                            maybeSecret.get(),
                            pipelineLogger));
        } catch (ApiUpdateFailedException e) {
            return handleBuildApiError(jiraSite, e.getMessage());
        }
    }

    protected abstract Set<String> getIssueKeys(
            final JiraBuildInfoRequest request, final PipelineLogger pipelineLogger);

    private Optional<JiraCloudSiteConfig> getSiteConfigFor(@Nullable final String jiraSite) {
        return siteConfigRetriever.getJiraSiteConfig(jiraSite);
    }

    private Optional<String> getCloudIdFor(final String jiraSite) {
        final String jiraSiteUrl = Constants.HTTPS_PROTOCOL + jiraSite;
        return cloudIdResolver.getCloudId(jiraSiteUrl);
    }

    private Optional<String> getSecretFor(final String credentialsId) {
        return secretRetriever.getSecretFor(credentialsId);
    }

    protected abstract Builds createJiraBuildInfo(
            final JiraBuildInfoRequest request, final Set<String> issueKeys);

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
