package com.atlassian.jira.cloud.jenkins.checkgatestatus.client.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class GateStatusResponse {
    private final GatingStatus gatingStatus;
    private final List<DetailKeyResponse> detailKeyResponse;

    @JsonCreator
    public GateStatusResponse(
            @JsonProperty("gatingStatus") final GatingStatus gatingStatus,
            @JsonProperty("details") final List<DetailKeyResponse> detailKeyResponse) {
        this.gatingStatus = gatingStatus;
        this.detailKeyResponse = detailKeyResponse;
    }

    public GatingStatus getStatus() {
        return gatingStatus;
    }

    public List<DetailKeyResponse> getDetailKeyResponse() {
        return detailKeyResponse;
    }
}
