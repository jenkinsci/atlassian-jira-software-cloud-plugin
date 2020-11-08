package com.atlassian.jira.cloud.jenkins.checkgatingstatus.service;

import com.atlassian.jira.cloud.jenkins.auth.AccessTokenRetriever;
import com.atlassian.jira.cloud.jenkins.checkgatingstatus.client.model.GatingStatusResponse;
import com.atlassian.jira.cloud.jenkins.common.client.JiraApi;
import com.atlassian.jira.cloud.jenkins.common.client.PostUpdateResult;
import com.atlassian.jira.cloud.jenkins.common.config.JiraSiteConfigRetriever;
import com.atlassian.jira.cloud.jenkins.common.model.AppCredential;
import com.atlassian.jira.cloud.jenkins.common.response.JiraCommonResponse;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudSiteConfig;
import com.atlassian.jira.cloud.jenkins.tenantinfo.CloudIdResolver;
import com.atlassian.jira.cloud.jenkins.util.SecretRetriever;
import com.google.common.collect.ImmutableMap;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;

import java.util.Optional;

public class JiraGatingStatusRetrieverImpl implements JiraGatingStatusRetriever {

    private static final String HTTPS_PROTOCOL = "https://";

    private final JiraSiteConfigRetriever siteConfigRetriever;
    private final SecretRetriever secretRetriever;
    private final CloudIdResolver cloudIdResolver;
    private final AccessTokenRetriever accessTokenRetriever;
    private final JiraApi gatingApi;

    public JiraGatingStatusRetrieverImpl(
            final JiraSiteConfigRetriever siteConfigRetriever,
            final SecretRetriever secretRetriever,
            final CloudIdResolver cloudIdResolver,
            final AccessTokenRetriever accessTokenRetriever,
            final JiraApi gatingApi) {
        this.siteConfigRetriever = siteConfigRetriever;
        this.secretRetriever = secretRetriever;
        this.cloudIdResolver = cloudIdResolver;
        this.accessTokenRetriever = accessTokenRetriever;
        this.gatingApi = gatingApi;
    }

    @Override
    public JiraGatingStatusResponse getGatingStatus(final GatingStatusRequest request) {
        final String jiraSite = request.getSite();
        final WorkflowRun run = request.getRun();

        final Optional<JiraCloudSiteConfig> maybeSiteConfig =
                siteConfigRetriever.getJiraSiteConfig(jiraSite);

        if (!maybeSiteConfig.isPresent()) {
            return JiraGatingStatusResponse.of(
                    JiraCommonResponse.failureSiteConfigNotFound(jiraSite));
        }

        final String resolvedSiteConfig = maybeSiteConfig.get().getSite();

        final JiraCloudSiteConfig siteConfig = maybeSiteConfig.get();
        final Optional<String> maybeSecret =
                secretRetriever.getSecretFor(siteConfig.getCredentialsId());

        if (!maybeSecret.isPresent()) {
            return JiraGatingStatusResponse.of(
                    JiraCommonResponse.failureSecretNotFound(resolvedSiteConfig));
        }

        final Optional<String> maybeCloudId =
                cloudIdResolver.getCloudId(HTTPS_PROTOCOL + resolvedSiteConfig);

        if (!maybeCloudId.isPresent()) {
            return JiraGatingStatusResponse.of(
                    JiraCommonResponse.failureSiteNotFound(resolvedSiteConfig));
        }

        final String cloudId = maybeCloudId.get();

        final AppCredential appCredential =
                new AppCredential(siteConfig.getClientId(), maybeSecret.get());
        final Optional<String> maybeAccessToken =
                accessTokenRetriever.getAccessToken(appCredential);

        if (!maybeAccessToken.isPresent()) {
            return JiraGatingStatusResponse.of(
                    JiraCommonResponse.failureAccessToken(resolvedSiteConfig));
        }

        final ImmutableMap<String, String> pathParams =
                ImmutableMap.<String, String>builder()
                        .put("cloudId", cloudId)
                        .put("deploymentId", Integer.toString(run.getNumber()))
                        .put("pipelineId", run.getParent().getFullName())
                        .put("environmentId", request.getEnvironmentId())
                        .build();

        final PostUpdateResult<GatingStatusResponse> result =
                gatingApi.getResult(
                        maybeAccessToken.get(),
                        pathParams,
                        siteConfig.getClientId(),
                        GatingStatusResponse.class);

        if (result.getResponseEntity().isPresent()) {
            return JiraGatingStatusResponse.success(result.getResponseEntity().get());
        } else {
            final String errorMessage = result.getErrorMessage().orElse("");
            return JiraGatingStatusResponse.failure(errorMessage);
        }
    }
}
