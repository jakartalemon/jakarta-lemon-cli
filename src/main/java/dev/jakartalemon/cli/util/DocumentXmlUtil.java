package dev.jakartalemon.cli.util;

/*
 * Copyright 2022 Apuntes de Java.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;

import static java.util.Collections.emptyMap;
import static javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING;
import static org.apache.commons.lang3.BooleanUtils.NO;
import static org.apache.commons.lang3.BooleanUtils.YES;

/**
 * Utility class for manipulating XML documents
 *
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
public class DocumentXmlUtil {

    private static final Logger LOGGER = Logger.getLogger(DocumentXmlUtil.class.getName());
    private static final String STRIP_XSL_FILE_NAME = "/xml/strip.xsl";

    private DocumentXmlUtil() {

    }

    /**
     * Creates a new XML object, with a root element given as a parameter.
     *
     * @param rootElementName Root element name
     * @return XML document created
     * @throws ParserConfigurationException ParserConfigurationException
     */
    public static Document newDocument(String rootElementName) throws ParserConfigurationException {
        var document = newDocument();
        var rootElement = document.createElement(rootElementName);
        document.appendChild(rootElement);
        return document;

    }

    public static Document newDocument() throws ParserConfigurationException {
        var documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setFeature(FEATURE_SECURE_PROCESSING, true);
        var documentBuilder = documentBuilderFactory.newDocumentBuilder();
        return documentBuilder.newDocument();
    }

    /**
     * Opens an XML document given by parameter, or {@link Optional#empty()} empty if it failed to
     * open the document.
     *
     * @param path XML document path
     * @return XML object, or empty if it failed to open.
     */
    public static Optional<Document> openDocument(Path path) {

        var documentBuilderFactory = DocumentBuilderFactory.newInstance();
        try {
            documentBuilderFactory.setFeature(FEATURE_SECURE_PROCESSING, true);
            var documentBuilder = documentBuilderFactory.newDocumentBuilder();
            var document = documentBuilder.parse(path.toFile());
            document.getDocumentElement().normalize();
            return Optional.of(document);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            LOGGER.severe(e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Gets one of the elements of a document, based on an XPath search given by parameter.
     *
     * @param document   xml document
     * @param expression XPath search expression
     * @return List of DOM elements found, based on the search criteria
     * @throws XPathExpressionException XPathExpressionException
     */
    public static List<Element> listElementsByFilter(Document document, String expression) throws
                                                                                           XPathExpressionException {
        var xPath = XPathFactory.newInstance().newXPath();
        var nodeList =
            (NodeList) xPath.compile(expression).evaluate(document, XPathConstants.NODESET);
        List<Element> elementList = new ArrayList<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            elementList.add((Element) nodeList.item(i));
        }
        return elementList;
    }

    /**
     * Creates an element within a specified path in the XML document. Returns the created element.
     *
     * @param document    xml document
     * @param inPath      Path where the element will be created
     * @param elementName Name of the element to be created
     * @return Element created, or {@link Optional#empty()} if not created, for example if the path
     * was not found.
     * @throws XPathExpressionException * @throws XPathExpressionException
     */
    public static Optional<Element> createElement(Document document, String inPath,
        String elementName) throws
                            XPathExpressionException {
        var elements = listElementsByFilter(document, inPath);
        if (!elements.isEmpty()) {
            Element element = document.createElement(elementName);
            elements.stream().findFirst().ifPresent(elem -> elem.appendChild(element));
            return Optional.of(element);
        }
        return Optional.empty();
    }

    /**
     * Creates an element as a child of an element given by parameter. In addition, the content text
     * that the element
     * will have is established.
     *
     * @param document      xml document
     * @param parentElement parent element
     * @param elementName   Name of the element to be created
     * @param textContent   Text content
     * @return Element created
     */
    public static Optional<Element> createElement(Document document, Element parentElement,
        String elementName,
        String textContent) {
        var element = document.createElement(elementName);
        if (StringUtils.isNotBlank(textContent)) {
            element.setTextContent(textContent);
        }
        parentElement.appendChild(element);
        return Optional.of(element);
    }

    /**
     * Creates an element as a child of an element given by parameter.
     *
     * @param document      xml document
     * @param parentElement parent element
     * @param elementName   Name of the element to be created
     * @return Element created
     */
    public static Optional<Element> createElement(Document document, Element parentElement,
        String elementName) {
        return createElement(document, parentElement, elementName, null);
    }

    /**
     * Saves an XML document object at the specified path
     *
     * @param path     Path where the xml document will be saved
     * @param document XML document to save
     */
    public static void saveDocument(Path path, Document document) {
        saveDocument(path, document, emptyMap());
    }

    /**
     * Saves an XML document object at the specified path. Additionally, properties will be
     * specified in the file save
     * transformation.
     *
     * @param path             Path where the xml document will be saved
     * @param document         XML document to save
     * @param outputProperties transformation properties. See {@link OutputKeys}
     */
    public static void saveDocument(Path path, Document document,
        Map<String, String> outputProperties) {
        try (var fos = new FileOutputStream(
            path.toFile()); var xlsIs = DocumentXmlUtil.class.getResourceAsStream(
            STRIP_XSL_FILE_NAME)) {
            Source xslt = new StreamSource(xlsIs);
            var transformerFactory = TransformerFactory.newInstance();
            var transformer = transformerFactory.newTransformer(xslt);
            transformer.setOutputProperty(OutputKeys.INDENT, YES);
            transformer.setOutputProperty(OutputKeys.STANDALONE, NO);
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, NO);
            outputProperties.forEach(transformer::setOutputProperty);
            document.setXmlStandalone(true);
            var source = new DOMSource(document);
            var result = new StreamResult(fos);
            transformer.transform(source, result);
        } catch (IOException | TransformerException e) {
            LOGGER.severe(e.getMessage());
        }
    }

    /**
     * Constructor class that helps create elements with attributes and children
     */
    public static class ElementBuilder {

        /**
         * Creates a new element instance, with a specific name.
         *
         * @param tagName Element Tag name
         * @return Element Builder itself
         */
        public static ElementBuilder newInstance(String tagName) {
            return new ElementBuilder(tagName);
        }

        private final String tagName;
        private final Set<String[]> attributes;
        private final Set<ElementBuilder> children;
        private String textContent;

        private ElementBuilder(String tagName) {
            this.tagName = tagName;
            attributes = new LinkedHashSet<>();
            children = new LinkedHashSet<>();
        }

        /**
         * Add an attribute to the attribute's constructor, in addition to the attribute's value
         *
         * @param name  attribute name
         * @param value attribute value
         * @return Element Builder itself
         */
        public ElementBuilder addAttribute(String name, String value) {
            attributes.add(new String[]{name, value});
            return this;
        }

        /**
         * Add a child element.
         *
         * @param elementBuilder element builder child
         * @return Element Builder itself
         */
        public ElementBuilder addChild(ElementBuilder elementBuilder) {
            children.add(elementBuilder);
            return this;
        }

        /**
         * Constructs a DOM element based on all the values set in the constructor.
         *
         * @param document xml document
         * @return Element created
         */
        public Element build(Document document) {
            var tagNameSplit = tagName.split(":");
            Element element = tagNameSplit.length == 1
                ? document.createElement(tagName)
                : document.createElementNS(NAMESPACES.get(tagNameSplit[0]), tagName);
            attributes.forEach(attr -> element.setAttribute(attr[0], attr[1]));
            children.forEach(child -> element.appendChild(child.build(document)));
            Optional.ofNullable(textContent).ifPresent(element::setTextContent);

            return element;
        }

        public ElementBuilder setTextContent(String textContent) {
            this.textContent = textContent;
            return this;
        }

    }

    private static final Map<String, String> NAMESPACES = Map.of(
        "f", "http://xmlns.jcp.org/jsf/core",
        "h", "http://xmlns.jcp.org/jsf/html",
        "p", "http://primefaces.org/ui",
        "ui", "http://xmlns.jcp.org/jsf/facelets"
    );
}
