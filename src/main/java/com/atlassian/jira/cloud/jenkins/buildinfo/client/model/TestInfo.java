package com.atlassian.jira.cloud.jenkins.buildinfo.client.model;

/**
 * Represents the test info.
 * This is included as part of a build.
 */
public class TestInfo {

    private Integer totalNumber;
    private Integer numberPassed;
    private Integer numberFailed;
    private Integer numberSkipped;

    public Integer getTotalNumber() {
        return totalNumber;
    }

    public TestInfo setTotalNumber(final Integer totalNumber) {
        this.totalNumber = totalNumber;
        return this;
    }

    public Integer getNumberPassed() {
        return numberPassed;
    }

    public TestInfo setNumberPassed(final Integer numberPassed) {
        this.numberPassed = numberPassed;
        return this;
    }

    public Integer getNumberFailed() {
        return numberFailed;
    }

    public TestInfo setNumberFailed(final Integer numberFailed) {
        this.numberFailed = numberFailed;
        return this;
    }

    public Integer getNumberSkipped() {
        return numberSkipped;
    }

    public TestInfo setNumberSkipped(final Integer numberSkipped) {
        this.numberSkipped = numberSkipped;
        return this;
    }
}
