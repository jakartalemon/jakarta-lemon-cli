/*
 * Copyright 2023 Diego Silva mailto:diego.silva@apuntesdejava.com.
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
package dev.jakartalemon.cli.project.hexa.handler;

import dev.jakartalemon.cli.util.DependenciesUtil;
import dev.jakartalemon.cli.util.DocumentXmlUtil;
import dev.jakartalemon.cli.util.FileClassUtil;
import dev.jakartalemon.cli.util.HttpClientUtil;
import dev.jakartalemon.cli.util.PomUtil;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import dev.jakartalemon.cli.util.JavaFileBuilder;

import static dev.jakartalemon.cli.util.Constants.*;
import static dev.jakartalemon.cli.util.LinesUtils.removeLastComma;
import static javax.xml.xpath.XPathConstants.NODESET;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;

/**
 *
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
@Slf4j
public class JpaPersistenceHandler {

    private static final String IMPORT_COLUMN = "import jakarta.persistence.%s;";
    private static final String IMPORT_MANY_TO_ONE = "import jakarta.persistence.ManyToOne;";
    private static final String IMPORT_ONE_TO_ONE = "import jakarta.persistence.OneToOne;";
    private static final String PERSISTENCE_FILE_NAME = "persistence.xml";
    private static final String PERSISTENCE = "persistence";
    private static final String[][] ASSOCIATIONS_DESCRIPTIONS = {
        {MANY_TO_ONE, IMPORT_MANY_TO_ONE},
        {ONE_TO_ONE, IMPORT_ONE_TO_ONE}
    };

    public static JpaPersistenceHandler getInstance() {
        return JpaPersistenceHandlerHolder.INSTANCE;
    }

    public static void createRepositoryImplementation(String modelName,
                                                      String packageName,
                                                      String infrastructurePath) {
        var importClass = "%s.%s.%s.%sRepository".formatted(packageName, DOMAIN, REPOSITORY, modelName);
        try {
            var javaFileBuilder = new JavaFileBuilder()
                .setClassName(modelName + "RepositoryImpl")
                .setPackage(packageName, INFRASTRUCTURE, REPOSITORY)
                .setModulePath(infrastructurePath)
                .addImportClass(importClass)
                .addImportClass("jakarta.inject.Inject")
                .addImportClass("jakarta.persistence.EntityManager")
                .addVariableDeclaration("EntityManager","em","Inject")
                .addImplementationInterface(modelName + "Repository");
            javaFileBuilder.build();
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private JsonObject columnAnnotationProperties;
    private JsonObject joinColumnAnnotationProperties;
    private Document persistenceXml;
    private Path persistenceXmlPath;

    private JpaPersistenceHandler() {
        try {
            addJpaDependency();
            readAnnotationsDefinitions();
            createPersistenceXml();
        } catch (InterruptedException | IOException | URISyntaxException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private void addJpaDependency() throws InterruptedException, IOException, URISyntaxException {
        DependenciesUtil.getLastVersionDependency(
            MAVEN_QUERY_PERSISTENCE_API).ifPresent(dependency -> PomUtil.getInstance()
            .addDependency(dependency, INFRASTRUCTURE));
    }

    private boolean checkAssociation(List<String> lines,
                                     JsonObject definitionValue) {
        for (var descriptionsRow : ASSOCIATIONS_DESCRIPTIONS) {
            var annotation = descriptionsRow[0];
            if (definitionValue.containsKey(annotation)) {
                var importDeclaration = descriptionsRow[1];
                if (!lines.contains(importDeclaration)) {
                    lines.add(2, importDeclaration);
                }
                lines.add("%s@%s".formatted(StringUtils.repeat(SPACE, TAB_SIZE), StringUtils.
                    capitalize(annotation)));
                return true;
            }
        }
        return false;

    }

    public void createEntityClass(JsonObject projectInfo,
                                  String className,
                                  JsonObject jsonObject) {
        try {
            log.debug("classDefinition:{}", jsonObject);
            List<String> lines = new ArrayList<>();
            var packageName = PACKAGE_TEMPLATE.formatted(projectInfo.getString(PACKAGE), INFRASTRUCTURE,
                ADAPTERS) + "." + ENTITIES;
            lines.add(TEMPLATE_2_STRING_COMMA.formatted(PACKAGE, packageName));
            lines.add(EMPTY);
            lines.add("import jakarta.persistence.Entity;");
            lines.add("import jakarta.persistence.Id;");
            lines.add("import lombok.Getter;");
            lines.add("import lombok.Setter;");
            lines.add(EMPTY);
            lines.add("@Entity");
            lines.add("@Getter");
            lines.add("@Setter");
            lines.add("public class %s {".formatted(className));
            jsonObject.getJsonObject(FIELDS).forEach((fieldName, definition) -> {
                var definitionValue = definition.asJsonObject();
                var type = definitionValue.getString(TYPE, STRING_TYPE);
                if (definitionValue.getBoolean(PRIMARY_KEY, false)) {
                    lines.add("%s@Id".formatted(StringUtils.repeat(SPACE,
                        TAB_SIZE)));
                }
                var hasAssociation = checkAssociation(lines, definitionValue);
                if (hasAssociation) {
                    checkColumnDefinition(lines, definitionValue, JOIN_COLUMN_ANNOTATION,
                        joinColumnAnnotationProperties);
                } else {
                    checkColumnDefinition(lines, definitionValue, COLUMN_ANNOTATION,
                        columnAnnotationProperties);
                }
                lines.add("%sprivate %s %s;".formatted(StringUtils.repeat(SPACE, TAB_SIZE), type,
                    fieldName));
                lines.add(EMPTY);
            });
            lines.add("}");
            FileClassUtil.writeClassFile(projectInfo, packageName, className, lines, INFRASTRUCTURE);

        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private String getPropertyValue(JsonObject definitionValue,
                                    JsonObject annotationsProperties,
                                    String property) {
        var valueType = annotationsProperties.getString(property, STRING_TYPE);
        if (StringUtils.equals(valueType, STRING_TYPE)) {
            return "\"%s\"".formatted(definitionValue.getString(property));
        }
        if (StringUtils.equals(valueType, INTEGER_TYPE)) {
            return "%s".formatted(definitionValue.getInt(property));
        }
        if (StringUtils.equals(valueType, BOOLEAN_TYPE)) {
            return "\"%s\"".formatted(definitionValue.getBoolean(property));
        }

        return null;
    }

    private void checkColumnDefinition(List<String> lines,
                                       JsonObject definitionValue,
                                       String annotation,
                                       JsonObject annotationsProperties) {
        var propColumns = definitionValue.keySet()
            .stream()
            .filter(annotationsProperties::containsKey).
            toList();
        if (propColumns.isEmpty()) {
            return;
        }
        var importColumn = IMPORT_COLUMN.formatted(annotation);
        if (!lines.contains(importColumn)) {
            lines.add(2, importColumn);
        }
        lines.add("%s@%s(".formatted(StringUtils.repeat(SPACE, TAB_SIZE), annotation));
        propColumns.forEach(property -> {
            var propertyValue = getPropertyValue(definitionValue, annotationsProperties, property);
            if (propertyValue != null) {
                lines.add("%s%s = %s,".formatted(StringUtils.repeat(SPACE, TAB_SIZE * 2), property,
                    propertyValue));
            }
        });

        removeLastComma(lines);

        lines.add("%s)".formatted(StringUtils.repeat(SPACE, TAB_SIZE)));
    }

    private void readAnnotationsDefinitions() throws IOException, InterruptedException,
                                                     URISyntaxException {
        JsonObject definitions = HttpClientUtil.getJson(JAKARTA_LEMON_CONFIG_URL,
            JsonReader::readObject);
        this.columnAnnotationProperties = definitions.getJsonObject(ANNOTATION_PROPS).getJsonObject(
            COLUMN_ANNOTATION);
        this.joinColumnAnnotationProperties = definitions.getJsonObject(ANNOTATION_PROPS).
            getJsonObject(JOIN_COLUMN_ANNOTATION);
    }

    private void createPersistenceXml() {
        try {
            this.persistenceXmlPath = Paths.get(".", INFRASTRUCTURE, SRC, MAIN, RESOURCES,
                PERSISTENCE_FILE_NAME).
                normalize();
            Files.createDirectories(persistenceXmlPath.getParent());

            this.persistenceXml = DocumentXmlUtil.openDocument(persistenceXmlPath).orElseGet(() -> {
                try {
                    var document = DocumentXmlUtil.newDocument(PERSISTENCE);
                    DocumentXmlUtil.listElementsByFilter(document, SLASH + PERSISTENCE)
                        .stream()
                        .findFirst()
                        .ifPresent(persistenceElement -> {
                            persistenceElement.setAttribute(XMLNS,
                                "https://jakarta.ee/xml/ns/persistence");
                            persistenceElement.setAttribute(XMLNS_XSI, XMLNS_XSI_INSTANCE);
                            persistenceElement.setAttribute(VERSION, "3.0");
                        });
                    return document;
                } catch (ParserConfigurationException | XPathExpressionException ex) {
                    log.error(ex.getMessage(), ex);
                }
                return null;
            });
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }

    }

    public void savePersistenceXml() {
        DocumentXmlUtil.saveDocument(persistenceXmlPath, persistenceXml);
    }

    public void createPersistenceUnit(String dataSourceName,
                                      String persistenceUnitName) {
        try {
            var xPath = XPathFactory.newInstance().newXPath();
            var persistenceUnitExp = "/persistence/persistence-unit[@name='%s']".
                formatted(persistenceUnitName);
            var searchResult = (NodeList) xPath.compile(persistenceUnitExp).evaluate(persistenceXml,
                NODESET);
            if (searchResult.getLength() == 0) {

                DocumentXmlUtil.createElement(persistenceXml, "/persistence", "persistence-unit")
                    .ifPresent(persistenceUnitElement -> {
                        persistenceUnitElement.setAttribute(NAME, persistenceUnitName);
                        DocumentXmlUtil.createElement(persistenceXml, persistenceUnitElement,
                            "jta-data-source", dataSourceName);
                    });
            }
        } catch (XPathExpressionException e) {
            log.error(e.getMessage(), e);
        }
    }

    public void createEntityManagerProvider(JsonObject projectInfo,
                                            String persistenceUnitName) {
        try {
            var javaFileBuilder = new JavaFileBuilder()
                .setModulePath(projectInfo.getString(INFRASTRUCTURE))
                .setPackage(projectInfo.getString(PACKAGE), INFRASTRUCTURE, ADAPTERS)
                .addClassAnnotation("ApplicationScoped")
                .addImportClass("jakarta.enterprise.context.ApplicationScoped")
                .addImportClass("jakarta.enterprise.inject.Produces")
                .addImportClass("jakarta.persistence.PersistenceContext")
                .addImportClass("jakarta.persistence.EntityManager")
                .setClassName("JpaProvider")
                .addVariableDeclaration("EntityManager", "em",
                    "Produces @PersistenceContext(unitName = \"%s\")".formatted(
                        persistenceUnitName));
            javaFileBuilder.build();
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private static class JpaPersistenceHandlerHolder {

        private static final JpaPersistenceHandler INSTANCE = new JpaPersistenceHandler();
    }
}
