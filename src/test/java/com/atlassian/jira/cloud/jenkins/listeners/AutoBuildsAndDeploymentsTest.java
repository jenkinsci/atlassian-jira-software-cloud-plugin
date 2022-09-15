package com.atlassian.jira.cloud.jenkins.listeners;

import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.BuildApiResponse;
import com.atlassian.jira.cloud.jenkins.buildinfo.service.JiraBuildInfoResponse;
import com.atlassian.jira.cloud.jenkins.buildinfo.service.JiraBuildInfoSender;
import com.atlassian.jira.cloud.jenkins.buildinfo.service.MultibranchBuildInfoRequest;
import com.atlassian.jira.cloud.jenkins.common.factory.JiraSenderFactory;
import com.atlassian.jira.cloud.jenkins.common.service.IssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudPluginConfig;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudSiteConfig;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.DeploymentApiResponse;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.State;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.service.JiraDeploymentInfoRequest;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.service.JiraDeploymentInfoResponse;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.service.JiraDeploymentInfoSender;
import com.atlassian.jira.cloud.jenkins.logging.PipelineLogger;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import com.google.common.collect.ImmutableList;
import hudson.model.Result;
import hudson.model.listeners.RunListener;
import hudson.util.Secret;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.flow.GraphListener;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AutoBuildsAndDeploymentsTest {

    private static final String SITE = "example.atlassian.net";
    private static final String CREDENTIAL_ID = UUID.randomUUID().toString();

    @ClassRule
    public static BuildWatcher buildWatcher = new BuildWatcher();

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    private final JiraSenderFactory mockSenderFactory = mock(JiraSenderFactory.class);

    private JiraBuildInfoSender jiraBuildInfoSender;

    private JiraDeploymentInfoSender jiraDeploymentInfoSender;

    private final IssueKeyExtractor issueKeyExtractor = Mockito.mock(IssueKeyExtractor.class);

    private final Logger logger = LoggerFactory.getLogger(AutoBuildsAndDeploymentsTest.class);

    @Before
    public void setUp() throws Exception {
        assertListenersRegistered();
        givenJiraSiteConfigured();
        givenCredentials();
        givenJiraAcceptsBuilds();
        givenJiraAcceptsDeployments();
        givenAutoBuildsRegex(null);
        JiraSenderFactory.setInstance(mockSenderFactory);
        JiraCloudPluginConfig.get().setDebugLogging(Boolean.TRUE);
    }

    private void assertListenersRegistered() {

        Optional<RunListener> existingListener = jenkins.getInstance()
                .getExtensionList(RunListener.class)
                .stream()
                .filter(listener -> listener instanceof JenkinsPipelineRunListener)
                .findFirst();

        if (existingListener.isPresent()) {
            logger.info("found existing {} ... removing it", JenkinsPipelineRunListener.class.getName());
            jenkins.getInstance()
                    .getExtensionList(RunListener.class)
                    .remove(existingListener.get());
        }

        jenkins.getInstance()
                .getExtensionList(RunListener.class)
                .add(0, new JenkinsPipelineRunListener(issueKeyExtractor));


        // Checking that the JenkinsPipelineRunListener is registered exactly once.
        assertThat(
                (int) jenkins.getInstance()
                        .getExtensionList(RunListener.class)
                        .stream()
                        .filter(listener -> listener instanceof JenkinsPipelineRunListener)
                        .count())
                .isEqualTo(1);

        // Checking that the JenkinsPipelineGraphListener is registered exactly once.
        assertThat(
                jenkins.getInstance()
                        .getExtensionList(GraphListener.class)
                        .stream()
                        .filter(
                                listener ->
                                        listener instanceof JenkinsPipelineGraphListener)
                        .count())
                .isEqualTo(1);
    }

    private void givenIssueKeys() {
        when(issueKeyExtractor.extractIssueKeys(any(), any()))
                .thenReturn(Stream.of("TEST-1", "TEST-2").collect(Collectors.toSet()));
    }

    private void givenNoIssueKeys() {
        when(issueKeyExtractor.extractIssueKeys(any(), any())).thenReturn(emptySet());
    }

    private void givenJiraSiteConfigured() {
        JiraCloudPluginConfig.get()
                .setSites(
                        ImmutableList.of(
                                new JiraCloudSiteConfig(
                                        SITE, "https://webhook.url", CREDENTIAL_ID)));
    }

    private void givenAutoBuildsEnabled() {
        JiraCloudPluginConfig.get().setAutoBuildsEnabled(true);
    }

    private void givenAutoBuildsDisabled() {
        JiraCloudPluginConfig.get().setAutoBuildsEnabled(false);
    }

    private void givenAutoDeploymentsEnabled() {
        JiraCloudPluginConfig.get().setAutoDeploymentsEnabled(true);
    }

    private void givenAutoDeploymentsDisabled() {
        JiraCloudPluginConfig.get().setAutoDeploymentsEnabled(false);
    }

    private void givenAutoBuildsRegex(String regex) {
        JiraCloudPluginConfig.get().setAutoBuildsRegex(regex);
    }

    private void givenAutoDeploymentsRegex(String regex) {
        JiraCloudPluginConfig.get().setAutoDeploymentsRegex(regex);
    }

    private void givenCredentials() throws IOException {
        CredentialsProvider.lookupStores(jenkins.getInstance())
                .iterator()
                .next()
                .addCredentials(Domain.global(), secretCredential());
    }

    private void givenJiraAcceptsBuilds() {
        jiraBuildInfoSender = mock(JiraBuildInfoSender.class);
        when(mockSenderFactory.getJiraBuildInfoSender()).thenReturn(jiraBuildInfoSender);

        final BuildApiResponse response =
                new BuildApiResponse(
                        Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        final JiraBuildInfoResponse buildAccepted =
                JiraBuildInfoResponse.successBuildAccepted(SITE, response);
        when(jiraBuildInfoSender.sendBuildInfo(any(), any()))
                .thenReturn(Collections.singletonList(buildAccepted));
    }

    private void givenJiraAcceptsDeployments() {
        jiraDeploymentInfoSender = mock(JiraDeploymentInfoSender.class);
        when(mockSenderFactory.getJiraDeploymentInfoSender()).thenReturn(jiraDeploymentInfoSender);
        final DeploymentApiResponse response =
                new DeploymentApiResponse(
                        Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        final JiraDeploymentInfoResponse deploymentAccepted =
                JiraDeploymentInfoResponse.successDeploymentAccepted(SITE, response);
        when(jiraDeploymentInfoSender.sendDeploymentInfo(any(), any()))
                .thenReturn(Collections.singletonList(deploymentAccepted));
    }

    @Test
    public void whenNoAutoBuildsRegex_thenSendsInProgressAndSuccessBuildEvents() throws Exception {
        WorkflowJob workflow = givenWorkflowFromFile("auto-build.groovy");
        givenAutoBuildsEnabled();
        givenIssueKeys();

        jenkins.assertBuildStatusSuccess(workflow.scheduleBuild2(0));

        verifyBuildEvent(0, State.IN_PROGRESS);
        verifyBuildEvent(1, State.SUCCESSFUL);
    }

    @Test
    public void
    whenAutoBuildsRegexMatching_thenSendsInProgressAndSuccessBuildEventsForFirstMatchingStep()
            throws Exception {
        WorkflowJob workflow = givenWorkflowFromFile("auto-build-with-multiple-build-steps.groovy");
        givenAutoBuildsEnabled();
        givenAutoBuildsRegex("^build.*$");
        givenIssueKeys();

        jenkins.assertBuildStatusSuccess(workflow.scheduleBuild2(0));

        verifyBuildEvent(0, State.IN_PROGRESS);
        verifyBuildEvent(1, State.SUCCESSFUL);
    }

    @Test
    public void whenAutoBuildsRegexNotMatching_thenSendsNoBuildEvents() throws Exception {
        WorkflowJob workflow = givenWorkflowFromFile("auto-build-with-multiple-build-steps.groovy");
        givenAutoBuildsEnabled();
        givenAutoBuildsRegex("^foo.*$");
        givenIssueKeys();

        jenkins.assertBuildStatusSuccess(workflow.scheduleBuild2(0));

        verifyNoBuildEvents();
    }

    @Test
    public void whenBuildFailing_thenSendsInProgressAndFailedBuildEvents() throws Exception {
        WorkflowJob workflow = givenWorkflowFromFile("auto-build-failing.groovy");
        givenAutoBuildsEnabled();
        givenIssueKeys();

        jenkins.assertBuildStatus(Result.FAILURE, workflow.scheduleBuild2(0));

        verifyBuildEvent(0, State.IN_PROGRESS);
        verifyBuildEvent(1, State.FAILED);
    }

    @Test
    public void whenNoIssueKeys_thenSendsNoBuildEvents() throws Exception {
        WorkflowJob workflow = givenWorkflowFromFile("auto-build.groovy");
        givenAutoBuildsEnabled();
        givenNoIssueKeys();

        jenkins.assertBuildStatus(Result.SUCCESS, workflow.scheduleBuild2(0));

        verifyNoBuildEvents();
    }

    @Test
    public void whenAutoBuildsDisabled_thenSendsNoBuildEvents() throws Exception {
        WorkflowJob workflow = givenWorkflowFromFile("auto-build.groovy");
        givenAutoBuildsDisabled();
        givenIssueKeys();

        jenkins.assertBuildStatus(Result.SUCCESS, workflow.scheduleBuild2(0));

        verifyNoBuildEvents();
    }

    @Test
    public void whenNoIssueKeys_thenSendsNoDeploymentEvents() throws Exception {
        WorkflowJob workflow = givenWorkflowFromFile("auto-deployment.groovy");
        givenAutoDeploymentsEnabled();
        givenNoIssueKeys();

        jenkins.assertBuildStatus(Result.SUCCESS, workflow.scheduleBuild2(0));

        verifyNoDeploymentEvents();
    }

    @Test
    public void whenAutoDeploymentsDisabled_thenSendsNoDeploymentEvents() throws Exception {
        WorkflowJob workflow = givenWorkflowFromFile("auto-deployment.groovy");
        givenAutoDeploymentsDisabled();
        givenIssueKeys();

        jenkins.assertBuildStatus(Result.SUCCESS, workflow.scheduleBuild2(0));

        verifyNoDeploymentEvents();
    }

    @Test
    public void whenNoAutoDeploymentsRegex_thenSendsNoDeploymentEvents() throws Exception {
        WorkflowJob workflow = givenWorkflowFromFile("auto-deployment.groovy");
        givenIssueKeys();
        givenAutoDeploymentsEnabled();
        givenAutoDeploymentsRegex(null);

        jenkins.assertBuildStatusSuccess(workflow.scheduleBuild2(0));

        verifyNoDeploymentEvents();
    }

    @Test
    public void whenAutoDeploymentsRegexWithoutEnvName_thenSendsNoDeploymentEvents()
            throws Exception {
        WorkflowJob workflow = givenWorkflowFromFile("auto-deployment.groovy");
        givenIssueKeys();
        givenAutoDeploymentsEnabled();
        givenAutoDeploymentsRegex("^deploy.*");

        jenkins.assertBuildStatusSuccess(workflow.scheduleBuild2(0));

        verifyNoDeploymentEvents();
    }

    @Test
    public void whenAutoDeploymentsRegexWithEnvName_thenSendsDeploymentInProgressAndSuccessEvents()
            throws Exception {
        WorkflowJob workflow = givenWorkflowFromFile("auto-deployment.groovy");
        givenIssueKeys();
        givenAutoDeploymentsEnabled();
        givenAutoDeploymentsRegex("^deploy to (?<envName>.*) ?.*$");

        jenkins.assertBuildStatusSuccess(workflow.scheduleBuild2(0));

        verifyDeploymentEvent(0, State.IN_PROGRESS, "stg");
        verifyDeploymentEvent(1, State.IN_PROGRESS, "prod");

        verifyDeploymentEvent(2, State.SUCCESSFUL, "stg");
        verifyDeploymentEvent(3, State.SUCCESSFUL, "prod");
    }

    @Test
    public void whenAutoDeploymentsFailing_thenSendsFailedDeploymentEvent() throws Exception {
        WorkflowJob workflow = givenWorkflowFromFile("auto-deployment-failing.groovy");
        givenIssueKeys();
        givenAutoDeploymentsEnabled();
        givenAutoDeploymentsRegex("^deploy to (?<envName>.*) ?.*$");

        jenkins.assertBuildStatus(Result.FAILURE, workflow.scheduleBuild2(0));

        verifyDeploymentEvent(0, State.IN_PROGRESS, "stg");
        verifyDeploymentEvent(1, State.SUCCESSFUL, "stg");

        verifyDeploymentEvent(2, State.IN_PROGRESS, "prod");
        verifyDeploymentEvent(3, State.FAILED, "prod");
    }

    @Test
    public void whenAutoBuildsAndDeployments_thenSendsBuildAndDeploymentEvents() throws Exception {
        WorkflowJob workflow = givenWorkflowFromFile("auto-build-and-deployment.groovy");
        givenIssueKeys();

        givenAutoBuildsEnabled();
        givenAutoBuildsRegex("^build.*$");

        givenAutoDeploymentsEnabled();
        givenAutoDeploymentsRegex("^deploy to (?<envName>.*) ?.*$");

        jenkins.assertBuildStatus(Result.FAILURE, workflow.scheduleBuild2(0));

        verifyBuildEvent(0, State.IN_PROGRESS);
        verifyBuildEvent(1, State.SUCCESSFUL);

        verifyDeploymentEvent(0, State.IN_PROGRESS, "stg");
        verifyDeploymentEvent(1, State.SUCCESSFUL, "stg");

        verifyDeploymentEvent(2, State.IN_PROGRESS, "prod");
        verifyDeploymentEvent(3, State.FAILED, "prod");
    }

    @NotNull
    private WorkflowJob givenWorkflowFromFile(String workflowFile)
            throws IOException, URISyntaxException {
        WorkflowJob project = jenkins.createProject(WorkflowJob.class);

        String flowDefinition =
                new String(
                        Files.readAllBytes(
                                Paths.get(ClassLoader.getSystemResource(workflowFile).toURI())));
        project.setDefinition(new CpsFlowDefinition(flowDefinition, true));
        return project;
    }

    private static BaseStandardCredentials secretCredential() {
        return new StringCredentialsImpl(
                CredentialsScope.GLOBAL, CREDENTIAL_ID, "test-secret", Secret.fromString("secret"));
    }

    private void verifyNoBuildEvents() {
        verify(jiraBuildInfoSender, never()).sendBuildInfo(any(), any());
    }

    private void verifyNoDeploymentEvents() {
        verify(jiraDeploymentInfoSender, never()).sendDeploymentInfo(any(), any());
    }

    private void verifyBuildEvent(int index, State expectedState) {
        ArgumentCaptor<MultibranchBuildInfoRequest> requestCaptor =
                ArgumentCaptor.forClass(MultibranchBuildInfoRequest.class);
        verify(jiraBuildInfoSender, atLeastOnce()).sendBuildInfo(requestCaptor.capture(), any());

        List<MultibranchBuildInfoRequest> requests = requestCaptor.getAllValues();
        if (index + 1 > requests.size()) {
            fail(
                    String.format(
                            "Expecting at least %d requests to Jira, but got only %d",
                            index + 1, requests.size()));
        }
        assertThat(requests.get(index).getJiraState()).isEqualTo(expectedState);
    }

    private void verifyDeploymentEvent(
            int index, State expectedState, String expectedEnvironmentName) {
        ArgumentCaptor<JiraDeploymentInfoRequest> requestCaptor =
                ArgumentCaptor.forClass(JiraDeploymentInfoRequest.class);
        verify(jiraDeploymentInfoSender, atLeastOnce())
                .sendDeploymentInfo(requestCaptor.capture(), any(PipelineLogger.class));

        List<JiraDeploymentInfoRequest> requests = requestCaptor.getAllValues();
        if (index + 1 > requests.size()) {
            fail(
                    String.format(
                            "Expecting at least %d requests to Jira, but got only %d",
                            index + 1, requests.size()));
        }

        // Need to translate a "null" state to "in progress" because the translation is done outside
        // the bounds
        // of the environment that is mocked for this integration test.
        JiraDeploymentInfoRequest request = requests.get(index);
        String actualState = request.getState();
        if (actualState == null) {
            actualState = State.IN_PROGRESS.value;
        }

        assertThat(actualState).isEqualTo(expectedState.value);
        assertThat(request.getEnvironmentName()).isEqualTo(expectedEnvironmentName);
    }
}
