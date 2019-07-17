package com.atlassian.jira.cloud.jenkins.deploymentinfo.service;

import com.atlassian.jira.cloud.jenkins.auth.AccessTokenRetriever;
import com.atlassian.jira.cloud.jenkins.common.client.JiraApi;
import com.atlassian.jira.cloud.jenkins.common.config.JiraSiteConfigRetriever;
import com.atlassian.jira.cloud.jenkins.common.model.AppCredential;
import com.atlassian.jira.cloud.jenkins.common.response.JiraCommonResponse;
import com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse;
import com.atlassian.jira.cloud.jenkins.common.service.IssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudSiteConfig;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.DeploymentPayloadBuilder;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.DeploymentApiResponse;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.Deployments;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.Environment;
import com.atlassian.jira.cloud.jenkins.tenantinfo.CloudIdResolver;
import com.atlassian.jira.cloud.jenkins.util.RunWrapperProvider;
import com.atlassian.jira.cloud.jenkins.util.SecretRetriever;
import hudson.model.Run;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper;

import java.util.Optional;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Implementation of JiraDeploymentInfoSender to send build updates to Jira by building the payload,
 * generating the access token, sending the request and parsing the response.
 */
public class JiraDeploymentInfoSenderImpl implements JiraDeploymentInfoSender {

    private static final String HTTPS_PROTOCOL = "https://";

    private final JiraSiteConfigRetriever siteConfigRetriever;
    private final SecretRetriever secretRetriever;
    private final CloudIdResolver cloudIdResolver;
    private final AccessTokenRetriever accessTokenRetriever;
    private final JiraApi deploymentsApi;
    private final RunWrapperProvider runWrapperProvider;
    private final IssueKeyExtractor issueKeyExtractor;

    public JiraDeploymentInfoSenderImpl(
            final JiraSiteConfigRetriever siteConfigRetriever,
            final SecretRetriever secretRetriever,
            final CloudIdResolver cloudIdResolver,
            final AccessTokenRetriever accessTokenRetriever,
            final JiraApi jiraApi,
            final IssueKeyExtractor issueKeyExtractor,
            final RunWrapperProvider runWrapperProvider) {
        this.siteConfigRetriever = requireNonNull(siteConfigRetriever);
        this.secretRetriever = requireNonNull(secretRetriever);
        this.cloudIdResolver = requireNonNull(cloudIdResolver);
        this.accessTokenRetriever = requireNonNull(accessTokenRetriever);
        this.deploymentsApi = requireNonNull(jiraApi);
        this.runWrapperProvider = requireNonNull(runWrapperProvider);
        this.issueKeyExtractor = requireNonNull(issueKeyExtractor);
    }

    @Override
    public JiraSendInfoResponse sendDeploymentInfo(final JiraDeploymentInfoRequest request) {
        final String jiraSite = request.getSite();
        final WorkflowRun deployment = request.getDeployment();

        final Optional<JiraCloudSiteConfig> maybeSiteConfig = getSiteConfigFor(jiraSite);

        if (!maybeSiteConfig.isPresent()) {
            return JiraCommonResponse.failureSiteConfigNotFound(jiraSite);
        }

        final JiraCloudSiteConfig siteConfig = maybeSiteConfig.get();
        final Optional<String> maybeSecret = getSecretFor(siteConfig.getCredentialsId());

        if (!maybeSecret.isPresent()) {
            return JiraCommonResponse.failureSecretNotFound(jiraSite);
        }

        final Set<String> issueKeys = issueKeyExtractor.extractIssueKeys(deployment);

        if (issueKeys.isEmpty()) {
            return JiraDeploymentInfoResponse.skippedIssueKeysNotFound(jiraSite);
        }

        final Optional<String> maybeCloudId = getCloudIdFor(jiraSite);

        if (!maybeCloudId.isPresent()) {
            return JiraCommonResponse.failureSiteNotFound(jiraSite);
        }

        final Optional<String> maybeAccessToken = getAccessTokenFor(siteConfig, maybeSecret.get());

        if (!maybeAccessToken.isPresent()) {
            return JiraCommonResponse.failureAccessToken(jiraSite);
        }

        final Deployments deploymentInfo = createJiraDeploymentInfo(deployment, request, issueKeys);

        return sendDeploymentInfo(
                        maybeCloudId.get(), maybeAccessToken.get(), jiraSite, deploymentInfo)
                .map(response -> handleDeploymentApiResponse(jiraSite, response))
                .orElseGet(() -> handleDeploymentApiError(jiraSite));
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

    private Deployments createJiraDeploymentInfo(
            final Run build, final JiraDeploymentInfoRequest request, final Set<String> issueKeys) {
        final RunWrapper buildWrapper = runWrapperProvider.getWrapper(build);

        return DeploymentPayloadBuilder.getDeploymentInfo(
                buildWrapper, buildEnvironment(request), issueKeys);
    }

    private Optional<DeploymentApiResponse> sendDeploymentInfo(
            final String cloudId,
            final String accessToken,
            final String jiraSite,
            final Deployments deploymentInfo) {
        return deploymentsApi.postUpdate(
                cloudId, accessToken, jiraSite, deploymentInfo, DeploymentApiResponse.class);
    }

    private JiraSendInfoResponse handleDeploymentApiResponse(
            final String jiraSite, final DeploymentApiResponse response) {
        if (!response.getAcceptedDeployments().isEmpty()) {
            return JiraDeploymentInfoResponse.successDeploymentAccepted(jiraSite, response);
        }

        if (!response.getRejectedDeployments().isEmpty()) {
            return JiraDeploymentInfoResponse.failureDeploymentdRejected(jiraSite, response);
        }

        if (!response.getUnknownIssueKeys().isEmpty()) {
            return JiraDeploymentInfoResponse.failureUnknownIssueKeys(jiraSite, response);
        }

        return JiraDeploymentInfoResponse.failureUnexpectedResponse();
    }

    private JiraDeploymentInfoResponse handleDeploymentApiError(final String jiraSite) {
        return JiraDeploymentInfoResponse.failureDeploymentsApiResponse(jiraSite);
    }

    private Environment buildEnvironment(final JiraDeploymentInfoRequest request) {
        return Environment.builder()
                .withId(request.getEnvironmentId())
                .withDisplayName(request.getEnvironmentName())
                .withType(request.getEnvironmentType())
                .build();
    }
}
