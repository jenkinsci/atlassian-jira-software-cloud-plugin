package com.atlassian.jira.cloud.jenkins.util;

import hudson.XmlFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;

/**
 * Utility class for handling XML-related operations such as parsing XML files and extracting values
 * from XML elements.
 *
 * <p>This class provides methods to parse XML files, extract specific values based on tag names,
 * and handle boolean values stored within XML elements.
 */
public class XmlUtils {

    private final DocumentBuilder builder;

    public XmlUtils() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            this.builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException("Error initializing XmlUtils", e);
        }
    }

    public Element parseXmlFile(final XmlFile file) {
        try {
            Document document = builder.parse(new InputSource(new StringReader(file.asString())));
            return document.getDocumentElement();
        } catch (SAXException | IOException e) {
            throw new RuntimeException("Error parsing XML file", e);
        }
    }

    public String extractXmlValue(final Element parentElement, final String tagName) {
        String sanitizedTagName = sanitizeTagName(tagName);
        NodeList nodeList = parentElement.getElementsByTagName(sanitizedTagName);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return "";
    }

    public boolean extractBooleanValue(final Element parentElement, final String tagName) {
        NodeList nodeList = parentElement.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            String value = nodeList.item(0).getTextContent();
            return Boolean.parseBoolean(value);
        }
        return false;
    }

    String sanitizeTagName(final String tagName) {
        return tagName.replaceAll("[^a-zA-Z0-9_]", "");
    }
}
