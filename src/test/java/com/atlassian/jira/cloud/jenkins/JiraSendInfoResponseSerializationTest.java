package com.atlassian.jira.cloud.jenkins;

import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.BuildApiResponse;
import com.atlassian.jira.cloud.jenkins.buildinfo.client.model.BuildKeyResponse;
import com.atlassian.jira.cloud.jenkins.buildinfo.service.JiraBuildInfoResponse;
import com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.DeploymentApiResponse;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model.DeploymentKeyResponse;
import com.atlassian.jira.cloud.jenkins.deploymentinfo.service.JiraDeploymentInfoResponse;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class JiraSendInfoResponseSerializationTest {

    private static final String JIRA_SITE = "example.atlassian.net";
    private static final String PIPELINE_ID = UUID.randomUUID().toString();
    private static final String ENVIRONMENT_ID = UUID.randomUUID().toString();

    @Test
    public void testSerializationJiraDeploymentInfoResponse() throws Exception {
        // given
        final JiraSendInfoResponse originalResponse = createJiraDeploymentInfoResponse();

        // when
        final byte[] serializedObject = serializeObject(originalResponse);
        final JiraDeploymentInfoResponse deserializedResponse =
                (JiraDeploymentInfoResponse) deserializeObject(serializedObject);

        // then
        assertThat(deserializedResponse).isEqualTo(originalResponse);
    }

    @Test
    public void shouldSerializeJiraBuildInfoResponse() throws Exception {
        // given
        final JiraSendInfoResponse originalResponse = createJiraBuildInfoResponse();

        // when
        final byte[] serializedObject = serializeObject(originalResponse);
        final JiraBuildInfoResponse deserializedResponse =
                (JiraBuildInfoResponse) deserializeObject(serializedObject);

        // then
        assertThat(deserializedResponse).isEqualTo(originalResponse);
    }

    private JiraSendInfoResponse createJiraDeploymentInfoResponse() {
        final DeploymentKeyResponse deploymentKeyResponse =
                new DeploymentKeyResponse(PIPELINE_ID, ENVIRONMENT_ID, 1);
        final DeploymentApiResponse response =
                new DeploymentApiResponse(
                        Collections.singletonList(deploymentKeyResponse),
                        Collections.emptyList(),
                        Collections.emptyList());

        return JiraDeploymentInfoResponse.successDeploymentAccepted(JIRA_SITE, response);
    }

    private JiraSendInfoResponse createJiraBuildInfoResponse() {
        final BuildKeyResponse buildKeyResponse = new BuildKeyResponse(PIPELINE_ID, 1);
        final BuildApiResponse response =
                new BuildApiResponse(
                        Collections.singletonList(buildKeyResponse),
                        Collections.emptyList(),
                        Collections.emptyList());

        return JiraBuildInfoResponse.successBuildAccepted(JIRA_SITE, response);
    }

    private byte[] serializeObject(final Object object) throws IOException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        objectOutputStream.writeObject(object);
        objectOutputStream.flush();
        objectOutputStream.close();

        return outputStream.toByteArray();
    }

    private Object deserializeObject(final byte[] bytes)
            throws IOException, ClassNotFoundException {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        final ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);

        return objectInputStream.readObject();
    }
}
