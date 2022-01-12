package com.atlassian.jira.cloud.jenkins.checkgatingstatus.service;

import com.atlassian.jira.cloud.jenkins.Messages;
import com.atlassian.jira.cloud.jenkins.checkgatingstatus.client.model.DetailKeyResponse;
import com.atlassian.jira.cloud.jenkins.checkgatingstatus.client.model.GatingStatusResponse;
import com.atlassian.jira.cloud.jenkins.checkgatingstatus.client.model.GatingStatus;
import com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.stream.Collectors;

public class JiraGatingStatusResponse extends JiraSendInfoResponse {

    private final GatingStatus gatingStatus;

    public JiraGatingStatusResponse(
            final String jiraSite,
            final Status status,
            final String message,
            @Nullable final GatingStatus gatingStatus) {
        super(jiraSite, status, message);
        this.gatingStatus = gatingStatus;
    }

    public static JiraGatingStatusResponse of(final JiraSendInfoResponse response) {
        return new JiraGatingStatusResponse(
                response.getJiraSite(), response.getStatus(), response.getMessage(), null);
    }

    public static JiraGatingStatusResponse success(
            final String jiraSite, final GatingStatusResponse gatingStatusResponse) {
        final String relatedIssues =
                gatingStatusResponse
                        .getDetailKeyResponse()
                        .stream()
                        .map(DetailKeyResponse::getIssueLink)
                        .collect(Collectors.joining(", "));
        return new JiraGatingStatusResponse(
                jiraSite,
                Status.SUCCESS_GATE_CHECK,
                Messages.JiraGateStatusResponse_GATE_CHECK_SUCCESS(
                        relatedIssues, gatingStatusResponse.getStatus()),
                gatingStatusResponse.getStatus());
    }

    public static JiraGatingStatusResponse failure(final String jiraSite, final String message) {
        return new JiraGatingStatusResponse(jiraSite, Status.FAILURE_GATE_CHECK, message, null);
    }

    public Optional<GatingStatus> getGatingStatus() {
        return Optional.ofNullable(gatingStatus);
    }
}
