package com.atlassian.jira.cloud.jenkins.common.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class ClassPathReader {

    /**
     * Reads a file from the class path into a String.
     */
    public static String readFromClasspath(String path) {
        return new BufferedReader(
                new InputStreamReader(ClassPathReader.class.getResourceAsStream(path)))
                .lines()
                .collect(Collectors.joining("\n"));
    }

}
