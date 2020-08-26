package com.atlassian.jira.cloud.jenkins.checkgatestatus.service;

import com.atlassian.jira.cloud.jenkins.Messages;
import com.atlassian.jira.cloud.jenkins.checkgatestatus.client.model.DetailKeyResponse;
import com.atlassian.jira.cloud.jenkins.checkgatestatus.client.model.GateStatusResponse;
import com.atlassian.jira.cloud.jenkins.checkgatestatus.client.model.GatingStatus;
import com.atlassian.jira.cloud.jenkins.common.response.JiraSendInfoResponse;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class JiraGateStatusResponse extends JiraSendInfoResponse {

    private final GatingStatus gatingStatus;

    public JiraGateStatusResponse(
            final Status status, final String message, @Nullable final GatingStatus gatingStatus) {
        super(status, message);
        this.gatingStatus = gatingStatus;
    }

    public static JiraGateStatusResponse of(final JiraSendInfoResponse response) {
        return new JiraGateStatusResponse(response.getStatus(), response.getMessage(), null);
    }

    public static JiraGateStatusResponse success(final GateStatusResponse gateStatusResponse) {
        final String relatedIssues =
                gateStatusResponse
                        .getDetailKeyResponse()
                        .stream()
                        .map(DetailKeyResponse::getIssueLink)
                        .collect(Collectors.joining(", "));
        return new JiraGateStatusResponse(
                Status.SUCCESS_GATE_CHECK,
                Messages.JiraGateStatusResponse_GATE_CHECK_SUCCESS(
                        relatedIssues, gateStatusResponse.getStatus()),
                gateStatusResponse.getStatus());
    }

    public static JiraGateStatusResponse failure(final String message) {
        return new JiraGateStatusResponse(Status.FAILURE_GATE_CHECK, message, null);
    }

    public Optional<GatingStatus> getGatingStatus() {
        return Optional.ofNullable(gatingStatus);
    }
}
