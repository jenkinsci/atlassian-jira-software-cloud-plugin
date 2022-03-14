package com.atlassian.jira.cloud.jenkins.checkgatingstatus.service;

import com.atlassian.jira.cloud.jenkins.checkgatingstatus.client.GatingStatusApi;
import com.atlassian.jira.cloud.jenkins.checkgatingstatus.client.model.GatingStatusResponse;
import com.atlassian.jira.cloud.jenkins.common.config.JiraSiteConfig2Retriever;
import com.atlassian.jira.cloud.jenkins.common.response.JiraCommonResponse;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudSiteConfig2;
import com.atlassian.jira.cloud.jenkins.tenantinfo.CloudIdResolver;
import com.atlassian.jira.cloud.jenkins.util.SecretRetriever;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class JiraGatingStatusRetrieverImpl implements JiraGatingStatusRetriever {

    private static final String HTTPS_PROTOCOL = "https://";

    private final JiraSiteConfig2Retriever siteConfigRetriever;
    private final SecretRetriever secretRetriever;
    private final CloudIdResolver cloudIdResolver;
    private final GatingStatusApi gatingApi;

    private static final Logger logger =
            LoggerFactory.getLogger(JiraGatingStatusRetrieverImpl.class);

    public JiraGatingStatusRetrieverImpl(
            final JiraSiteConfig2Retriever siteConfigRetriever,
            final SecretRetriever secretRetriever,
            final CloudIdResolver cloudIdResolver,
            final GatingStatusApi gatingApi) {
        this.siteConfigRetriever = siteConfigRetriever;
        this.secretRetriever = secretRetriever;
        this.cloudIdResolver = cloudIdResolver;
        this.gatingApi = gatingApi;
    }

    @Override
    public JiraGatingStatusResponse getGatingStatus(
            final String jiraSite, final String environmentId, final WorkflowRun run) {

        final Optional<JiraCloudSiteConfig2> maybeSiteConfig =
                siteConfigRetriever.getJiraSiteConfig(jiraSite);

        if (!maybeSiteConfig.isPresent()) {
            return JiraGatingStatusResponse.of(
                    JiraCommonResponse.failureSiteConfigNotFound(jiraSite));
        }

        final String resolvedSiteConfig = maybeSiteConfig.get().getSite();

        final JiraCloudSiteConfig2 siteConfig = maybeSiteConfig.get();
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

        try {
            final GatingStatusResponse result =
                    gatingApi.getGatingStatus(
                            siteConfig.getWebhookUrl(),
                            maybeSecret.get(),
                            cloudId,
                            Integer.toString(run.getNumber()),
                            run.getParent().getFullName(),
                            environmentId);

            return JiraGatingStatusResponse.success(jiraSite, result);
        } catch (Exception e) {
            logger.error("Error while retrieving gating status", e);
            final String errorMessage = e.getMessage();
            return JiraGatingStatusResponse.failure(jiraSite, errorMessage);
        }
    }
}
