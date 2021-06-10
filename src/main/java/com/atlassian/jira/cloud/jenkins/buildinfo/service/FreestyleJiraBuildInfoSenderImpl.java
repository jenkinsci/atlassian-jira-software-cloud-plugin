package com.atlassian.jira.cloud.jenkins.buildinfo.service;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper;

import com.atlassian.jira.cloud.jenkins.auth.AccessTokenRetriever;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.BuildPayloadBuilder;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.BuildApiResponse;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.Builds;
import com.atlassian.jira.cloud.jenkins.common.client.JiraApi;
import com.atlassian.jira.cloud.jenkins.common.client.PostUpdateResult;
import com.atlassian.jira.cloud.jenkins.common.config.JiraSiteConfigRetriever;
import com.atlassian.jira.cloud.jenkins.common.model.AppCredential;
import com.atlassian.jira.cloud.jenkins.common.model.IssueKey;
import com.atlassian.jira.cloud.jenkins.common.response.JiraCommonResponse;
import com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse;
import com.atlassian.jira.cloud.jenkins.common.service.FreestyleIssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudSiteConfig;
import com.atlassian.jira.cloud.jenkins.tenantinfo.CloudIdResolver;
import com.atlassian.jira.cloud.jenkins.util.IssueKeyStringExtractor;
import com.atlassian.jira.cloud.jenkins.util.RunWrapperProvider;
import com.atlassian.jira.cloud.jenkins.util.SecretRetriever;

import hudson.model.AbstractBuild;

/**
 * Implementation of JiraBuildInfoSender to send build updates to Jira by building the payload,
 * generating the access token, sending the request and parsing the response.
 */
public class FreestyleJiraBuildInfoSenderImpl implements FreestyleBuildInfoSender{



    private static final String HTTPS_PROTOCOL = "https://";

    private final JiraSiteConfigRetriever siteConfigRetriever;
    private final SecretRetriever secretRetriever;
    private final FreestyleIssueKeyExtractor issueKeyExtractor;
    private final CloudIdResolver cloudIdResolver;
    private final AccessTokenRetriever accessTokenRetriever;
    private final JiraApi buildsApi;
    private final RunWrapperProvider runWrapperProvider;
    private final FreestyleIssueKeyExtractor changeLogIssueKeyExtractor;
    
    public FreestyleJiraBuildInfoSenderImpl(
            final JiraSiteConfigRetriever siteConfigRetriever,
            final SecretRetriever secretRetriever,
            final FreestyleIssueKeyExtractor issueKeyExtractor,
            final CloudIdResolver cloudIdResolver,
            final AccessTokenRetriever accessTokenRetriever,
            final JiraApi buildsApi,
            final RunWrapperProvider runWrapperProvider,
            final FreestyleIssueKeyExtractor changeLogIssueKeyExtractor) {
        this.siteConfigRetriever = requireNonNull(siteConfigRetriever);
        this.secretRetriever = requireNonNull(secretRetriever);
        this.issueKeyExtractor = requireNonNull(issueKeyExtractor);
        this.cloudIdResolver = requireNonNull(cloudIdResolver);
        this.accessTokenRetriever = requireNonNull(accessTokenRetriever);
        this.buildsApi = requireNonNull(buildsApi);
        this.runWrapperProvider = requireNonNull(runWrapperProvider);
        this.changeLogIssueKeyExtractor = requireNonNull(changeLogIssueKeyExtractor);
    }

    

    @Override
    public JiraSendInfoResponse sendBuildInfo(final FreestyleBuildInfoRequest request) {
        final String jiraSite = request.getSite();


        final Optional<JiraCloudSiteConfig> maybeSiteConfig = getSiteConfigFor(jiraSite);

        if (!maybeSiteConfig.isPresent()) {
            return JiraCommonResponse.failureSiteConfigNotFound(jiraSite);
        }

        final String resolvedSiteConfig = maybeSiteConfig.get().getSite();

        final JiraCloudSiteConfig siteConfig = maybeSiteConfig.get();
        final Optional<String> maybeSecret = getSecretFor(siteConfig.getCredentialsId());

        if (!maybeSecret.isPresent()) {
            return JiraCommonResponse.failureSecretNotFound(resolvedSiteConfig);
        }

        final Set<String> issueKeys = getIssueKeys(request);

        if (issueKeys.isEmpty()) {
            return JiraBuildInfoResponse.skippedIssueKeysNotFound();
        }

        final Optional<String> maybeCloudId = getCloudIdFor(resolvedSiteConfig);

        if (!maybeCloudId.isPresent()) {
            return JiraCommonResponse.failureSiteNotFound(resolvedSiteConfig);
        }

        final Optional<String> maybeAccessToken = getAccessTokenFor(siteConfig, maybeSecret.get());

        if (!maybeAccessToken.isPresent()) {
            return JiraCommonResponse.failureAccessToken(resolvedSiteConfig);
        }

        final Builds buildInfo = invokeCreateJiraBuildInfo(request, issueKeys);

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
    
    private Builds invokeCreateJiraBuildInfo(
            final FreestyleBuildInfoRequest request, final Set<String> issueKeys) {

        return createJiraBuildInfo(request.getBuild(), issueKeys);
    }

    private Set<String> getIssueKeys(final FreestyleBuildInfoRequest request) {

        Set<String> branchIssueKeys =
                Optional.ofNullable(request.getBranch())
                        .filter(StringUtils::isNotEmpty)
                        .map(
                                branch ->
                                        IssueKeyStringExtractor.extractIssueKeys(branch)
                                                .stream()
                                                .map(IssueKey::toString)
                                                .collect(Collectors.toSet()))
                        .orElseGet(() -> issueKeyExtractor.extractIssueKeys(request.getBuild()));
        Set<String> commitIssueKeys =
                changeLogIssueKeyExtractor.extractIssueKeys(request.getBuild());
        if (!commitIssueKeys.isEmpty()) {
            branchIssueKeys.addAll(commitIssueKeys);
        }
        return branchIssueKeys;
    }

    private Optional<JiraCloudSiteConfig> getSiteConfigFor(@Nullable final String jiraSite) {
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

    private Builds createJiraBuildInfo(final AbstractBuild<?, ?> build, final Set<String> issueKeys) {
        final RunWrapper buildWrapper = runWrapperProvider.getWrapper(build);

        return BuildPayloadBuilder.getBuildPayload(buildWrapper, issueKeys);
    }

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

        return JiraBuildInfoResponse.failureUnexpectedResponse();
    }

    private JiraBuildInfoResponse handleBuildApiError(
            final String jiraSite, final String errorMessage) {
        return JiraBuildInfoResponse.failureBuildsApiResponse(jiraSite, errorMessage);
    }
}
