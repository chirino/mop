/**************************************************************************************
 * Copyright (C) 2009 Progress Software, Inc. All rights reserved.                    *
 * http://fusesource.com                                                              *
 * ---------------------------------------------------------------------------------- *
 * The software in this package is published under the terms of the AGPL license      *
 * a copy of which has been included with this distribution in the license.txt file.  *
 **************************************************************************************/
package org.fusesource.mop.support;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Helper methods for parsing all the commands.
 *
 * @version $Revision: 1.1 $
 */
public class CommandDefinitions {
    private static final transient Log LOG = LogFactory.getLog(CommandDefinitions.class);

    public static final String COMMANDS_URI = "META-INF/services/mop/commands.xml";
    public static final String COMMAND_PROPERTIES = "META-INF/services/mop.properties";

    /**
     * Loads all of the MRS commmands that can be found on the classpath in the given class loader
     * using the {@link #COMMANDS_URI} URI
     */
    public static Map<String, CommandDefinition> loadCommands(ClassLoader classLoader) {
        Map<String, CommandDefinition> answer = new TreeMap<String, CommandDefinition>();
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

    public static void loadCommands(Map<String, CommandDefinition> commands, URL url) {
        try {
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = documentBuilder.parse(url.openStream(), url.toString());
            Element element = document.getDocumentElement();
            NodeList list = element.getElementsByTagName("command");
            for (int i = 0, size = list.getLength(); i < size; i++) {
                Element commandElement = (Element) list.item(i);
                CommandDefinition command = loadCommand(commandElement);
                if (command != null) {
                    commands.put(command.getName(), command);
                }
            }
        } catch (Exception e) {
            LOG.warn("Failed to parse " + e, e);
        }
    }

    private static CommandDefinition loadCommand(Element element) {
        CommandDefinition answer = new CommandDefinition();
        answer.setName(element.getAttribute("name"));
        answer.setAlias(element.getAttribute("alias"));
        answer.setDescription(childElemenText(element, "description"));
        answer.setUsage(childElemenText(element, "usage"));
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


    /**
     * Looks on the classpath for all of the <code>META-INF/services/mop.properties</code> resource bundles and
     * loads their usage/descriptions in.
     */
    public static void addCommandDescriptions(Map<String, CommandDefinition> commands, ClassLoader classLoader) {
        Enumeration<URL> resources = null;
        try {
            resources = classLoader.getResources(COMMAND_PROPERTIES);
        } catch (IOException e) {
            LOG.debug("Could not load any MRS commands via " + COMMANDS_URI, e);
        }
        if (resources != null) {
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                addCommandDescriptions(commands, url);
            }
        }


    }

    private static void addCommandDescriptions(Map<String, CommandDefinition> commands, URL url) {
        Properties properties = new Properties();
        try {
            properties.load(url.openStream());
        } catch (IOException e) {
            LOG.warn("Failed to load MOP file: " + url);
        }
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String key = entry.getKey().toString();
            String value = entry.getValue().toString();

            String name = extractPostfix(key, ".usage");
            if (name != null) {
                CommandDefinition command = commands.get(name);
                if (command == null) {
                    missingCommand(name);
                } else {
                    command.setUsage(value);
                }
            } else {
                name = extractPostfix(key, ".description");
                if (name == null) {
                    LOG.warn("Missing command name for key " + key);
                }
                else {
                    CommandDefinition command = commands.get(name);
                    if (command == null) {
                        missingCommand(name);
                    } else {
                        command.setDescription(value);
                    }
                }
            }
        }
    }

    /**
     * Checks that a string ends with a given postfix, if it does then it is removed from the input string and returned
     */
    private static String extractPostfix(String key, String postfix) {
        if (key.endsWith(postfix)) {
            return key.substring(0, key.length() - postfix.length());
        }
        return null;
    }

    protected static void missingCommand(String name) {
        LOG.warn("No command loaded for name: " + name);
    }

}
