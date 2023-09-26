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

import dev.jakartalemon.cli.model.BuildModel;
import dev.jakartalemon.cli.model.PomModel;
import dev.jakartalemon.cli.util.Constants;
import dev.jakartalemon.cli.util.DependenciesUtil;
import dev.jakartalemon.cli.util.FileClassUtil;
import dev.jakartalemon.cli.util.HttpClientUtil;
import dev.jakartalemon.cli.util.JsonFileUtil;
import dev.jakartalemon.cli.util.PomUtil;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static dev.jakartalemon.cli.util.Constants.ADAPTERS;
import static dev.jakartalemon.cli.util.Constants.ARTIFACT_ID;
import static dev.jakartalemon.cli.util.Constants.DOMAIN;
import static dev.jakartalemon.cli.util.Constants.ENTITIES;
import static dev.jakartalemon.cli.util.Constants.FIELDS;
import static dev.jakartalemon.cli.util.Constants.GROUP_ID;
import static dev.jakartalemon.cli.util.Constants.INFRASTRUCTURE;
import static dev.jakartalemon.cli.util.Constants.JAKARTA_ANOTATION;
import static dev.jakartalemon.cli.util.Constants.JAKARTA_ANOTATION_API;
import static dev.jakartalemon.cli.util.Constants.JAKARTA_ANOTATION_API_VERSION_KEY;
import static dev.jakartalemon.cli.util.Constants.JAKARTA_CDI_API;
import static dev.jakartalemon.cli.util.Constants.JAKARTA_CDI_API_VERSION_KEY;
import static dev.jakartalemon.cli.util.Constants.JAKARTA_ENTERPRISE;
import static dev.jakartalemon.cli.util.Constants.JAR;
import static dev.jakartalemon.cli.util.Constants.LOMBOK_DEPENDENCY;
import static dev.jakartalemon.cli.util.Constants.MAVEN_QUERY_PERSISTENCE_API;
import static dev.jakartalemon.cli.util.Constants.MOCKITO_DEPENDENCY;
import static dev.jakartalemon.cli.util.Constants.ORG_MAPSTRUCT;
import static dev.jakartalemon.cli.util.Constants.PACKAGE;
import static dev.jakartalemon.cli.util.Constants.PACKAGE_TEMPLATE;
import static dev.jakartalemon.cli.util.Constants.PRIMARY_KEY;
import static dev.jakartalemon.cli.util.Constants.PROJECT_GROUP_ID;
import static dev.jakartalemon.cli.util.Constants.PROJECT_VERSION;
import static dev.jakartalemon.cli.util.Constants.TAB_SIZE;
import static dev.jakartalemon.cli.util.Constants.TEMPLATE_2_STRING_COMMA;
import static dev.jakartalemon.cli.util.Constants.TYPE;
import static dev.jakartalemon.cli.util.Constants.VERSION;
import static dev.jakartalemon.cli.util.LinesUtils.removeLastComma;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;

