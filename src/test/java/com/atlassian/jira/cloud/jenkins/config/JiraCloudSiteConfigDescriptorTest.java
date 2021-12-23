package com.atlassian.jira.cloud.jenkins.config;

import com.atlassian.jira.cloud.jenkins.tenantinfo.CloudIdResolver;
import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.domains.Domain;
import hudson.util.FormValidation;
import hudson.util.Secret;
import com.atlassian.jira.cloud.jenkins.auth.AccessTokenRetriever;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JiraCloudSiteConfigDescriptorTest {

    private static final String SITE = "example.atlassian.net";
    private static final String CLOUD_ID = UUID.randomUUID().toString();
    private static final String CLIENT_ID = "clientId";
    private static final String CREDENTIALS_ID = "credsId";

    @Rule public JenkinsRule jRule = new JenkinsRule();

    private AccessTokenRetriever accessTokenRetriever;

    private CloudIdResolver cloudIdResolver;

    private JiraCloudSiteConfig.DescriptorImpl classUnderTest;

    @Before
    public void setUp() {
        accessTokenRetriever = mock(AccessTokenRetriever.class);
        cloudIdResolver = mock(CloudIdResolver.class);
        final JiraCloudSiteConfig siteConfig =
                new JiraCloudSiteConfig(SITE, CLIENT_ID, CREDENTIALS_ID);
        classUnderTest = (JiraCloudSiteConfig.DescriptorImpl) siteConfig.getDescriptor();
        classUnderTest.setAccessTokenRetriever(accessTokenRetriever);
        classUnderTest.setCloudIdResolver(cloudIdResolver);

        setupMocks();
    }

    @Test
    public void testConnection_whenSiteResolved() throws Exception {
        // given
        setupCredentials(CREDENTIALS_ID, "secret");

        // when
        final FormValidation result =
                classUnderTest.doTestConnection(SITE, CLIENT_ID, CREDENTIALS_ID);

        // then
        assertThat(result.kind).isEqualTo(FormValidation.Kind.OK);
    }

    @Test
    public void testConnection_whenSiteNotResolved() throws Exception {
        // given
        setupCredentials(CREDENTIALS_ID, "secret");
        when(cloudIdResolver.getCloudId(any())).thenReturn(Optional.empty());
        final String site = "foobar.atlassian.net";

        // when
        final FormValidation result =
                classUnderTest.doTestConnection(site, CLIENT_ID, CREDENTIALS_ID);

        // then
        assertThat(result.kind).isEqualTo(FormValidation.Kind.ERROR);
        assertThat(result.getMessage()).isEqualTo("Failed to resolve Jira Cloud site: " + site);
    }

    @Test
    public void testSuccessfullyTestConnection() throws Exception {
        // given
        setupCredentials(CREDENTIALS_ID, "secret");

        // when
        final FormValidation result =
                classUnderTest.doTestConnection(SITE, CLIENT_ID, CREDENTIALS_ID);

        // then
        assertThat(result.kind).isEqualTo(FormValidation.Kind.OK);
    }

    @Test
    public void testFailTestConnection_whenCredentialsNotFound() {
        // when
        final FormValidation result =
                classUnderTest.doTestConnection(SITE, CLIENT_ID, CREDENTIALS_ID);

        // then
        assertThat(result.kind).isEqualTo(FormValidation.Kind.ERROR);
        assertThat(result.getMessage()).isEqualTo("Failed to retrieve secret");
    }

    @Test
    public void testFailTestConnection_whenTokenRetrievalFails() throws Exception {
        // given
        setupCredentials(CREDENTIALS_ID, "secret");
        setupCloudIdResolver();
        when(accessTokenRetriever.getAccessToken(any(), any())).thenReturn(Optional.empty());

        // when
        final FormValidation result =
                classUnderTest.doTestConnection(SITE, CLIENT_ID, CREDENTIALS_ID);
        assertThat(result.kind).isEqualTo(FormValidation.Kind.ERROR);
        assertThat(result.getMessage()).isEqualTo("Failed to validate site credentials");
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

        // than
        assertThat(result.kind).isEqualTo(FormValidation.Kind.ERROR);
        assertThat(result.getMessage())
                .isEqualTo(
                        "Site name is invalid. Paste a valid site name, e.g. sitename.atlassian.net.");
    }

    private void setupCredentials(String credentialId, String secret) throws Exception {
        final CredentialsStore credentialsStore =
                CredentialsProvider.lookupStores(jRule.jenkins).iterator().next();
        final Domain domain = Domain.global();
        final Credentials credentials =
                new StringCredentialsImpl(
                        CredentialsScope.GLOBAL, credentialId, "", Secret.fromString(secret));
        credentialsStore.addCredentials(domain, credentials);
    }

    private void setupMocks() {
        setupAccessTokenRetriever();
        setupCloudIdResolver();
    }

    private void setupAccessTokenRetriever() {
        when(accessTokenRetriever.getAccessToken(any(), any()))
                .thenReturn(Optional.of("access_token"));
    }

    private void setupCloudIdResolver() {
        when(cloudIdResolver.getCloudId(any())).thenReturn(Optional.of(CLOUD_ID));
    }
}
