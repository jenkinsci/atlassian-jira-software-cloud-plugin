package com.atlassian.jira.cloud.jenkins.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class IpAddressProvider {

    public static String getIpAddress() {
        String ipAddress = "UNKNOWN";
        try {
            InetAddress localhost = InetAddress.getLocalHost();
            ipAddress = localhost.getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return ipAddress;
    }
}
