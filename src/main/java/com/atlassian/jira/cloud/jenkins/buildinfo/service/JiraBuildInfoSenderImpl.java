package com.atlassian.jira.cloud.jenkins.buildinfo.service;

import com.atlassian.jira.cloud.jenkins.auth.AccessTokenRetriever;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.BuildPayloadBuilder;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.BuildsApi;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.BuildApiResponse;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.JiraBuildInfo;
import com.atlassian.jira.cloud.jenkins.common.model.AppCredential;
import com.atlassian.jira.cloud.jenkins.common.model.IssueKey;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudSiteConfig;
import com.atlassian.jira.cloud.jenkins.tenantinfo.CloudIdResolver;
import com.atlassian.jira.cloud.jenkins.common.config.JiraSiteConfigRetriever;
import com.atlassian.jira.cloud.jenkins.util.IssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.util.RunWrapperProvider;
import com.atlassian.jira.cloud.jenkins.util.ScmRevision;
import com.atlassian.jira.cloud.jenkins.util.ScmRevisionExtractor;
import com.atlassian.jira.cloud.jenkins.util.SecretRetriever;
import hudson.model.Run;
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class JiraBuildInfoSenderImpl implements JiraBuildInfoSender {

    private static final Logger log = LoggerFactory.getLogger(JiraBuildInfoSenderImpl.class);

    private static final String HTTPS_PROTOCOL = "https://";

    private final JiraSiteConfigRetriever siteConfigRetriever;
    private final SecretRetriever secretRetriever;
    private final ScmRevisionExtractor scmRevisionExtractor;
    private final CloudIdResolver cloudIdResolver;
    private final AccessTokenRetriever accessTokenRetriever;
    private final BuildsApi buildsApi;
    private final RunWrapperProvider runWrapperProvider;

    public JiraBuildInfoSenderImpl(
            final JiraSiteConfigRetriever siteConfigRetriever,
            final SecretRetriever secretRetriever,
            final ScmRevisionExtractor scmRevisionExtractor,
            final CloudIdResolver cloudIdResolver,
            final AccessTokenRetriever accessTokenRetriever,
            final BuildsApi buildsApi,
            final RunWrapperProvider runWrapperProvider) {
        this.siteConfigRetriever = requireNonNull(siteConfigRetriever);
        this.secretRetriever = requireNonNull(secretRetriever);
        this.scmRevisionExtractor = requireNonNull(scmRevisionExtractor);
        this.cloudIdResolver = requireNonNull(cloudIdResolver);
        this.accessTokenRetriever = requireNonNull(accessTokenRetriever);
        this.buildsApi = requireNonNull(buildsApi);
        this.runWrapperProvider = requireNonNull(runWrapperProvider);
    }

    @Override
    public JiraBuildInfoResponse sendBuildInfo(final JiraBuildInfoRequest request) {
        final String jiraSite = request.getSite();
        final Run build = request.getBuild();

        final Optional<JiraCloudSiteConfig> maybeSiteConfig = getSiteConfigFor(jiraSite);

        if (!maybeSiteConfig.isPresent()) {
            return JiraBuildInfoResponse.failureSiteConfigNotFound(build, jiraSite);
        }

        final JiraCloudSiteConfig siteConfig = maybeSiteConfig.get();
        final Optional<String> maybeSecret = getSecretFor(siteConfig.getCredentialsId());

        if (!maybeSecret.isPresent()) {
            return JiraBuildInfoResponse.failureSecretNotFound(build, jiraSite);
        }

        final Optional<ScmRevision> maybeScmRevision = getScmRevisionFor(build);

        if (!maybeScmRevision.isPresent()) {
            return JiraBuildInfoResponse.failureScmRevisionNotFound(build);
        }

        final ScmRevision scmRevision = maybeScmRevision.get();
        final Set<String> issueKeys = extractIssueKeys(scmRevision);

        if (issueKeys.isEmpty()) {
            return JiraBuildInfoResponse.skippedIssueKeysNotFound(build, scmRevision.getHead());
        }

        final Optional<String> maybeCloudId = getCloudIdFor(jiraSite);

        if (!maybeCloudId.isPresent()) {
            return JiraBuildInfoResponse.failureSiteNotFound(build, jiraSite);
        }

        final Optional<String> maybeAccessToken = getAccessTokenFor(siteConfig, maybeSecret.get());

        if (!maybeAccessToken.isPresent()) {
            return JiraBuildInfoResponse.failureAccessToken(build, jiraSite);
        }

        final JiraBuildInfo buildInfo = createJiraBuildInfo(build, issueKeys);

        return sendBuildInfo(maybeCloudId.get(), maybeAccessToken.get(), jiraSite, buildInfo)
                .map(response -> handleBuildApiResponse(build, jiraSite, response))
                .orElseGet(() -> handleBuildApiError(build, jiraSite));
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

    private Optional<ScmRevision> getScmRevisionFor(final Run build) {
        return scmRevisionExtractor.getScmRevision(build);
    }

    private Set<String> extractIssueKeys(final ScmRevision scmRevision) {
        return IssueKeyExtractor.extractIssueKeys(scmRevision.getHead())
                .stream()
                .map(IssueKey::toString)
                .collect(Collectors.toSet());
    }

    private JiraBuildInfo createJiraBuildInfo(final Run build, final Set<String> issueKeys) {
        final RunWrapper buildWrapper = runWrapperProvider.getWrapper(build);

        return BuildPayloadBuilder.getBuildInfo(buildWrapper, issueKeys);
    }

    private Optional<BuildApiResponse> sendBuildInfo(
            final String cloudId,
            final String accessToken,
            final String jiraSite,
            final JiraBuildInfo buildInfo) {
        return buildsApi.postBuildUpdate(cloudId, accessToken, jiraSite, buildInfo);
    }

    private JiraBuildInfoResponse handleBuildApiResponse(
            final Run build, final String jiraSite, final BuildApiResponse response) {
        if (!response.getAcceptedBuilds().isEmpty()) {
            return JiraBuildInfoResponse.successBuildAccepted(build, jiraSite, response);
        }

        if (!response.getRejectedBuilds().isEmpty()) {
            return JiraBuildInfoResponse.failureBuildRejected(build, jiraSite, response);
        }

        if (!response.getUnknownIssueKeys().isEmpty()) {
            return JiraBuildInfoResponse.failureUnknownIssueKeys(build, jiraSite, response);
        }

        return JiraBuildInfoResponse.failureUnexpectedResponse(build, jiraSite);
    }

    private JiraBuildInfoResponse handleBuildApiError(final Run build, final String jiraSite) {
        return JiraBuildInfoResponse.failureBuildsApiResponse(build, jiraSite);
    }
}
