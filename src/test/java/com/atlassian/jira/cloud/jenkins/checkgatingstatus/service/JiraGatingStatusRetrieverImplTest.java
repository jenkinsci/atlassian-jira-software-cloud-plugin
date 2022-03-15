package com.atlassian.jira.cloud.jenkins.checkgatingstatus.service;

import com.atlassian.jira.cloud.jenkins.checkgatingstatus.client.GatingStatusApi;
import com.atlassian.jira.cloud.jenkins.checkgatingstatus.client.model.GatingStatus;
import com.atlassian.jira.cloud.jenkins.checkgatingstatus.client.model.GatingStatusResponse;
import com.atlassian.jira.cloud.jenkins.common.config.JiraSiteConfig2Retriever;
import com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudSiteConfig2;
import com.atlassian.jira.cloud.jenkins.tenantinfo.CloudIdResolver;
import com.atlassian.jira.cloud.jenkins.util.SecretRetriever;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse.Status.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JiraGatingStatusRetrieverImplTest {

    public static final String ENVIRONMENT_ID = "prod-east-1";
    public static final String PIPELINE_ID = UUID.randomUUID().toString();
    public static final int BUILD_NUMBER = 1;
    private static final String SITE = "example.atlassian.com";
    private static final String CLOUD_ID = UUID.randomUUID().toString();
    private static final JiraCloudSiteConfig2 JIRA_SITE_CONFIG =
            new JiraCloudSiteConfig2(SITE, "clientId", "credsId");

    @Mock
    private JiraSiteConfig2Retriever siteConfigRetriever;
    @Mock
    private SecretRetriever secretRetriever;
    @Mock
    private CloudIdResolver cloudIdResolver;
    @Mock
    private GatingStatusApi jiraApi;
    @Mock
    private WorkflowRun run;

    private JiraGatingStatusRetrieverImpl classUnderTest;

    @Before
    public void before() throws Exception {
        classUnderTest =
                new JiraGatingStatusRetrieverImpl(
                        siteConfigRetriever, secretRetriever, cloudIdResolver, jiraApi);

        setupMocks();
    }

    @Test
    public void testGetGateState_whenSiteConfigNotFound() {
        // given
        when(siteConfigRetriever.getJiraSiteConfig(any())).thenReturn(Optional.empty());

        // when
        final JiraGatingStatusResponse response =
                classUnderTest.getGatingStatus(SITE, ENVIRONMENT_ID, run);

        // then
        assertThat(response.getStatus()).isEqualTo(FAILURE_SITE_CONFIG_NOT_FOUND);
    }

    @Test
    public void testGetGateState_whenSecretNotFound() {
        // given
        when(secretRetriever.getSecretFor(any())).thenReturn(Optional.empty());

        // when
        final JiraGatingStatusResponse response =
                classUnderTest.getGatingStatus(SITE, ENVIRONMENT_ID, run);

        // then
        assertThat(response.getStatus()).isEqualTo(FAILURE_SECRET_NOT_FOUND);
    }

    @Test
    public void testGetGateState_whenCloudIdNotFound() {
        // given
        when(cloudIdResolver.getCloudId(any())).thenReturn(Optional.empty());

        // when
        final JiraGatingStatusResponse response =
                classUnderTest.getGatingStatus(SITE, ENVIRONMENT_ID, run);

        // then
        assertThat(response.getStatus()).isEqualTo(FAILURE_SITE_NOT_FOUND);
    }

    @Test
    public void testGetGateState_whenApiResponseFailure() {
        // given
        setupApiFailure();

        // when
        final JiraGatingStatusResponse response =
                classUnderTest.getGatingStatus(SITE, ENVIRONMENT_ID, run);

        // then
        assertThat(response.getStatus()).isEqualTo(JiraSendInfoResponse.Status.FAILURE_GATE_CHECK);
    }

    @Test
    public void testGetGateState_whenApiResponseSuccess() {
        // given
        setupApiSuccess();

        // when
        final JiraGatingStatusResponse response =
                classUnderTest.getGatingStatus(SITE, ENVIRONMENT_ID, run);

        // then
        assertThat(response.getStatus()).isEqualTo(JiraSendInfoResponse.Status.SUCCESS_GATE_CHECK);
    }

    private void setupMocks() {
        setupSiteConfigRetriever();
        setupSecretRetriever();
        setupCloudIdResolver();
        setupRun();
    }

    private void setupSiteConfigRetriever() {
        when(siteConfigRetriever.getJiraSiteConfig(any()))
                .thenReturn(Optional.of(JIRA_SITE_CONFIG));
    }

    private void setupSecretRetriever() {
        when(secretRetriever.getSecretFor(any())).thenReturn(Optional.of("secret"));
    }

    private void setupCloudIdResolver() {
        when(cloudIdResolver.getCloudId(any())).thenReturn(Optional.of(CLOUD_ID));
    }

    private void setupRun() {
        final WorkflowJob workflowJob = mock(WorkflowJob.class);
        when(run.getNumber()).thenReturn(BUILD_NUMBER);
        when(workflowJob.getFullName()).thenReturn(PIPELINE_ID);
        when(run.getParent()).thenReturn(workflowJob);
    }

    private void setupApiFailure() {
        when(jiraApi.getGatingStatus(any(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("BWAAAH!"));
    }

    private void setupApiSuccess() {
        final GatingStatusResponse gatingStatusResponse =
                new GatingStatusResponse(
                        LocalDateTime.now().toString(),
                        GatingStatus.AWAITING,
                        Collections.emptyList(),
                        PIPELINE_ID,
                        ENVIRONMENT_ID,
                        BUILD_NUMBER);
        when(jiraApi.getGatingStatus(any(), any(), any(), any(), any()))
                .thenReturn(gatingStatusResponse);
    }
}
