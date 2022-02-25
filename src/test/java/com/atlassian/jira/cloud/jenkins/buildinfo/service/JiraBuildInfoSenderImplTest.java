package com.atlassian.jira.cloud.jenkins.buildinfo.service;

import com.atlassian.jira.cloud.jenkins.buildinfo.client.BuildsApi;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.BuildApiResponse;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.BuildKeyResponse;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.RejectedBuildResponse;
import com.atlassian.jira.cloud.jenkins.common.client.ApiUpdateFailedException;
import com.atlassian.jira.cloud.jenkins.common.config.JiraSiteConfig2Retriever;
import com.atlassian.jira.cloud.jenkins.common.model.ApiErrorResponse;
import com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse;
import com.atlassian.jira.cloud.jenkins.common.service.IssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudSiteConfig2;
import com.atlassian.jira.cloud.jenkins.tenantinfo.CloudIdResolver;
import com.atlassian.jira.cloud.jenkins.util.RunWrapperProvider;
import com.atlassian.jira.cloud.jenkins.util.SecretRetriever;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import hudson.AbortException;
import hudson.model.Run;
import hudson.scm.ChangeLogSet;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse.Status.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class JiraBuildInfoSenderImplTest {

    private static final String SITE = "example.atlassian.com";
    private static final String SITE2 = "example2.atlassian.com";
    private static final String BRANCH_NAME = "TEST-123-branch-name";
    private static final String CLOUD_ID = "my-cloud-id";
    private static final String CLOUD_ID2 = "my-cloud-id-2";
    public static final String PIPELINE_ID = "my-pipeline-id";
    public static final int BUILD_NUMBER = 1;
    private static final JiraCloudSiteConfig2 JIRA_SITE_CONFIG =
            new JiraCloudSiteConfig2(
                    SITE, "https://webhook.url?jenkins_server_uuid=foo", "credsId");

    private static final JiraCloudSiteConfig2 JIRA_SITE_CONFIG2 =
            new JiraCloudSiteConfig2(
                    SITE2, "https://webhook.url?jenkins_server_uuid=bar", "credsId2");

    @Mock private JiraSiteConfig2Retriever siteConfigRetriever;

    @Mock private SecretRetriever secretRetriever;

    @Mock private IssueKeyExtractor issueKeyExtractor;

    @Mock private CloudIdResolver cloudIdResolver;

    @Mock private BuildsApi buildsApi;

    @Mock private RunWrapperProvider runWrapperProvider;

    private JiraBuildInfoSender classUnderTest;

    @Before
    public void setUp() {
        classUnderTest =
                new MultibranchBuildInfoSenderImpl(
                        siteConfigRetriever,
                        secretRetriever,
                        issueKeyExtractor,
                        cloudIdResolver,
                        buildsApi,
                        runWrapperProvider);

        setupMocks();
    }

    @Test
    public void testSendBuildInfo_whenSiteConfigNotFound() {
        // given
        when(siteConfigRetriever.getJiraSiteConfig(any())).thenReturn(Optional.empty());

        // when
        final JiraSendInfoResponse response =
                classUnderTest.sendBuildInfo(createOneJiraRequest()).get(0);

        // then
        assertThat(response.getStatus()).isEqualTo(FAILURE_SITE_CONFIG_NOT_FOUND);
    }

    @Test
    public void testSendBuildInfo_whenSecretNotFound() {
        // given
        when(secretRetriever.getSecretFor(any())).thenReturn(Optional.empty());

        // when
        final JiraSendInfoResponse response =
                classUnderTest.sendBuildInfo(createOneJiraRequest()).get(0);

        // then
        assertThat(response.getStatus()).isEqualTo(FAILURE_SECRET_NOT_FOUND);
    }

    @Test
    public void testSendBuildInfo_whenIssueKeysNotFound() {
        // given
        when(issueKeyExtractor.extractIssueKeys(any())).thenReturn(Collections.emptySet());

        // when
        final JiraSendInfoResponse response =
                classUnderTest.sendBuildInfo(createOneJiraRequest()).get(0);

        // then
        assertThat(response.getStatus()).isEqualTo(SKIPPED_ISSUE_KEYS_NOT_FOUND);
        final String message = response.getMessage();
        assertThat(message).startsWith("No issue keys found in the current branch name");
    }

    @Test
    public void testSendBuildInfo_whenCloudIdNotFound() {
        // given
        when(cloudIdResolver.getCloudId(any())).thenReturn(Optional.empty());

        // when
        final JiraSendInfoResponse response =
                classUnderTest.sendBuildInfo(createOneJiraRequest()).get(0);

        // then
        assertThat(response.getStatus()).isEqualTo(FAILURE_SITE_NOT_FOUND);
    }

    @Test
    public void testSendBuildInfo_whenApiResponseFailure() {
        // given
        setupBuildsApiFailure();

        // when
        final JiraSendInfoResponse response =
                classUnderTest.sendBuildInfo(createOneJiraRequest()).get(0);

        // then
        assertThat(response.getStatus())
                .isEqualTo(JiraSendInfoResponse.Status.FAILURE_BUILDS_API_RESPONSE);
    }

    @Test
    public void testSendBuildInfo_whenBuildAccepted() {
        // given
        setupBuildsApiBuildAccepted();

        // when
        final JiraSendInfoResponse response =
                classUnderTest.sendBuildInfo(createOneJiraRequest()).get(0);

        // then
        assertThat(response.getStatus())
                .isEqualTo(JiraSendInfoResponse.Status.SUCCESS_BUILD_ACCEPTED);
        final String message = response.getMessage();
        assertThat(message).isNotBlank();
    }

    @Test
    public void testSendBuildInfo_whenBuildRejected() {
        // given
        setupBuildApiBuildRejected();

        // when
        final JiraSendInfoResponse response =
                classUnderTest.sendBuildInfo(createOneJiraRequest()).get(0);

        // then
        assertThat(response.getStatus())
                .isEqualTo(JiraSendInfoResponse.Status.FAILURE_BUILD_REJECTED);
        final String message = response.getMessage();
        assertThat(message).isNotBlank();
    }

    @Test
    public void testSendBuildInfo_whenUserProvidedBranch() {
        // given
        final JiraBuildInfoRequest jiraBuildInfoRequest =
                new MultibranchBuildInfoRequest(
                        SITE, BRANCH_NAME, mockWorkflowRun(), Optional.empty());
        setupBuildsApiBuildAccepted();

        // when
        final JiraSendInfoResponse response =
                classUnderTest.sendBuildInfo(jiraBuildInfoRequest).get(0);

        verify(issueKeyExtractor, never()).extractIssueKeys(any());
        assertThat(response.getStatus())
                .isEqualTo(JiraSendInfoResponse.Status.SUCCESS_BUILD_ACCEPTED);
        final String message = response.getMessage();
        assertThat(message).isNotBlank();
    }

    @Test
    public void testSendBuildInfo_whenBranchNameIsEmpty() {
        // given
        final JiraBuildInfoRequest jiraBuildInfoRequest =
                new MultibranchBuildInfoRequest(SITE, "", mockWorkflowRun(), Optional.empty());
        setupBuildsApiBuildAccepted();

        // when
        final JiraSendInfoResponse response =
                classUnderTest.sendBuildInfo(jiraBuildInfoRequest).get(0);

        verify(issueKeyExtractor).extractIssueKeys(any());
        assertThat(response.getStatus())
                .isEqualTo(JiraSendInfoResponse.Status.SUCCESS_BUILD_ACCEPTED);
        final String message = response.getMessage();
        assertThat(message).isNotBlank();
    }

    @Test
    public void testSendBuildInfo_whenUnknownIssueKeys() {
        // given
        setupBuildApiUnknownIssueKeys();

        // when
        final JiraSendInfoResponse response =
                classUnderTest.sendBuildInfo(createOneJiraRequest()).get(0);

        assertThat(response.getStatus())
                .isEqualTo(JiraSendInfoResponse.Status.FAILURE_UNKNOWN_ISSUE_KEYS);
        final String message = response.getMessage();
        assertThat(message).isNotBlank();
    }

    @Test
    public void testSendBuildInfo_whenIssueKeysInUserCommitTitle() {
        // given
        final WorkflowRun workflowRun = changeSetWithOneChangeSetEntry();
        final JiraBuildInfoRequest jiraBuildInfoRequest =
                new MultibranchBuildInfoRequest(SITE, BRANCH_NAME, workflowRun, Optional.empty());
        setupBuildsApiBuildAccepted();

        // when
        final JiraSendInfoResponse response =
                classUnderTest.sendBuildInfo(jiraBuildInfoRequest).get(0);

        verify(issueKeyExtractor, never()).extractIssueKeys(any());
        assertThat(response.getStatus())
                .isEqualTo(JiraSendInfoResponse.Status.SUCCESS_BUILD_ACCEPTED);
        final String message = response.getMessage();
        assertThat(message).isNotBlank();
    }

    @Test
    public void testSendBuildInfo_whenNoJiraSpecified_sendsBuildsToAllJiras() {
        // given
        when(siteConfigRetriever.getAllJiraSites()).thenReturn(Arrays.asList(SITE, SITE2));
        setupBuildsApiBuildAccepted();

        // when
        final List<JiraSendInfoResponse> responses =
                classUnderTest.sendBuildInfo(createAllJirasRequest());

        // then
        assertThat(responses).hasSize(2);
        for (int idx = 0; idx < responses.size(); idx++) {
            final JiraSendInfoResponse response = responses.get(idx);
            assertThat(response.getStatus())
                    .isEqualTo(JiraSendInfoResponse.Status.SUCCESS_BUILD_ACCEPTED);
            final String message = response.getMessage();
            assertThat(message).isNotBlank();
        }
        verify(buildsApi, times(1)).sendBuild(eq(JIRA_SITE_CONFIG.getWebhookUrl()), any());
        verify(buildsApi, times(1)).sendBuild(eq(JIRA_SITE_CONFIG2.getWebhookUrl()), any());
    }

    private JiraBuildInfoRequest createOneJiraRequest() {
        return new MultibranchBuildInfoRequest(SITE, null, mockWorkflowRun(), Optional.empty());
    }

    private JiraBuildInfoRequest createAllJirasRequest() {
        return new MultibranchBuildInfoRequest(null, null, mockWorkflowRun(), Optional.empty());
    }

    private void setupMocks() {
        setupSiteConfigRetriever();
        setupSecretRetriever();
        setupIssueKeyExtractor();
        setupCloudIdResolver();
        setupRunWrapperProvider();
    }

    private void setupSiteConfigRetriever() {
        when(siteConfigRetriever.getJiraSiteConfig(eq(SITE)))
                .thenReturn(Optional.of(JIRA_SITE_CONFIG));
        when(siteConfigRetriever.getJiraSiteConfig(eq(SITE2)))
                .thenReturn(Optional.of(JIRA_SITE_CONFIG2));
    }

    private void setupSecretRetriever() {
        when(secretRetriever.getSecretFor(any())).thenReturn(Optional.of("secret"));
    }

    private void setupIssueKeyExtractor() {
        when(issueKeyExtractor.extractIssueKeys(any())).thenReturn(ImmutableSet.of("TEST-123"));
    }

    private void setupCloudIdResolver() {
        when(cloudIdResolver.getCloudId(eq("https://" + SITE))).thenReturn(Optional.of(CLOUD_ID));
        when(cloudIdResolver.getCloudId(eq("https://" + SITE2))).thenReturn(Optional.of(CLOUD_ID2));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void setupRunWrapperProvider() {
        try {
            final RunWrapper mockRunWrapper = mock(RunWrapper.class);
            when(mockRunWrapper.getFullProjectName())
                    .thenReturn("multibranch-1/TEST-123-branch-name");
            when(mockRunWrapper.getNumber()).thenReturn(1);
            when(mockRunWrapper.getDisplayName()).thenReturn("#1");
            when(mockRunWrapper.getAbsoluteUrl())
                    .thenReturn(
                            "http://localhost:8080/jenkins/multibranch-1/job/TEST-123-branch-name");

            final Run run = mock(Run.class);

            when(mockRunWrapper.getRawBuild()).thenReturn(run);
            when(runWrapperProvider.getWrapper(any())).thenReturn(mockRunWrapper);
        } catch (final AbortException e) {
            throw new RuntimeException(e);
        }
    }

    private void setupBuildsApiFailure() {
        when(buildsApi.sendBuild(any(), any())).thenThrow(new ApiUpdateFailedException("Error"));
    }

    private void setupBuildsApiBuildAccepted() {
        final BuildKeyResponse buildKeyResponse = new BuildKeyResponse(PIPELINE_ID, BUILD_NUMBER);
        final BuildApiResponse buildApiResponse =
                new BuildApiResponse(
                        ImmutableList.of(buildKeyResponse),
                        Collections.emptyList(),
                        Collections.emptyList());
        when(buildsApi.sendBuild(any(), any())).thenReturn(buildApiResponse);
    }

    private void setupBuildApiBuildRejected() {
        final BuildKeyResponse buildKeyResponse = new BuildKeyResponse(PIPELINE_ID, BUILD_NUMBER);
        final ApiErrorResponse errorResponse = new ApiErrorResponse("Error message");
        final RejectedBuildResponse buildResponse =
                new RejectedBuildResponse(buildKeyResponse, ImmutableList.of(errorResponse));

        final BuildApiResponse buildApiResponse =
                new BuildApiResponse(
                        Collections.emptyList(),
                        ImmutableList.of(buildResponse),
                        Collections.emptyList());
        when(buildsApi.sendBuild(any(), any())).thenReturn(buildApiResponse);
    }

    private void setupBuildApiUnknownIssueKeys() {
        final BuildApiResponse buildApiResponse =
                new BuildApiResponse(
                        Collections.emptyList(),
                        Collections.emptyList(),
                        ImmutableList.of("TEST-123"));
        when(buildsApi.sendBuild(any(), any())).thenReturn(buildApiResponse);
    }

    private static WorkflowRun mockWorkflowRun() {
        return mock(WorkflowRun.class);
    }

    private WorkflowRun changeSetWithOneChangeSetEntry() {
        final ChangeLogSet.Entry entry = mock(ChangeLogSet.Entry.class);
        when(entry.getMsg()).thenReturn("TEST-125 Commit message");
        final ChangeLogSet changeLogSet = mock(ChangeLogSet.class);
        when(changeLogSet.getItems()).thenReturn(new Object[] {entry});
        final WorkflowRun workflowRun = mock(WorkflowRun.class);

        when(workflowRun.getChangeSets()).thenReturn(ImmutableList.of(changeLogSet));
        return workflowRun;
    }
}
