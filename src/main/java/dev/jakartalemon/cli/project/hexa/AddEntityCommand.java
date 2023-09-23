/*
 * Copyright 2023 diego.
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
package dev.jakartalemon.cli.project.hexa;

import dev.jakartalemon.cli.util.Constants;
import dev.jakartalemon.cli.util.DependenciesUtil;
import dev.jakartalemon.cli.util.FileClassUtil;
import dev.jakartalemon.cli.util.HttpClientUtil;
import dev.jakartalemon.cli.util.JsonFileUtil;
import dev.jakartalemon.cli.util.PomUtil;
import jakarta.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static dev.jakartalemon.cli.util.Constants.ADAPTERS;
import static dev.jakartalemon.cli.util.Constants.ENTITIES;
import static dev.jakartalemon.cli.util.Constants.ENTITY;
import static dev.jakartalemon.cli.util.Constants.FIELDS;
import static dev.jakartalemon.cli.util.Constants.INFRASTRUCTURE;
import static dev.jakartalemon.cli.util.Constants.LENGTH;
import static dev.jakartalemon.cli.util.Constants.MAVEN_QUERY_PERSISTENCE_API;
import static dev.jakartalemon.cli.util.Constants.PACKAGE;
import static dev.jakartalemon.cli.util.Constants.PACKAGE_TEMPLATE;
import static dev.jakartalemon.cli.util.Constants.PRIMARY_KEY;
import static dev.jakartalemon.cli.util.Constants.TAB_SIZE;
import static dev.jakartalemon.cli.util.Constants.TEMPLATE_2_STRING_COMMA;
import static dev.jakartalemon.cli.util.Constants.TYPE;
import jakarta.json.JsonValue;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;

/**
 * @author Diego Silva <diego.silva at apuntesdejava.com>
 */
@CommandLine.Command(
    name = "addentity",
    resourceBundle = "messages",
    description = "Create a entity model to infrastructure. The model is given from the console "
    + "in JSON format"
)
@Slf4j
public class AddEntityCommand implements Callable<Integer> {

    private final Map<String, String> importablesMap;
    private final JsonObject databasesConfigs;

    @CommandLine.Parameters(
        paramLabel = "ENTITY_DEFINITION.json",
        descriptionKey = "entity_definition"
    )
    private File file;

    public AddEntityCommand() throws InterruptedException {
        importablesMap = HttpClientUtil.getConfigs(Constants.IMPORTABLES);
        databasesConfigs = HttpClientUtil.getDatabasesConfigs().orElseThrow();
    }

    @Override
    public Integer call() throws Exception {
        createDatabaseConfig();
        return JsonFileUtil.getFileJson(file.toPath())
            .map(structure -> JsonFileUtil.getProjectInfo().map(projectInfo -> {
            structure.getJsonArray(ENTITIES).stream()
                .map(JsonValue::asJsonObject)
                .forEach(item -> item.forEach(
                (key, classDef) -> createEntityClass(projectInfo, key,
                    classDef.asJsonObject())));

            return 0;
        }).orElse(1)).orElse(2);
    }

    private void createDatabaseConfig() {
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
                    .addDependency(dependency, INFRASTRUCTURE,
                        ADAPTERS));
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
            var last = StringUtils.substringBeforeLast(lines.get(lines.size() - 1), Constants.COMMA);
            lines.set(lines.size() - 1, last);

            lines.add(")");
            var className = "DataSourceProvider";
            lines.add("public class %s {".formatted(className));

            lines.add("}");
            FileClassUtil.writeClassFile(projectInfo, packageName, className,
                lines, INFRASTRUCTURE, ADAPTERS);

        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }

    }
    private final List<String> props = List.of("url", "password", "user");

    private void createEntityClass(JsonObject projectInfo,
                                   String className,
                                   JsonObject jsonObject) {
        try {
            log.debug("classDefinition:{}", jsonObject);
            List<String> lines = new ArrayList<>();
            var packageName = PACKAGE_TEMPLATE.formatted(projectInfo.getString(PACKAGE), INFRASTRUCTURE,
                ADAPTERS) + "." + ENTITY;
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
                
                lines.add("%sprivate %s %s;".formatted(StringUtils.repeat(SPACE, TAB_SIZE), type,
                    fieldName));
                lines.add(EMPTY);
            });
            lines.add("}");
            FileClassUtil.writeClassFile(projectInfo, packageName, className,
                lines, INFRASTRUCTURE, ADAPTERS);

        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private void addJpaDependency() throws InterruptedException, IOException, URISyntaxException {
        DependenciesUtil.getLastVersionDependency(
            MAVEN_QUERY_PERSISTENCE_API).ifPresent(dependency -> PomUtil.getInstance()
            .addDependency(dependency, INFRASTRUCTURE, ADAPTERS));
    }

    
}
