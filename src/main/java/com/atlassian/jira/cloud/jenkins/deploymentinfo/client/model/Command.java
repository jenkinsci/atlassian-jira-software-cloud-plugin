package com.atlassian.jira.cloud.jenkins.deploymentinfo.client.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/** This represents a command record in the request */
public class Command {
    private final String command;

    @JsonCreator
    public Command(@JsonProperty("command") final String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Command command1 = (Command) o;
        return command.equals(command1.command);
    }

    @Override
    public int hashCode() {
        return Objects.hash(command);
    }

    @Override
    public String toString() {
        return "Command{" + "command='" + command + '\'' + '}';
    }
}
