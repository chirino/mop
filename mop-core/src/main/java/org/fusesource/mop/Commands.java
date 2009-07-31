/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.mop;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.CharacterData;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;


/**
 * Helper methods for parsing all the commands.
 *
 * @version $Revision: 1.1 $
 */
public class Commands {
    private static final transient Log LOG = LogFactory.getLog(Commands.class);

    public static final String COMMANDS_URI = "META-INF/services/mop/commands.xml";

    /**
     * Loads all of the MRS commmands that can be found on the classpath in the given class loader
     * using the {@link #COMMANDS_URI} URI
     */
    public static Map<String, Command> loadCommands(ClassLoader classLoader) {
        Map<String, Command> answer = new TreeMap<String, Command>();
        Enumeration<URL> resources = null;
        try {
            resources = classLoader.getResources(COMMANDS_URI);
        } catch (IOException e) {
            LOG.debug("Could not load any MRS commands via " + COMMANDS_URI, e);
        }
        if (resources != null) {
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                loadCommands(answer, url);
            }
        }
        return answer;
    }

    public static void loadCommands(Map<String, Command> commands, URL url) {
        try {
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = documentBuilder.parse(url.openStream(), url.toString());
            Element element = document.getDocumentElement();
            NodeList list = element.getElementsByTagName("command");
            for (int i = 0, size = list.getLength(); i < size; i++) {
                Element commandElement = (Element) list.item(i);
                Command command = loadCommand(commandElement);
                if (command != null) {
                    commands.put(command.getName(), command);
                }
            }
        } catch (Exception e) {
            LOG.warn("Failed to parse " + e, e);
        }
    }

    private static Command loadCommand(Element element) {
        Command answer = new Command();
        answer.setName(element.getAttribute("name"));
        answer.setAlias(element.getAttribute("alias"));
        answer.setDescription(childElemenText(element, "description"));
        answer.setOptions(childElemenText(element, "options"));
        return answer;
    }

    protected static String childElemenText(Element element, String elementName) {
        NodeList descriptionList = element.getElementsByTagName(elementName);
        String text = toString(descriptionList);
        String value = text.trim();
        return value;
    }

    public static String toString(NodeList nodeList) {
        StringBuffer buffer = new StringBuffer();
        append(buffer, nodeList);
        return buffer.toString();
    }

    private static void append(StringBuffer buffer, NodeList nodeList) {
        int size = nodeList.getLength();
        for (int i = 0; i < size; i++) {
            append(buffer, nodeList.item(i));
        }
    }

    private static void append(StringBuffer buffer, Node node) {
        if (node instanceof Text) {
            Text text = (Text) node;
            buffer.append(text.getTextContent());
        } else if (node instanceof Attr) {
            Attr attribute = (Attr) node;
            buffer.append(attribute.getTextContent());
        } else if (node instanceof Element) {
            Element element = (Element) node;
            append(buffer, element.getChildNodes());
        } else if (node instanceof Document) {
            Document doc = (Document) node;
            append(buffer, doc.getChildNodes());
        }
    }
}