/**
 *
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
@Slf4j
public class InfrastructureModuleHandler {

    private static final String IMPORT_COLUMN = "import jakarta.persistence.Column;";

    private final Map<String, String> importablesMap;
    private final JsonObject databasesConfigs;

    private final List<String> props = List.of("url", "password", "user");
    private Map<String, Class<?>> columnProps = Map.of(
        "columnDefinition", String.class,
        "insertable", Boolean.class,
        "length", Integer.class,
        "name", String.class,
        "nullable", Boolean.class,
        "precision", Integer.class,
        "table", String.class,
        "unique", Boolean.class,
        "updatable", Boolean.class
    );

    private InfrastructureModuleHandler() {
        importablesMap = HttpClientUtil.getConfigs(Constants.IMPORTABLES);
        databasesConfigs = HttpClientUtil.getDatabasesConfigs().orElseThrow();
    }

    public static InfrastructureModuleHandler getInstance() {
        return InfrastructureModuleHandlerHolder.INSTANCE;
    }

    private void addJpaDependency() throws InterruptedException, IOException, URISyntaxException {
        DependenciesUtil.getLastVersionDependency(
            MAVEN_QUERY_PERSISTENCE_API).ifPresent(dependency -> PomUtil.getInstance()
            .addDependency(dependency, INFRASTRUCTURE));
    }

    private void createDataSourceClass(JsonObject projectInfo,
                                       String dataSourceClass,
                                       JsonObject connectionInfo) {
        try {
            List<String> lines = new ArrayList<>();
            var packageName = PACKAGE_TEMPLATE.formatted(projectInfo.getString(PACKAGE), INFRASTRUCTURE,
                ADAPTERS);
            lines.add(TEMPLATE_2_STRING_COMMA.formatted(PACKAGE, packageName));
            lines.add(EMPTY);
            lines.add("import jakarta.annotation.sql.DataSourceDefinition;");
            lines.add(EMPTY);
            lines.add("@DataSourceDefinition(");
            lines.add("%sclassName = \"%s\",".formatted(StringUtils.repeat(SPACE, TAB_SIZE),
                dataSourceClass));
            lines.add("%sname = \"java:global/App/Datasource\",".formatted(StringUtils.repeat(SPACE,
                TAB_SIZE)));
            props.forEach(prop -> {
                if (connectionInfo.containsKey(prop)) {
                    lines.add("%s%s = \"%s\",".formatted(StringUtils.repeat(SPACE,
                        TAB_SIZE), prop, connectionInfo.getString(prop)));
                }
            });
            removeLastComma(lines);

            lines.add(")");
            var className = "DataSourceProvider";
            lines.add("public class %s {".formatted(className));

            lines.add("}");
            FileClassUtil.writeClassFile(projectInfo, packageName, className,
                lines, INFRASTRUCTURE);

        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }

    }

    public void createDatabaseConfig(File file) {
        JsonFileUtil.getFileJson(file.toPath())
            .ifPresent(config -> JsonFileUtil.getProjectInfo().ifPresent(projectInfo -> {
            try {
                var storageType = config.getString("storageType");
                var configDb = databasesConfigs.getJsonObject(storageType);
                log.debug("storageType:{}", storageType);
                log.debug("configDb:{}", configDb);

                addJpaDependency();

                DependenciesUtil.getLastVersionDependency(configDb.getString(
                    "search")).ifPresent(dependency -> PomUtil.getInstance()
                    .addDependency(dependency, INFRASTRUCTURE));
                var dataSourceClass = configDb.getString("datasource");
                var connectionInfo = config.getJsonObject("connectionInfo");
                var connectionType = config.getString("connectionType");
                switch (connectionType) {
                    case "DataSourceEmbedded" ->
                        createDataSourceClass(projectInfo, dataSourceClass, connectionInfo);
                }

            } catch (InterruptedException | IOException | URISyntaxException e) {
                log.error(e.getMessage(), e);

            }
        }));

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
                var type = definitionValue.getString(TYPE, "String");
                if (definitionValue.getBoolean(PRIMARY_KEY, false)) {
                    lines.add("%s@Id".formatted(StringUtils.repeat(SPACE,
                        TAB_SIZE)));
                }
                checkColumnDefinition(lines, definitionValue);
                lines.add("%sprivate %s %s;".formatted(StringUtils.repeat(SPACE, TAB_SIZE), type,
                    fieldName));
                lines.add(EMPTY);
            });
            lines.add("}");
            FileClassUtil.writeClassFile(projectInfo, packageName, className,lines, INFRASTRUCTURE);

        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private void checkColumnDefinition(List<String> lines,
                                       JsonObject definitionValue) {
        var propColumns = definitionValue.keySet()
            .stream()
            .filter(key -> columnProps.containsKey(key)).
            toList();
        if (propColumns.isEmpty()) {
            return;
        }
        if (!lines.contains(IMPORT_COLUMN)) {
            lines.add(2, IMPORT_COLUMN);
        }
        lines.add("%s@Column(".formatted(StringUtils.repeat(SPACE, TAB_SIZE)));
        propColumns.forEach(property -> {
            var propertyValue = getPropertyValue(definitionValue, property);
            if (propertyValue != null) {
                lines.add("%s%s = %s,".formatted(StringUtils.repeat(SPACE, TAB_SIZE * 2), property,
                    propertyValue));
            }
        });

        removeLastComma(lines);

        lines.add("%s)".formatted(StringUtils.repeat(SPACE, TAB_SIZE)));
    }

    private String getPropertyValue(JsonObject definitionValue,
                                    String property) {
        var valueType = columnProps.get(property);
        if (valueType == String.class) {
            return "\"%s\"".formatted(definitionValue.getString(property));
        }
        if (valueType == Integer.class) {
            return "%s".formatted(definitionValue.getInt(property));
        }
        if (valueType == Boolean.class) {
            return "\"%s\"".formatted(definitionValue.getBoolean(property));
        }

        return null;
    }

    public Optional<Path> createInfrastructureModule(Path projectPath,
                                                     String groupId,
                                                     String artifactId,
                                                     String version,
                                                     String packageName) {
        var modulePom = PomModel.builder()
            .parent(Map.of(GROUP_ID, groupId,
                ARTIFACT_ID, artifactId,
                VERSION, version
            ))
            .artifactId(INFRASTRUCTURE)
            .packaging(JAR)
            .dependencies(
                List.of(
                    Map.of(
                        GROUP_ID, "org.projectlombok",
                        ARTIFACT_ID, "lombok",
                        VERSION, "${org.projectlombok.version}"
                    ), Map.of(
                    GROUP_ID, ORG_MAPSTRUCT,
                    ARTIFACT_ID, "mapstruct",
                    VERSION, "${org.mapstruct.version}"
                ),
                    MOCKITO_DEPENDENCY,
                    LOMBOK_DEPENDENCY,
                    Map.of(
                        GROUP_ID, PROJECT_GROUP_ID,
                        ARTIFACT_ID, DOMAIN,
                        VERSION, PROJECT_VERSION
                    ), Map.of(
                    GROUP_ID, JAKARTA_ENTERPRISE,
                    ARTIFACT_ID, JAKARTA_CDI_API,
                    VERSION, "${%s}".formatted(JAKARTA_CDI_API_VERSION_KEY)
                ), Map.of(
                    GROUP_ID, JAKARTA_ANOTATION,
                    ARTIFACT_ID, JAKARTA_ANOTATION_API,
                    VERSION, "${%s}".formatted(JAKARTA_ANOTATION_API_VERSION_KEY)
                )
                )
            ).buildModel(
                BuildModel.builder()
                    .plugins(Json.createArrayBuilder()
                        .add(
                            Json.createObjectBuilder()
                                .add(GROUP_ID, "org.apache.maven.plugins")
                                .add(ARTIFACT_ID, "maven-compiler-plugin")
                                .add(VERSION, "3.11.0")
                                .add("configuration", Json.createObjectBuilder()
                                    .add("annotationProcessorPaths", Json.createObjectBuilder()
                                        .add("path", Json.createObjectBuilder()
                                            .add(GROUP_ID, ORG_MAPSTRUCT)
                                            .add(ARTIFACT_ID, "mapstruct-processor")
                                            .add(VERSION, "${org.mapstruct.version}")))
                                )
                        ).build()
                    ).build());
        var pomPath = PomUtil.getInstance().createPom(projectPath.resolve(INFRASTRUCTURE),
            modulePom.build());

        pomPath.ifPresent(pom -> {
            log.debug("entities created at {}", pom.toAbsolutePath());
            PomUtil.getInstance()
                .createJavaProjectStructure(pom.getParent(), PACKAGE_TEMPLATE.formatted(
                    packageName, INFRASTRUCTURE, ENTITIES));
            PomUtil.getInstance()
                .createJavaProjectStructure(pom.getParent(), "%s.%s.mapper".formatted(
                    packageName, INFRASTRUCTURE));
            PomUtil.getInstance()
                .createJavaProjectStructure(pom.getParent(), PACKAGE_TEMPLATE.formatted(
                    packageName, INFRASTRUCTURE, ADAPTERS));
        });
        return pomPath;
    }

    private static class InfrastructureModuleHandlerHolder {

        private static final InfrastructureModuleHandler INSTANCE = new InfrastructureModuleHandler();
    }
}
