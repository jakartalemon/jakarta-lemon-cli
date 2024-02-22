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

import com.camucode.gen.DefinitionBuilder;
import com.camucode.gen.FieldDefinitionBuilder;
import com.camucode.gen.type.AnnotationType;
import com.camucode.gen.type.AnnotationTypeBuilder;
import com.camucode.gen.type.ClassType;
import com.camucode.gen.type.ClassTypeBuilder;
import com.camucode.gen.values.Modifier;
import dev.jakartalemon.cli.util.DependenciesUtil;
import dev.jakartalemon.cli.util.DocumentXmlUtil;
import dev.jakartalemon.cli.util.HttpClientUtil;
import dev.jakartalemon.cli.util.PomUtil;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static dev.jakartalemon.cli.util.Constants.ADAPTERS;
import static dev.jakartalemon.cli.util.Constants.ANNOTATION_PROPS;
import static dev.jakartalemon.cli.util.Constants.BOOLEAN_TYPE;
import static dev.jakartalemon.cli.util.Constants.COLUMN_ANNOTATION;
import static dev.jakartalemon.cli.util.Constants.DOMAIN;
import static dev.jakartalemon.cli.util.Constants.DOT;
import static dev.jakartalemon.cli.util.Constants.FIELDS;
import static dev.jakartalemon.cli.util.Constants.INFRASTRUCTURE;
import static dev.jakartalemon.cli.util.Constants.INTEGER_TYPE;
import static dev.jakartalemon.cli.util.Constants.JAKARTA_LEMON_CONFIG_URL;
import static dev.jakartalemon.cli.util.Constants.JAVA;
import static dev.jakartalemon.cli.util.Constants.JOIN_COLUMN_ANNOTATION;
import static dev.jakartalemon.cli.util.Constants.MAIN;
import static dev.jakartalemon.cli.util.Constants.MANY_TO_ONE;
import static dev.jakartalemon.cli.util.Constants.MAVEN_QUERY_PERSISTENCE_API;
import static dev.jakartalemon.cli.util.Constants.MAVEN_QUERY_TRANSACTION_API;
import static dev.jakartalemon.cli.util.Constants.META_INF;
import static dev.jakartalemon.cli.util.Constants.NAME;
import static dev.jakartalemon.cli.util.Constants.ONE_TO_ONE;
import static dev.jakartalemon.cli.util.Constants.PACKAGE;
import static dev.jakartalemon.cli.util.Constants.PACKAGE_TEMPLATE;
import static dev.jakartalemon.cli.util.Constants.PRIMARY_KEY;
import static dev.jakartalemon.cli.util.Constants.REPOSITORY;
import static dev.jakartalemon.cli.util.Constants.RESOURCES;
import static dev.jakartalemon.cli.util.Constants.SLASH;
import static dev.jakartalemon.cli.util.Constants.SRC;
import static dev.jakartalemon.cli.util.Constants.STRING_TYPE;
import static dev.jakartalemon.cli.util.Constants.TYPE;
import static dev.jakartalemon.cli.util.Constants.VERSION;
import static dev.jakartalemon.cli.util.Constants.XMLNS;
import static dev.jakartalemon.cli.util.Constants.XMLNS_XSI;
import static dev.jakartalemon.cli.util.Constants.XMLNS_XSI_INSTANCE;
import static javax.xml.xpath.XPathConstants.NODESET;

