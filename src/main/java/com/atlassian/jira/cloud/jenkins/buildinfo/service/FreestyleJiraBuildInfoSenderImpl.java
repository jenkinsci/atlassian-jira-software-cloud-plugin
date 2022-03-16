package com.atlassian.jira.cloud.jenkins.buildinfo.service;

import com.atlassian.jira.cloud.jenkins.buildinfo.client.BuildPayloadBuilder;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.BuildsApi;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.Builds;
import com.atlassian.jira.cloud.jenkins.common.config.JiraSiteConfigRetriever;
import com.atlassian.jira.cloud.jenkins.common.model.IssueKey;
import com.atlassian.jira.cloud.jenkins.common.service.FreestyleIssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.tenantinfo.CloudIdResolver;
import com.atlassian.jira.cloud.jenkins.util.IssueKeyStringExtractor;
import com.atlassian.jira.cloud.jenkins.util.RunWrapperProvider;
import com.atlassian.jira.cloud.jenkins.util.SecretRetriever;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class FreestyleJiraBuildInfoSenderImpl extends JiraBuildInfoSenderImpl {

    private final FreestyleIssueKeyExtractor issueKeyExtractor;
    private final FreestyleIssueKeyExtractor changeLogIssueKeyExtractor;

    public FreestyleJiraBuildInfoSenderImpl(
            final JiraSiteConfigRetriever siteConfigRetriever,
            final SecretRetriever secretRetriever,
            final FreestyleIssueKeyExtractor issueKeyExtractor,
            final CloudIdResolver cloudIdResolver,
            final BuildsApi buildsApi,
            final RunWrapperProvider runWrapperProvider,
            final FreestyleIssueKeyExtractor changeLogIssueKeyExtractor) {
        super(siteConfigRetriever, secretRetriever, cloudIdResolver, buildsApi, runWrapperProvider);
        this.issueKeyExtractor = requireNonNull(issueKeyExtractor);
        this.changeLogIssueKeyExtractor = requireNonNull(changeLogIssueKeyExtractor);
    }

    @Override
    protected Set<String> getIssueKeys(final JiraBuildInfoRequest request) {
        FreestyleBuildInfoRequest freestyleRequest = (FreestyleBuildInfoRequest) request;
        Set<String> branchIssueKeys =
                Optional.ofNullable(request.getBranch())
                        .filter(StringUtils::isNotEmpty)
                        .map(
                                branch ->
                                        IssueKeyStringExtractor.extractIssueKeys(branch)
                                                .stream()
                                                .map(IssueKey::toString)
                                                .collect(Collectors.toSet()))
                        .orElseGet(
                                () ->
                                        issueKeyExtractor.extractIssueKeys(
                                                freestyleRequest.getBuild()));
        Set<String> commitIssueKeys =
                changeLogIssueKeyExtractor.extractIssueKeys(freestyleRequest.getBuild());
        if (!commitIssueKeys.isEmpty()) {
            branchIssueKeys.addAll(commitIssueKeys);
        }
        return branchIssueKeys;
    }

    @Override
    protected Builds createJiraBuildInfo(
            final JiraBuildInfoRequest request, final Set<String> issueKeys) {
        FreestyleBuildInfoRequest freestyleRequest = (FreestyleBuildInfoRequest) request;
        final RunWrapper buildWrapper = runWrapperProvider.getWrapper(freestyleRequest.getBuild());

        return BuildPayloadBuilder.getBuildPayload(request.getJiraState(), buildWrapper, issueKeys);
    }
}
