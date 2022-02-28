package com.atlassian.jira.cloud.jenkins.buildinfo.service;

import com.atlassian.jira.cloud.jenkins.auth.AccessTokenRetriever;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.BuildPayloadBuilder;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.Builds;
import com.atlassian.jira.cloud.jenkins.common.client.JiraApi;
import com.atlassian.jira.cloud.jenkins.common.config.JiraSiteConfigRetriever;
import com.atlassian.jira.cloud.jenkins.common.model.IssueKey;
import com.atlassian.jira.cloud.jenkins.common.service.IssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.service.ChangeLogIssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.tenantinfo.CloudIdResolver;
import com.atlassian.jira.cloud.jenkins.util.IssueKeyStringExtractor;
import com.atlassian.jira.cloud.jenkins.util.RunWrapperProvider;
import com.atlassian.jira.cloud.jenkins.util.SecretRetriever;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class MultibranchBuildInfoSenderImpl extends JiraBuildInfoSenderImpl {

    private final IssueKeyExtractor changeLogIssueKeyExtractor = new ChangeLogIssueKeyExtractor();
    private final IssueKeyExtractor issueKeyExtractor;

    public MultibranchBuildInfoSenderImpl(
            final JiraSiteConfigRetriever siteConfigRetriever,
            final SecretRetriever secretRetriever,
            final IssueKeyExtractor issueKeyExtractor,
            final CloudIdResolver cloudIdResolver,
            final AccessTokenRetriever accessTokenRetriever,
            final JiraApi buildsApi,
            final RunWrapperProvider runWrapperProvider) {
        super(
                siteConfigRetriever,
                secretRetriever,
                cloudIdResolver,
                accessTokenRetriever,
                buildsApi,
                runWrapperProvider);
        this.issueKeyExtractor = issueKeyExtractor;
    }

    @Override
    protected Set<String> getIssueKeys(final JiraBuildInfoRequest request) {

        MultibranchBuildInfoRequest multibranchRequest = (MultibranchBuildInfoRequest) request;
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
                                                multibranchRequest.getBuild()));
        Set<String> commitIssueKeys =
                changeLogIssueKeyExtractor.extractIssueKeys(multibranchRequest.getBuild());
        if (!commitIssueKeys.isEmpty()) {
            branchIssueKeys.addAll(commitIssueKeys);
        }
        return branchIssueKeys;
    }

    @Override
    protected Builds createJiraBuildInfo(
            final JiraBuildInfoRequest request, final Set<String> issueKeys) {
        MultibranchBuildInfoRequest multibranchRequest = (MultibranchBuildInfoRequest) request;
        final RunWrapper buildWrapper =
                runWrapperProvider.getWrapper(multibranchRequest.getBuild());
        return BuildPayloadBuilder.getBuildPayload(request.getJiraState(), buildWrapper, issueKeys);
    }
}
