package com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model;

import com.atlassian.jira.cloud.jenkins.provider.ObjectMapperProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DeploymentApiResponseTest {

    private final ObjectMapper mapper = new ObjectMapperProvider().objectMapper();

    @Test
    public void deserializesAcceptedDeploymentWithUnknownIssueIdOrKeys()
            throws JsonProcessingException {
        String json =
                "{\"acceptedDeployments\":[{\"pipelineId\":\"pipeline\","
                        + "\"environmentId\":\"staging\",\"deploymentSequenceNumber\":42}],"
                        + "\"rejectedDeployments\":[],"
                        + "\"unknownAssociations\":[{\"type\":\"issueIdOrKeys\","
                        + "\"values\":[\"TEST-404\"]}]}";

        DeploymentApiResponse response = mapper.readValue(json, DeploymentApiResponse.class);

        assertThat(response.getAcceptedDeployments()).hasSize(1);
        assertThat(response.getAcceptedDeployments().get(0).getDeploymentSequenceNumber())
                .isEqualTo(42);
        assertThat(response.getRejectedDeployments()).isEmpty();
        assertThat(response.getUnknownAssociations()).hasSize(1);
        assertThat(response.getUnknownAssociations().get(0).getAssociationType().getValue())
                .isEqualTo("issueIdOrKeys");
        assertThat(response.getUnknownAssociations().get(0).getValues())
                .containsExactly("TEST-404");
    }

    @Test
    public void stillDeserializesExistingAssociationTypes() throws JsonProcessingException {
        assertThat(mapper.readValue("\"issueKeys\"", AssociationType.class))
                .isEqualTo(AssociationType.ISSUE_KEYS);
        assertThat(mapper.readValue("\"serviceIdOrKeys\"", AssociationType.class))
                .isEqualTo(AssociationType.SERVICE_ID_OR_KEYS);
    }

    @Test
    public void stillSerializesIssueKeysForDeploymentRequests() throws JsonProcessingException {
        assertThat(mapper.writeValueAsString(AssociationType.ISSUE_KEYS))
                .isEqualTo("\"issueKeys\"");
    }
}
