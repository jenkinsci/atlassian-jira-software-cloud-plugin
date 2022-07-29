package com.atlassian.jira.cloud.jenkins.deploymentinfo.service;

import com.atlassian.jira.cloud.jenkins.common.client.ApiUpdateFailedException;
import com.atlassian.jira.cloud.jenkins.common.config.JiraSiteConfigRetriever;
import com.atlassian.jira.cloud.jenkins.common.model.ApiErrorResponse;
import com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse;
import com.atlassian.jira.cloud.jenkins.common.service.IssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudSiteConfig;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.DeploymentsApi;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.Association;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.AssociationType;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.Command;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.DeploymentApiResponse;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.DeploymentKeyResponse;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.Deployments;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.JiraDeploymentInfo;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.RejectedDeploymentResponse;
import com.atlassian.jira.cloud.jenkins.logging.PipelineLogger;
import com.atlassian.jira.cloud.jenkins.tenantinfo.CloudIdResolver;
import com.atlassian.jira.cloud.jenkins.util.RunWrapperProvider;
import com.atlassian.jira.cloud.jenkins.util.SecretRetriever;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import hudson.AbortException;
import hudson.model.Job;
import hudson.model.Run;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.annotation.Nullable;
import java.nio.channels.Pipe;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse.Status.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class JiraDeploymentInfoSenderImplTest {

    private static final String SITE = "example.atlassian.com";
    private static final String SITE2 = "example2.atlassian.com";
    public static final String ENVIRONMENT_ID = "prod-east-1";
    public static final String ENVIRONMENT_NAME = "prod-east-1";
    public static final String ENVIRONMENT_TYPE = "production";
    private static final String CLOUD_ID = "my-cloud-id";
    private static final String CLOUD_ID2 = "my-cloud-id2";
    public static final String PIPELINE_ID = UUID.randomUUID().toString();
    public static final int BUILD_NUMBER = 1;
    private static final JiraCloudSiteConfig JIRA_SITE_CONFIG =
            new JiraCloudSiteConfig(SITE, "https://webhook.url?jenkins_server_uuid=foo", "credsId");
    private static final JiraCloudSiteConfig JIRA_SITE_CONFIG2 =
            new JiraCloudSiteConfig(
                    SITE2, "https://webhook.url?jenkins_server_uuid=bar", "credsId2");

    @Mock private JiraSiteConfigRetriever siteConfigRetriever;

    @Mock private SecretRetriever secretRetriever;

    @Mock private CloudIdResolver cloudIdResolver;

    @Mock private DeploymentsApi deploymentsApi;

    @Mock private IssueKeyExtractor issueKeyExtractor;

    @Mock private RunWrapperProvider runWrapperProvider;

    private JiraDeploymentInfoSender classUnderTest;

    @Before
    public void setUp() {
        classUnderTest =
                new JiraDeploymentInfoSenderImpl(
                        siteConfigRetriever,
                        secretRetriever,
                        cloudIdResolver,
                        deploymentsApi,
                        issueKeyExtractor,
                        runWrapperProvider);

        setupMocks();
    }

    @Test
    public void testSendDeploymentInfo_whenSiteConfigNotFound() {
        // given
        when(siteConfigRetriever.getJiraSiteConfig(any())).thenReturn(Optional.empty());

        // when
        final JiraSendInfoResponse response =
                classUnderTest
                        .sendDeploymentInfo(createRequest(), PipelineLogger.noopInstance())
                        .get(0);

        // then
        assertThat(response.getStatus()).isEqualTo(FAILURE_SITE_CONFIG_NOT_FOUND);
    }

    @Test
    public void testSendDeploymentInfo_whenSecretNotFound() {
        // given
        when(secretRetriever.getSecretFor(any())).thenReturn(Optional.empty());

        // when
        final JiraSendInfoResponse response =
                classUnderTest
                        .sendDeploymentInfo(createRequest(), PipelineLogger.noopInstance())
                        .get(0);

        // then
        assertThat(response.getStatus()).isEqualTo(FAILURE_SECRET_NOT_FOUND);
    }

    @Test
    public void testSendDeploymentInfo_whenIssueKeysNotFound() {
        // given
        when(issueKeyExtractor.extractIssueKeys(any(), any())).thenReturn(Collections.emptySet());

        // when
        final JiraSendInfoResponse response =
                classUnderTest
                        .sendDeploymentInfo(createRequest(), PipelineLogger.noopInstance())
                        .get(0);

        // then
        assertThat(response.getStatus())
                .isEqualTo(SKIPPED_ISSUE_KEYS_NOT_FOUND_AND_SERVICE_IDS_ARE_EMPTY);
        final String message = response.getMessage();
        assertThat(message)
                .startsWith(
                        "No issue keys found in the change log and service ids were not provided");
    }

    @Test
    public void testSendDeploymentInfo_whenCloudIdNotFound() {
        // given
        when(cloudIdResolver.getCloudId(any())).thenReturn(Optional.empty());

        // when
        final JiraSendInfoResponse response =
                classUnderTest
                        .sendDeploymentInfo(createRequest(), PipelineLogger.noopInstance())
                        .get(0);

        // then
        assertThat(response.getStatus()).isEqualTo(FAILURE_SITE_NOT_FOUND);
    }

    @Test
    public void testSendDeploymentInfo_whenApiResponseFailure() {
        // given
        setupDeploymentsApiFailure();

        // when
        final JiraSendInfoResponse response =
                classUnderTest
                        .sendDeploymentInfo(createRequest(), PipelineLogger.noopInstance())
                        .get(0);

        // then
        assertThat(response.getStatus())
                .isEqualTo(JiraSendInfoResponse.Status.FAILURE_DEPLOYMENTS_API_RESPONSE);
    }

    @Test
    public void testSendDeploymentInfo_whenDeploymentAccepted() {
        // given
        setupDeploymentsApiDeploymentAccepted();

        // when
        final JiraSendInfoResponse response =
                classUnderTest
                        .sendDeploymentInfo(createRequest(), PipelineLogger.noopInstance())
                        .get(0);

        // then
        assertThat(response.getStatus())
                .isEqualTo(JiraSendInfoResponse.Status.SUCCESS_DEPLOYMENT_ACCEPTED);
        final String message = response.getMessage();
        assertThat(message).isNotBlank();
    }

    @Test
    public void testSendDeploymentInfo_whenDeploymentAcceptedWithExternalIssueKeys() {
        // given
        setupDeploymentsApiDeploymentAccepted();

        // when
        final JiraSendInfoResponse response =
                classUnderTest
                        .sendDeploymentInfo(
                                createRequestWithGating(SITE, ImmutableSet.of("TEST-123"), false),
                                PipelineLogger.noopInstance())
                        .get(0);

        // then
        verifyNoInteractions(issueKeyExtractor);
        assertThat(response.getStatus())
                .isEqualTo(JiraSendInfoResponse.Status.SUCCESS_DEPLOYMENT_ACCEPTED);
        final String message = response.getMessage();
        assertThat(message).isNotBlank();
    }

    @Test
    public void testSendDeploymentInfo_whenDeploymentRejected() {
        // given
        setupDeploymentsApiDeploymentRejected();

        // when
        final JiraSendInfoResponse response =
                classUnderTest
                        .sendDeploymentInfo(createRequest(), PipelineLogger.noopInstance())
                        .get(0);

        // then
        assertThat(response.getStatus())
                .isEqualTo(JiraSendInfoResponse.Status.FAILURE_DEPLOYMENT_REJECTED);
        final String message = response.getMessage();
        assertThat(message).isNotBlank();
    }

    @Test
    public void testSendDeploymentInfo_whenUnknownAssociations() {
        // given
        setupDeploymentApiUnknownIssueKeys();

        // when
        final JiraSendInfoResponse response =
                classUnderTest
                        .sendDeploymentInfo(createRequest(), PipelineLogger.noopInstance())
                        .get(0);

        assertThat(response.getStatus())
                .isEqualTo(JiraSendInfoResponse.Status.FAILURE_UNKNOWN_ASSOCIATIONS);
        final String message = response.getMessage();
        assertThat(message).isNotBlank();
    }

    @Test
    public void testSendDeploymentInfo_whenMissingEnvironmentIdAndEnvironmentName() {
        // given
        final JiraDeploymentInfoRequest request = createRequest(null, null, null);

        // when
        final JiraSendInfoResponse response =
                classUnderTest.sendDeploymentInfo(request, PipelineLogger.noopInstance()).get(0);

        // then
        assertThat(response.getStatus())
                .isEqualTo(JiraSendInfoResponse.Status.FAILURE_ENVIRONMENT_INVALID);
        assertThat(response.getMessage())
                .isEqualTo(
                        "The deployment environment is not valid. "
                                + "The parameter environmentId is required. The parameter environmentName is required.");
    }

    @Test
    public void testSendDeploymentInfo_whenNotAllowedEnvironmentType() {
        // given
        final JiraDeploymentInfoRequest request =
                createRequest("prod-east", "Production East", "foobar");

        // when
        final JiraSendInfoResponse response =
                classUnderTest.sendDeploymentInfo(request, PipelineLogger.noopInstance()).get(0);

        // then
        assertThat(response.getStatus())
                .isEqualTo(JiraSendInfoResponse.Status.FAILURE_ENVIRONMENT_INVALID);
        assertThat(response.getMessage())
                .isEqualTo(
                        "The deployment environment is not valid. "
                                + "The parameter environmentType is not valid. "
                                + "Allowed values are: [development, testing, staging, production, unmapped]");
    }

    @Test
    public void testSendDeploymentInfo_whenNotAllowedDeploymentState() {
        // given
        final JiraDeploymentInfoRequest request = createRequest("test");
        final WorkflowRun mockWorkflowRun = request.getDeployment();

        // when
        final JiraSendInfoResponse response =
                classUnderTest.sendDeploymentInfo(request, PipelineLogger.noopInstance()).get(0);

        // then
        verify(mockWorkflowRun, never()).getResult();
        assertThat(response.getStatus())
                .isEqualTo(JiraSendInfoResponse.Status.FAILURE_STATE_INVALID);
        assertThat(response.getMessage()).contains("The deployment state is not valid.");
    }

    @Test
    public void testSendDeploymentInfo_whenGateEnabled() {
        // given
        setupDeploymentsApiDeploymentAccepted();
        final JiraDeploymentInfoRequest request =
                new JiraDeploymentInfoRequest(
                        SITE,
                        ENVIRONMENT_ID,
                        ENVIRONMENT_NAME,
                        ENVIRONMENT_TYPE,
                        "pending",
                        Collections.emptySet(),
                        Boolean.TRUE,
                        Collections.emptySet(),
                        mockWorkflowRun());

        final ArgumentCaptor<Deployments> deploymentsArgumentCaptor =
                ArgumentCaptor.forClass(Deployments.class);

        // when
        final JiraSendInfoResponse response =
                classUnderTest.sendDeploymentInfo(request, PipelineLogger.noopInstance()).get(0);

        // then
        assertThat(response.getStatus())
                .isEqualTo(JiraSendInfoResponse.Status.SUCCESS_DEPLOYMENT_ACCEPTED);
        verify(deploymentsApi)
                .sendDeploymentAsJwt(any(), deploymentsArgumentCaptor.capture(), any());
        final JiraDeploymentInfo jiraDeploymentInfo =
                deploymentsArgumentCaptor.getValue().getDeployments().get(0);
        assertThat(jiraDeploymentInfo.getCommands())
                .contains(new Command("initiate_deployment_gating"));
    }

    @Test
    public void testSendDeploymentInfo_whenNoJiraSpecified_sendsBuildsToAllJiras() {
        // given
        when(siteConfigRetriever.getAllJiraSites()).thenReturn(Arrays.asList(SITE, SITE2));
        setupDeploymentsApiDeploymentAccepted();

        // when
        final List<JiraSendInfoResponse> responses =
                classUnderTest.sendDeploymentInfo(
                        createAllJirasRequest(false), PipelineLogger.noopInstance());

        // then
        assertThat(responses).hasSize(2);
        for (int idx = 0; idx < responses.size(); idx++) {
            final JiraSendInfoResponse response = responses.get(idx);
            assertThat(response.getStatus())
                    .isEqualTo(JiraSendInfoResponse.Status.SUCCESS_DEPLOYMENT_ACCEPTED);
            final String message = response.getMessage();
            assertThat(message).isNotBlank();
        }
        verify(deploymentsApi, times(1))
                .sendDeploymentAsJwt(eq(JIRA_SITE_CONFIG.getWebhookUrl()), any(), any());
        verify(deploymentsApi, times(1))
                .sendDeploymentAsJwt(eq(JIRA_SITE_CONFIG2.getWebhookUrl()), any(), any());
    }

    @Test
    public void testSendDeploymentInfo_withMultipleJiras_withoutSite_withEnabledGating_fails() {
        // given
        when(siteConfigRetriever.getAllJiraSites()).thenReturn(Arrays.asList(SITE, SITE2));

        // when
        final List<JiraSendInfoResponse> responses =
                classUnderTest.sendDeploymentInfo(
                        createAllJirasRequest(true), PipelineLogger.noopInstance());

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getStatus())
                .isEqualTo(JiraSendInfoResponse.Status.FAILURE_DEPLOYMENT_GATING_MANY_JIRAS);
        verify(deploymentsApi, times(0)).sendDeploymentAsJwt(any(), any(), any());
    }

    @Test
    public void getDeploymentState_whenUsedJenkinsRunState() {
        // given
        when(issueKeyExtractor.extractIssueKeys(any(), any())).thenReturn(Collections.emptySet());
        JiraDeploymentInfoRequest request = createRequest();
        final WorkflowRun mockWorkflowRun = request.getDeployment();

        // when
        classUnderTest.sendDeploymentInfo(request, PipelineLogger.noopInstance()).get(0);

        // then
        verify(mockWorkflowRun, times(1)).getResult();
    }

    private JiraDeploymentInfoRequest createRequest() {
        return createRequestWithGating(SITE, Collections.emptySet(), false);
    }

    private JiraDeploymentInfoRequest createAllJirasRequest(final boolean enableGating) {
        return createRequestWithGating(null, Collections.emptySet(), enableGating);
    }

    private JiraDeploymentInfoRequest createRequestWithGating(
            @Nullable final String site, final Set<String> issueKeys, final boolean enableGating) {
        return new JiraDeploymentInfoRequest(
                site,
                ENVIRONMENT_ID,
                ENVIRONMENT_NAME,
                ENVIRONMENT_TYPE,
                null,
                Collections.emptySet(),
                enableGating,
                issueKeys,
                mockWorkflowRun());
    }

    private JiraDeploymentInfoRequest createRequest(final String state) {
        return new JiraDeploymentInfoRequest(
                SITE,
                ENVIRONMENT_ID,
                ENVIRONMENT_NAME,
                ENVIRONMENT_TYPE,
                state,
                Collections.emptySet(),
                Boolean.FALSE,
                Collections.emptySet(),
                mockWorkflowRun());
    }

    private JiraDeploymentInfoRequest createRequest(
            final String environmentId,
            final String environmentName,
            final String environmentType) {
        return new JiraDeploymentInfoRequest(
                SITE,
                environmentId,
                environmentName,
                environmentType,
                null,
                Collections.emptySet(),
                Boolean.FALSE,
                Collections.emptySet(),
                mockWorkflowRun());
    }

    private void setupMocks() {
        setupSiteConfigRetriever();
        setupSecretRetriever();
        setupCloudIdResolver();
        setupChangeLogExtractor();
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

    private void setupCloudIdResolver() {
        when(cloudIdResolver.getCloudId(eq("https://" + SITE))).thenReturn(Optional.of(CLOUD_ID));
        when(cloudIdResolver.getCloudId(eq("https://" + SITE2))).thenReturn(Optional.of(CLOUD_ID2));
    }

    private void setupChangeLogExtractor() {
        when(issueKeyExtractor.extractIssueKeys(any(), any()))
                .thenReturn(ImmutableSet.of("TEST-123"));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void setupRunWrapperProvider() {
        try {
            final RunWrapper mockRunWrapper = mock(RunWrapper.class);
            when(mockRunWrapper.getFullProjectName())
                    .thenReturn("multibranch-1/TEST-123-branch-name");
            when(mockRunWrapper.getDisplayName()).thenReturn("#1");
            when(mockRunWrapper.getAbsoluteUrl())
                    .thenReturn(
                            "http://localhost:8080/jenkins/multibranch-1/job/TEST-123-branch-name");

            final Job job = mock(Job.class);
            when(job.getAbsoluteUrl())
                    .thenReturn(
                            "http://localhost:8080/jenkins/multibranch-1/job/TEST-123-branch-name");

            final Run run = mock(Run.class);
            when(run.getParent()).thenReturn(job);

            when(mockRunWrapper.getRawBuild()).thenReturn(run);

            when(runWrapperProvider.getWrapper(any())).thenReturn(mockRunWrapper);
        } catch (final AbortException e) {
            throw new RuntimeException(e);
        }
    }

    private void setupDeploymentsApiFailure() {
        when(deploymentsApi.sendDeploymentAsJwt(any(), any(), any()))
                .thenThrow(new ApiUpdateFailedException("Error"));
    }

    private void setupDeploymentsApiDeploymentAccepted() {
        final DeploymentKeyResponse deploymentKeyResponse =
                new DeploymentKeyResponse(PIPELINE_ID, ENVIRONMENT_ID, BUILD_NUMBER);
        final DeploymentApiResponse deploymentApiResponse =
                new DeploymentApiResponse(
                        ImmutableList.of(deploymentKeyResponse),
                        Collections.emptyList(),
                        Collections.emptyList());
        when(deploymentsApi.sendDeploymentAsJwt(any(), any(), any()))
                .thenReturn(deploymentApiResponse);
    }

    private void setupDeploymentsApiDeploymentRejected() {
        final DeploymentKeyResponse deploymentKeyResponse =
                new DeploymentKeyResponse(PIPELINE_ID, ENVIRONMENT_ID, BUILD_NUMBER);
        final ApiErrorResponse errorResponse = new ApiErrorResponse("Error message");
        final RejectedDeploymentResponse deploymentResponse =
                new RejectedDeploymentResponse(
                        deploymentKeyResponse, ImmutableList.of(errorResponse));

        final DeploymentApiResponse deploymentApiResponse =
                new DeploymentApiResponse(
                        Collections.emptyList(),
                        ImmutableList.of(deploymentResponse),
                        Collections.emptyList());
        when(deploymentsApi.sendDeploymentAsJwt(any(), any(), any()))
                .thenReturn(deploymentApiResponse);
    }

    private void setupDeploymentApiUnknownIssueKeys() {
        final DeploymentApiResponse deploymentApiResponse =
                new DeploymentApiResponse(
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.singletonList(
                                Association.builder()
                                        .withAssociationType(AssociationType.ISSUE_KEYS)
                                        .withValues(ImmutableSet.of("TEST-123"))
                                        .build()));
        when(deploymentsApi.sendDeploymentAsJwt(any(), any(), any()))
                .thenReturn(deploymentApiResponse);
    }

    private static WorkflowRun mockWorkflowRun() {
        return mock(WorkflowRun.class);
    }
}
