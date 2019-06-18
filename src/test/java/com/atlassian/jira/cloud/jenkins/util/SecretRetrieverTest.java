package com.atlassian.jira.cloud.jenkins.util;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.domains.Domain;
import hudson.util.Secret;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class SecretRetrieverTest {

    @Rule public JenkinsRule jRule = new JenkinsRule();

    private SecretRetriever classUnderTest = new SecretRetriever();

    @Test
    public void testRetrieveSecret_whenCredentialsExist() throws Exception {
        // given
        final String credentialId = "credsId";
        final String secretText = "secret";
        setupCredentials(credentialId, secretText);

        // when
        final Optional<String> secret = classUnderTest.getSecretFor(credentialId);

        // then
        assertThat(secret.isPresent()).isTrue();
        assertThat(secret.get()).isEqualTo(secretText);
    }

    @Test
    public void testReturnEmpty_whenCredentialsDoNotExist() {
        // when
        final Optional<String> secret = classUnderTest.getSecretFor("nonExistingCredentialsId");

        // then
        assertThat(secret.isPresent()).isFalse();
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
}
