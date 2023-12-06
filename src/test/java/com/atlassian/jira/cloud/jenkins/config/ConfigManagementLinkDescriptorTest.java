package com.atlassian.jira.cloud.jenkins.config;

import com.atlassian.jira.cloud.jenkins.ping.PingApi;
import com.atlassian.jira.cloud.jenkins.tenantinfo.CloudIdResolver;
import com.atlassian.jira.cloud.jenkins.util.SecretRetriever;
import hudson.util.FormValidation;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConfigManagementLinkDescriptorTest {

    private static final String SITE = "example.atlassian.net";
    private static final String CLOUD_ID = UUID.randomUUID().toString();
    private static final String WEBHOOK_URL = "https://webhook.url?jenkins_server_uuid=foo";
    private static final String CREDENTIALS_ID = "credsId";
    private static final String AUTO_BUILDS_REGEX = "^build$";
    private static final String AUTO_DEPLOYMENTS_REGEX = "^deploy to (?<envName>.*)$";

    @Rule public JenkinsRule jRule = new JenkinsRule();

    private CloudIdResolver cloudIdResolver;
    private SecretRetriever secretRetriever;

    private ConfigManagementLink.DescriptorImpl classUnderTest;

    private PingApi pingApi;

    @Before
    public void setUp() {
        cloudIdResolver = mock(CloudIdResolver.class);
        secretRetriever = mock(SecretRetriever.class);
        pingApi = mock(PingApi.class);
        final ConfigManagementLink configManagementLink = new ConfigManagementLink();
        classUnderTest = (ConfigManagementLink.DescriptorImpl) configManagementLink.getDescriptor();
        classUnderTest.setCloudIdResolver(cloudIdResolver);
        classUnderTest.setSecretRetriever(secretRetriever);
        classUnderTest.setPingApi(pingApi);

        setupMocks();
    }

    @Test
    public void testConnection_whenSiteResolved() throws Exception {
        // given
        when(secretRetriever.getSecretFor(any())).thenReturn(Optional.of("secret"));

        // when
        final FormValidation result =
                classUnderTest.doTestConnection(SITE, WEBHOOK_URL, CREDENTIALS_ID);

        // then
        assertThat(result.kind).isEqualTo(FormValidation.Kind.OK);
    }

    @Test
    public void testConnection_whenSiteNotResolved() throws Exception {
        // given
        when(secretRetriever.getSecretFor(any())).thenReturn(Optional.of("secret"));
        when(cloudIdResolver.getCloudId(any())).thenReturn(Optional.empty());
        final String site = "foobar.atlassian.net";

        // when
        final FormValidation result =
                classUnderTest.doTestConnection(site, WEBHOOK_URL, CREDENTIALS_ID);

        // then
        assertThat(result.kind).isEqualTo(FormValidation.Kind.ERROR);
        assertThat(result.getMessage()).isEqualTo("Failed to resolve Jira Cloud site: " + site);
    }

    @Test
    public void testSuccessfullyTestConnection() throws Exception {
        // given
        when(secretRetriever.getSecretFor(any())).thenReturn(Optional.of("secret"));

        // when
        final FormValidation result =
                classUnderTest.doTestConnection(SITE, WEBHOOK_URL, CREDENTIALS_ID);

        // then
        assertThat(result.kind).isEqualTo(FormValidation.Kind.OK);
    }

    @Test
    public void testFailTestConnection_whenCredentialsNotFound() {
        // given
        when(secretRetriever.getSecretFor(any())).thenReturn(Optional.empty());
        // when
        final FormValidation result =
                classUnderTest.doTestConnection(SITE, WEBHOOK_URL, CREDENTIALS_ID);

        // then
        assertThat(result.kind).isEqualTo(FormValidation.Kind.ERROR);
        assertThat(result.getMessage()).isEqualTo("Failed to retrieve secret");
    }

    @Test
    public void testFailTestConnection_whenPingFailed() {
        given(pingApi.sendPing(any(), any(), any())).willReturn(false);

        // when
        final FormValidation result =
                classUnderTest.doTestConnection(SITE, WEBHOOK_URL, CREDENTIALS_ID);

        // then
        assertThat(result.kind).isEqualTo(FormValidation.Kind.ERROR);
        assertThat(result.getMessage()).isEqualTo("Failed to retrieve secret");
    }

    @Test
    public void testSiteValidation_whenValidSiteName() {
        // when
        final FormValidation result = classUnderTest.doCheckSite(SITE);

        // then
        assertThat(result.kind).isEqualTo(FormValidation.Kind.OK);
    }

    @Test
    public void testSiteValidation_whenInvalidSiteName() {
        // when
        final FormValidation result = classUnderTest.doCheckSite("hdfsjqkwjqkwj");

        // then
        assertThat(result.kind).isEqualTo(FormValidation.Kind.ERROR);
        assertThat(result.getMessage())
                .isEqualTo(
                        "Site name is invalid. Paste a valid site name, e.g. sitename.atlassian.net.");
    }

    @Test
    public void testWebhookValidation_whenValidWebhookUrl() {
        // when
        final FormValidation result = classUnderTest.doCheckWebhookUrl(WEBHOOK_URL);

        // then
        assertThat(result.kind).isEqualTo(FormValidation.Kind.OK);
    }

    @Test
    public void testSiteValidation_whenEmptyWebhookUrl() {
        // when
        final FormValidation result = classUnderTest.doCheckWebhookUrl("");

        // then
        assertThat(result.kind).isEqualTo(FormValidation.Kind.ERROR);
        assertThat(result.getMessage()).contains("Webhook URL can’t be blank");
    }

    @Test
    public void testSiteValidation_whenWebhookUrlWithoutQueryParam() {
        // when
        final FormValidation result = classUnderTest.doCheckWebhookUrl("https://webhook.url");

        // then
        assertThat(result.kind).isEqualTo(FormValidation.Kind.ERROR);
        assertThat(result.getMessage()).contains("Webhook URL needs to contain query parameter");
    }

    @Test
    public void testSiteValidation_whenWebhookUrlWithWrongQueryParam() {
        // when
        final FormValidation result =
                classUnderTest.doCheckWebhookUrl("https://webhook.url?jenkins_uuid=foo");

        // then
        assertThat(result.kind).isEqualTo(FormValidation.Kind.ERROR);
        assertThat(result.getMessage()).contains("Webhook URL needs to contain query parameter");
    }

    @Test
    public void testSiteValidation_whenInvalidWebhookUrl() {
        // when
        final FormValidation result = classUnderTest.doCheckWebhookUrl("abc");

        // then
        assertThat(result.kind).isEqualTo(FormValidation.Kind.ERROR);
        assertThat(result.getMessage()).contains("Webhook URL is not a valid URL");
    }

    public void testWebhookValidation_whenValidAutoBuildsRegex() {
        // when
        final FormValidation result = classUnderTest.doCheckAutoBuildsRegex(AUTO_BUILDS_REGEX);

        // then
        assertThat(result.kind).isEqualTo(FormValidation.Kind.OK);
    }

    @Test
    public void testWebhookValidation_whenEmptyAutoBuildsRegex() {
        // when
        final FormValidation result = classUnderTest.doCheckAutoBuildsRegex("");

        // then
        assertThat(result.kind).isEqualTo(FormValidation.Kind.OK);
    }

    @Test
    public void testWebhookValidation_whenInvalidAutoBuildsRegex() {
        // when
        final FormValidation result = classUnderTest.doCheckAutoBuildsRegex("?trash-regex?");

        // then
        assertThat(result.kind).isEqualTo(FormValidation.Kind.ERROR);
        assertThat(result.getMessage()).contains("Invalid regular expression:");
    }

    public void testWebhookValidation_whenValidAutoDeploymentsRegex() {
        // when
        final FormValidation result = classUnderTest.doCheckAutoBuildsRegex(AUTO_DEPLOYMENTS_REGEX);

        // then
        assertThat(result.kind).isEqualTo(FormValidation.Kind.OK);
    }

    @Test
    public void testWebhookValidation_whenEmptyAutoDeploymentsRegex() {
        // when
        final FormValidation result = classUnderTest.doCheckAutoDeploymentsRegex("");

        // then
        assertThat(result.kind).isEqualTo(FormValidation.Kind.ERROR);
        assertThat(result.getMessage()).contains("Deployment regex cannot be empty.");
    }

    @Test
    public void testWebhookValidation_whenInvalidAutoDeploymentsRegex() {
        // when
        final FormValidation result = classUnderTest.doCheckAutoBuildsRegex("?trash-regex?");

        // then
        assertThat(result.kind).isEqualTo(FormValidation.Kind.ERROR);
        assertThat(result.getMessage()).contains("Invalid regular expression:");
    }
    private void setupMocks() {
        setupCloudIdResolver();
        setupPingApi();
    }

    private void setupPingApi() {
        when(pingApi.sendPing(any(), any(), any())).thenReturn(true);
    }

    private void setupCloudIdResolver() {
        when(cloudIdResolver.getCloudId(any())).thenReturn(Optional.of(CLOUD_ID));
    }
}
