package com.atlassian.jira.cloud.jenkins.deploymentinfo.service;

import com.atlassian.jira.cloud.jenkins.auth.AccessTokenRetriever;
import com.atlassian.jira.cloud.jenkins.common.client.JiraApi;
import com.atlassian.jira.cloud.jenkins.common.client.PostUpdateResult;
import com.atlassian.jira.cloud.jenkins.common.config.JiraSiteConfigRetriever;
import com.atlassian.jira.cloud.jenkins.common.model.AppCredential;
import com.atlassian.jira.cloud.jenkins.common.model.IssueKey;
import com.atlassian.jira.cloud.jenkins.common.response.JiraCommonResponse;
import com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse;
import com.atlassian.jira.cloud.jenkins.common.service.IssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudSiteConfig;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.DeploymentPayloadBuilder;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.DeploymentApiResponse;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.Deployments;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.Environment;
import com.atlassian.jira.cloud.jenkins.tenantinfo.CloudIdResolver;
import com.atlassian.jira.cloud.jenkins.util.IssueKeyStringExtractor;
import com.atlassian.jira.cloud.jenkins.util.RunWrapperProvider;
import com.atlassian.jira.cloud.jenkins.util.SecretRetriever;
import com.google.common.collect.ImmutableSet;
import hudson.model.Run;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

        final String resolvedSiteConfig = maybeSiteConfig.get().getSite();

        final JiraCloudSiteConfig siteConfig = maybeSiteConfig.get();
        final Optional<String> maybeSecret = getSecretFor(siteConfig.getCredentialsId());

        if (!maybeSecret.isPresent()) {
            return JiraCommonResponse.failureSecretNotFound(resolvedSiteConfig);
        }

        final Environment environment = buildEnvironment(request);
        final List<String> errorMessages = EnvironmentValidator.validate(environment);

        if (!errorMessages.isEmpty()) {
            return JiraDeploymentInfoResponse.failureEnvironmentInvalid(jiraSite, errorMessages);
        }

        Set<String> jenkinsIssues = issueKeyExtractor.extractIssueKeys(deployment);
        Set<String> issues = new HashSet<>();
        if (request.getChangeSet() != null && request.getChangeSet().size() > 0) {
            for (String change : request.getChangeSet()) {
                issues.addAll(
                        IssueKeyStringExtractor.extractIssueKeys(change)
                                .stream()
                                .map(IssueKey::toString)
                                .collect(Collectors.toSet()));
            }
        }
        issues.addAll(jenkinsIssues);
        final Set<String> issueKeys = ImmutableSet.copyOf(issues);

        if (issueKeys.isEmpty()) {
            return JiraDeploymentInfoResponse.skippedIssueKeysNotFound(resolvedSiteConfig);
        }

        final Optional<String> maybeCloudId = getCloudIdFor(resolvedSiteConfig);

        if (!maybeCloudId.isPresent()) {
            return JiraCommonResponse.failureSiteNotFound(resolvedSiteConfig);
        }

        final Optional<String> maybeAccessToken = getAccessTokenFor(siteConfig, maybeSecret.get());

        if (!maybeAccessToken.isPresent()) {
            return JiraCommonResponse.failureAccessToken(resolvedSiteConfig);
        }

        final Deployments deploymentInfo =
                createJiraDeploymentInfo(deployment, environment, issueKeys);

        final PostUpdateResult<DeploymentApiResponse> postUpdateResult =
                sendDeploymentInfo(
                        maybeCloudId.get(),
                        maybeAccessToken.get(),
                        resolvedSiteConfig,
                        deploymentInfo);

        if (postUpdateResult.getResponseEntity().isPresent()) {
            return handleDeploymentApiResponse(
                    resolvedSiteConfig, postUpdateResult.getResponseEntity().get());
        } else {
            final String errorMessage = postUpdateResult.getErrorMessage().orElse("");
            return handleDeploymentApiError(resolvedSiteConfig, errorMessage);
        }
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

    private Deployments createJiraDeploymentInfo(
            final Run build, final Environment environment, final Set<String> issueKeys) {
        final RunWrapper buildWrapper = runWrapperProvider.getWrapper(build);

        return DeploymentPayloadBuilder.getDeploymentInfo(buildWrapper, environment, issueKeys);
    }

    private PostUpdateResult<DeploymentApiResponse> sendDeploymentInfo(
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

    private JiraDeploymentInfoResponse handleDeploymentApiError(
            final String jiraSite, final String errorMessage) {
        return JiraDeploymentInfoResponse.failureDeploymentsApiResponse(jiraSite, errorMessage);
    }

    private Environment buildEnvironment(final JiraDeploymentInfoRequest request) {
        // JENKINS-59862: if environmentType parameter was not provided, we should fallback to
        // "unmapped"
        final String environmentType =
                StringUtils.isNotBlank(request.getEnvironmentType())
                        ? request.getEnvironmentType()
                        : "unmapped";
        return Environment.builder()
                .withId(request.getEnvironmentId())
                .withDisplayName(request.getEnvironmentName())
                .withType(environmentType)
                .build();
    }
}
