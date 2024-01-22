package com.atlassian.jira.cloud.jenkins.util;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import hudson.XmlFile;
import org.w3c.dom.Element;

import java.io.IOException;

import static org.mockito.Mockito.when;

public class XmlUtilsTest {

    @Mock private XmlFile xmlFile;

    private XmlUtils xmlUtils;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        xmlUtils = new XmlUtils();
    }

    @Test
    public void testParseXmlFile() throws IOException {
        String xmlString = "<root><value>TestValue</value></root>";
        when(xmlFile.asString()).thenReturn(xmlString);

        Element rootElement = xmlUtils.parseXmlFile(xmlFile);

        Assert.assertEquals("root", rootElement.getNodeName());
        Assert.assertEquals("TestValue", xmlUtils.extractXmlValue(rootElement, "value"));
    }

    @Test
    public void testExtractXmlValue() throws IOException {
        String xmlString = "<root><value>TestValue</value></root>";
        when(xmlFile.asString()).thenReturn(xmlString);

        Element rootElement = xmlUtils.parseXmlFile(xmlFile);

        Assert.assertEquals("TestValue", xmlUtils.extractXmlValue(rootElement, "value"));
    }

    @Test
    public void testExtractBooleanValue() throws IOException {
        String xmlString = "<root><flag>true</flag></root>";
        when(xmlFile.asString()).thenReturn(xmlString);

        Element rootElement = xmlUtils.parseXmlFile(xmlFile);

        Assert.assertTrue(xmlUtils.extractBooleanValue(rootElement, "flag"));
    }

    @Test
    public void testSanitizeTagName() {
        String tagName = "tag-name!@#";
        String sanitizedTagName = xmlUtils.sanitizeTagName(tagName);

        Assert.assertEquals("tagname", sanitizedTagName);
    }
}