/**
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
@Slf4j
public class JpaPersistenceHandler {

    private static final String PERSISTENCE_FILE_NAME = "persistence.xml";
    private static final String PERSISTENCE = "persistence";

    private static final Map<String, AnnotationType> ASSOCIATIONS_ANNOTATIONS = Map.of(
        MANY_TO_ONE,
        AnnotationTypeBuilder.newBuilder()
            .classType(ClassTypeBuilder.newBuilder().className("ManyToOne").packageName("jakarta.persistence").build())
            .build(),
        ONE_TO_ONE,
        AnnotationTypeBuilder.newBuilder()
            .classType(ClassTypeBuilder.newBuilder().className("OneToOne").packageName("jakarta.persistence").build())
            .build()
    );

    public static JpaPersistenceHandler getInstance() {
        return JpaPersistenceHandlerHolder.INSTANCE;
    }

    public static void createRepositoryImplementation(String modelName,
                                                      String packagePath,
                                                      String infrastructurePath) {
        try {
            var packageName = PACKAGE_TEMPLATE.formatted(packagePath, INFRASTRUCTURE, REPOSITORY);
            var packageRepository = PACKAGE_TEMPLATE.formatted(packagePath, DOMAIN, REPOSITORY);
            var injectAnnotation = AnnotationTypeBuilder.newBuilder()
                .classType(ClassTypeBuilder.newBuilder()
                    .packageName("jakarta.inject")
                    .className("Inject")
                    .build())
                .build();
            var repositoryClassType = ClassTypeBuilder.newBuilder()
                .className(modelName + "Repository")
                .packageName(packageRepository)
                .build();
            var entityManagerClassType = ClassTypeBuilder.newBuilder()
                .className("EntityManager")
                .packageName("jakarta.persistence")
                .build();

            var repositoryDefinition = DefinitionBuilder.createClassBuilder(packageName, modelName + "RepositoryImpl")
                .addInterfaceImplements(repositoryClassType)
                .addModifier(Modifier.PUBLIC)
                .addField(
                    FieldDefinitionBuilder.createBuilder()
                        .fieldName("em")
                        .addModifier(Modifier.PRIVATE)
                        .classType(entityManagerClassType)
                        .addAnnotationType(injectAnnotation)
                        .build()
                )
                .build();

            var destinationPath = Paths.get(infrastructurePath, SRC, MAIN, JAVA);
            var javaFile = com.camucode.gen.JavaFileBuilder.createBuilder(repositoryDefinition, destinationPath)
                .build();
            javaFile.writeFile();


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
            addTransactionApiDependency();
            readAnnotationsDefinitions();
            createPersistenceXml();
        } catch (InterruptedException | IOException | URISyntaxException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private void addJpaDependency() throws InterruptedException, IOException, URISyntaxException {
        DependenciesUtil.getLastVersionDependency(
            MAVEN_QUERY_PERSISTENCE_API
        ).ifPresent(
            dependency -> PomUtil.getInstance()
                .addDependency(dependency, INFRASTRUCTURE)
        );
    }

    private Optional<AnnotationType> getAnnotationByAssociation(JsonObject definitionValue) {
        Set<String> fields = definitionValue.keySet();
        for (String field : fields) {
            if (ASSOCIATIONS_ANNOTATIONS.containsKey(field))
                return Optional.of(ASSOCIATIONS_ANNOTATIONS.get(field));
        }
        return Optional.empty();
    }


    public void createEntityClass(JsonObject projectInfo,
                                  String className,
                                  JsonObject jsonObject) {
        try {
            log.debug("classDefinition:{}", jsonObject);
            var packageName = PACKAGE_TEMPLATE.formatted(projectInfo.getString(PACKAGE), INFRASTRUCTURE,
                ADAPTERS);

            //create fields
            Collection<FieldDefinitionBuilder.FieldDefinition> fields =
                jsonObject.getJsonObject(FIELDS).entrySet().stream().map(entry -> {
                    JsonObject definitionValue = entry.getValue().asJsonObject();
                    FieldDefinitionBuilder fieldDefinitionBuilder = FieldDefinitionBuilder.createBuilder()
                        .getter(true)
                        .setter(true)
                        .addModifier(Modifier.PRIVATE)
                        .fieldName(entry.getKey())
                        .nativeType(definitionValue.getString(TYPE));
                    if (definitionValue.getBoolean(PRIMARY_KEY, false))
                        fieldDefinitionBuilder.addAnnotationType(
                            AnnotationTypeBuilder.newBuilder()
                                .classType(
                                    ClassTypeBuilder.newBuilder()
                                        .packageName("jakarta.persistence")
                                        .className("Id")
                                        .build()
                                )
                                .build()
                        );

                    //creating associations
                    Optional<AnnotationType> association = getAnnotationByAssociation(definitionValue);

                    //join column
                    AnnotationType annotation = association.map(assoc -> {
                        fieldDefinitionBuilder.addAnnotationType(assoc);
                        return getAnnotationByColumns(definitionValue, joinColumnAnnotationProperties,
                            ClassTypeBuilder.newBuilder().className("JoinColumn").packageName("jakarta.persistence")
                                .build());
                    }).orElseGet(() -> getAnnotationByColumns(definitionValue, //@Column
                        columnAnnotationProperties,
                        ClassTypeBuilder.newBuilder().className("Column").packageName("jakarta.persistence")
                            .build()));
                    if (annotation != null)
                        fieldDefinitionBuilder.addAnnotationType(annotation);

                    return fieldDefinitionBuilder.build();
                }).collect(Collectors.toList());
            var entitytDefinition = DefinitionBuilder.createClassBuilder(packageName, className)
                .addModifier(Modifier.PUBLIC)
                .addAnnotationType(
                    AnnotationTypeBuilder.newBuilder()
                        .classType(ClassTypeBuilder.newBuilder()
                            .className("Entity")
                            .packageName("jakarta.persistence")
                            .build())
                        .build()
                )
                .addFields(fields)
                .build();
            var destinationPath = Paths.get(projectInfo.getString(INFRASTRUCTURE), SRC, MAIN, JAVA);
            var javaFile = com.camucode.gen.JavaFileBuilder.createBuilder(entitytDefinition, destinationPath).build();
            javaFile.writeFile();


        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }


    private AnnotationType getAnnotationByColumns(JsonObject definitionValue, JsonObject annotationsProperties,
                                                  ClassType classType) {
        var propColumns = definitionValue.keySet()
            .stream()
            .filter(annotationsProperties::containsKey)
            .toList();
        if (propColumns.isEmpty()) return null;
        AnnotationTypeBuilder annotationTypeBuilder = AnnotationTypeBuilder.newBuilder()
            .classType(classType);

        propColumns.forEach(property -> {
            var propertyType = annotationsProperties.getString(property, STRING_TYPE);
            switch (propertyType) {
                case STRING_TYPE -> annotationTypeBuilder.addAttribute(property, definitionValue.getString(property));
                case INTEGER_TYPE -> annotationTypeBuilder.addAttribute(property, definitionValue.getInt(property));
                case BOOLEAN_TYPE -> annotationTypeBuilder.addAttribute(property, definitionValue.getBoolean(property));
            }

        });

        return annotationTypeBuilder.build();
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
            this.persistenceXmlPath = Paths.get(DOT, INFRASTRUCTURE, SRC, MAIN, RESOURCES, META_INF,
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
                        DocumentXmlUtil.createElement(persistenceXml, persistenceUnitElement,
                                "properties").flatMap(propertiesElement -> DocumentXmlUtil.
                                createElement(persistenceXml,
                                    propertiesElement, "property"))
                            .ifPresent(propertyElement -> {
                                propertyElement.setAttribute("name",
                                    "jakarta.persistence.schema-generation.database.action");
                                propertyElement.setAttribute("value", "create");
                            });
                    });
            }
        } catch (XPathExpressionException e) {
            log.error(e.getMessage(), e);
        }
    }

    public void createEntityManagerProvider(JsonObject projectInfo,
                                            String persistenceUnitName) {
        try {
            var packageName = PACKAGE_TEMPLATE.formatted(projectInfo.getString(PACKAGE), INFRASTRUCTURE,
                ADAPTERS);
            var entityManagerClassType = ClassTypeBuilder.newBuilder()
                .packageName("jakarta.persistence")
                .className("EntityManager")
                .build();
            var produceAnnotation = AnnotationTypeBuilder.newBuilder()
                .classType(
                    ClassTypeBuilder.newBuilder()
                        .packageName("jakarta.enterprise.inject")
                        .className("Produces")
                        .build()
                )
                .build();
            var applicationScopedAnnotation = AnnotationTypeBuilder.newBuilder()
                .classType(
                    ClassTypeBuilder.newBuilder()
                        .packageName("jakarta.enterprise.context")
                        .className("ApplicationScoped")
                        .build()
                )
                .build();
            var persistenceContextAnnotation = AnnotationTypeBuilder.newBuilder()
                .classType(
                    ClassTypeBuilder.newBuilder()
                        .packageName("jakarta.persistence")
                        .className("PersistenceContext")
                        .build()
                )
                .addAttribute("unitName", persistenceUnitName)
                .build();
            var jpaProviderDefinition = DefinitionBuilder.createClassBuilder(packageName, "JpaProvider")
                .addField(FieldDefinitionBuilder.createBuilder()
                    .fieldName("em")
                    .classType(entityManagerClassType)
                    .addModifier(Modifier.PRIVATE)
                    .addAnnotationType(produceAnnotation)
                    .addAnnotationType(persistenceContextAnnotation)
                    .build())
                .addAnnotationType(applicationScopedAnnotation)
                .addModifier(Modifier.PUBLIC)
                .build();

            var destinationPath = Paths.get(projectInfo.getString(INFRASTRUCTURE), SRC, MAIN, JAVA);
            var javaFile = com.camucode.gen.JavaFileBuilder.createBuilder(jpaProviderDefinition, destinationPath)
                .build();
            javaFile.writeFile();
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private void addTransactionApiDependency() throws URISyntaxException, IOException,
        InterruptedException {
        DependenciesUtil.getLastVersionDependency(
            MAVEN_QUERY_TRANSACTION_API
        ).ifPresent(
            dependency -> PomUtil.getInstance()
                .addDependency(dependency, INFRASTRUCTURE)
        );
    }

    private static class JpaPersistenceHandlerHolder {

        private static final JpaPersistenceHandler INSTANCE = new JpaPersistenceHandler();
    }
}
