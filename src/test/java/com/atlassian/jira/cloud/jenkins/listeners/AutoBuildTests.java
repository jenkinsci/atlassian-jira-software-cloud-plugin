package com.atlassian.jira.cloud.jenkins.listeners;

import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.BuildApiResponse;
import com.atlassian.jira.cloud.jenkins.buildinfo.service.JiraBuildInfoResponse;
import com.atlassian.jira.cloud.jenkins.buildinfo.service.JiraBuildInfoSender;
import com.atlassian.jira.cloud.jenkins.buildinfo.service.MultibranchBuildInfoRequest;
import com.atlassian.jira.cloud.jenkins.common.factory.JiraSenderFactory;
import com.atlassian.jira.cloud.jenkins.common.service.IssueKeyExtractor;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudPluginConfig;
import com.atlassian.jira.cloud.jenkins.config.JiraCloudSiteConfig;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.State;
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

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AutoBuildTests {

    private static final String SITE = "example.atlassian.net";
    private static final String CLIENT_ID = UUID.randomUUID().toString();
    private static final String CREDENTIAL_ID = UUID.randomUUID().toString();

    @ClassRule public static BuildWatcher buildWatcher = new BuildWatcher();

    @Rule public JenkinsRule jenkins = new JenkinsRule();

    private JiraBuildInfoSender jiraBuildInfoSender;

    private final IssueKeyExtractor issueKeyExtractor = Mockito.mock(IssueKeyExtractor.class);

    @Before
    public void setUp() throws Exception {
        assertListenersRegistered();
        givenJiraSiteConfigured();
        givenCredentials();
        givenJiraApiAcceptsBuilds();
        givenAutoBuildsRegex(null);
    }

    private void assertListenersRegistered() {

        // Adding a custom JenkinsPipelineListener so we can inject mocks.
        // The default listener instance will also still be registered (I didn't find a way to
        // unregister it),
        // but it will not send any requests to Jira because it won't find any issue keys.
        jenkins.getInstance()
                .getExtensionList(RunListener.class)
                .add(0, new JenkinsPipelineRunListener(this.issueKeyExtractor));

        // Checking that the JenkinsPipelineRunListener is registered.
        assertThat(
                        jenkins.getInstance()
                                .getExtensionList(RunListener.class)
                                .stream()
                                .filter(listener -> listener instanceof JenkinsPipelineRunListener)
                                .findFirst())
                .isNotEmpty();

        // Checking that the JenkinsPipelineGraphListener is registered.
        assertThat(
                        jenkins.getInstance()
                                .getExtensionList(GraphListener.class)
                                .stream()
                                .filter(
                                        listener ->
                                                listener instanceof JenkinsPipelineGraphListener)
                                .findFirst())
                .isNotEmpty();
    }

    private void givenIssueKeys() {
        when(issueKeyExtractor.extractIssueKeys(any()))
                .thenReturn(Stream.of("TEST-1", "TEST-2").collect(Collectors.toSet()));
    }

    private void givenJiraSiteConfigured() {
        JiraCloudPluginConfig.get()
                .setSites(
                        ImmutableList.of(new JiraCloudSiteConfig(SITE, CLIENT_ID, CREDENTIAL_ID)));
    }

    private void givenAutoBuildsEnabled() {
        JiraCloudPluginConfig.get().setAutoBuildsEnabled(true);
    }

    private void givenAutoBuildsRegex(String regex) {
        JiraCloudPluginConfig.get().setAutoBuildsRegex(regex);
    }

    private void givenCredentials() throws IOException {
        CredentialsProvider.lookupStores(jenkins.getInstance())
                .iterator()
                .next()
                .addCredentials(Domain.global(), secretCredential());
    }

    private void givenJiraApiAcceptsBuilds() {
        final JiraSenderFactory mockSenderFactory = mock(JiraSenderFactory.class);
        jiraBuildInfoSender = mock(JiraBuildInfoSender.class);
        when(mockSenderFactory.getJiraBuildInfoSender()).thenReturn(jiraBuildInfoSender);
        JiraSenderFactory.setInstance(mockSenderFactory);
        final BuildApiResponse response =
                new BuildApiResponse(
                        Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        final JiraBuildInfoResponse buildAccepted =
                JiraBuildInfoResponse.successBuildAccepted(SITE, response);
        when(jiraBuildInfoSender.sendBuildInfo(any()))
                .thenReturn(Collections.singletonList(buildAccepted));
    }

    @Test
    public void whenNoAutoBuildsRegex_thenSendsInProgressAndSuccessEvents() throws Exception {
        WorkflowJob workflow = givenWorkflowFromFile("auto-build.groovy");
        givenAutoBuildsEnabled();
        givenIssueKeys();

        jenkins.assertBuildStatusSuccess(workflow.scheduleBuild2(0));

        verifyBuildEvent(0, State.IN_PROGRESS);
        verifyBuildEvent(1, State.SUCCESSFUL);
    }

    @Test
    public void
            whenAutoBuildsRegexMatching_thenSendsInProgressAndSuccessEventsForFirstMatchingStep()
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
    public void whenAutoBuildsRegexNotMatching_thenSendsNoEvents() throws Exception {
        WorkflowJob workflow = givenWorkflowFromFile("auto-build-with-multiple-build-steps.groovy");
        givenAutoBuildsEnabled();
        givenAutoBuildsRegex("^foo.*$");
        givenIssueKeys();

        jenkins.assertBuildStatusSuccess(workflow.scheduleBuild2(0));

        verifyNoBuildEvents();
    }

    @Test
    public void whenBuildFailing_thenSendsInProgressAndFailedEvents() throws Exception {
        WorkflowJob workflow = givenWorkflowFromFile("auto-build-failing.groovy");
        givenAutoBuildsEnabled();
        givenIssueKeys();

        jenkins.assertBuildStatus(Result.FAILURE, workflow.scheduleBuild2(0));

        verifyBuildEvent(0, State.IN_PROGRESS);
        verifyBuildEvent(1, State.FAILED);
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
        verify(jiraBuildInfoSender, never()).sendBuildInfo(any());
    }

    private void verifyBuildEvent(int index, State state) {
        ArgumentCaptor<MultibranchBuildInfoRequest> requestCaptor =
                ArgumentCaptor.forClass(MultibranchBuildInfoRequest.class);
        verify(jiraBuildInfoSender, atLeastOnce()).sendBuildInfo(requestCaptor.capture());

        List<MultibranchBuildInfoRequest> requests = requestCaptor.getAllValues();
        if (index + 1 > requests.size()) {
            fail(
                    String.format(
                            "Expecting at least %d requests to the Jira, but got only %d",
                            index + 1, requests.size()));
        }
        assertThat(requests.get(index).getJiraState()).isEqualTo(state);
    }
}
