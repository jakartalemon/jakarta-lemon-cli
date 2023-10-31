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

import static dev.jakartalemon.cli.util.Constants.APPLICATION;
import static dev.jakartalemon.cli.util.Constants.PACKAGE;
import static dev.jakartalemon.cli.util.Constants.RESOURCES;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static dev.jakartalemon.cli.util.Constants.SLASH_CHAR;
import dev.jakartalemon.cli.util.JavaFileBuilder;
import static dev.jakartalemon.cli.util.OpenApiUtil.getType;
import java.io.IOException;

/**
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
@Slf4j
public class RestAdapterHandler {

    private JsonObject schemas;
    private OpenAPI openAPI;
    private Map<String, List<PathItem>> pathsGroup;

    private RestAdapterHandler() {
    }

    public static RestAdapterHandler getInstance() {
        return RestAdapterHandlerHolder.INSTANCE;
    }

    /**
     * Loads the definitions of the REST services that must be created.
     *
     * @param file JSON file with the definitions of the REST services in OpenAPI format
     */
    public void loadOpenApiDefinition(File file) {
        var swaggerParseResult
            = new OpenAPIParser().readLocation(file.getAbsolutePath(), null, null);
        this.openAPI = swaggerParseResult.getOpenAPI();
        Optional.ofNullable(swaggerParseResult.getMessages())
            .ifPresent(messages -> messages.forEach(log::error));

    }

    public void createComponents(Consumer<JsonObject> createClasses) {
        loadComponentsDefinitions();
        createClasses.accept(schemas);

    }

    private void loadComponentsDefinitions() {
        var jsonBuilder = Json.createObjectBuilder();
        openAPI.getComponents().getSchemas().forEach((schemaName, schema) -> {
            var type = getType(schema);
            var schemaDefinition = Json.createObjectBuilder();
            Optional.ofNullable(schema.getProperties()).ifPresent(properties -> {
                properties.forEach((propertyName, property) -> {
                    var propertyType = getType((Schema<?>) property);
                    schemaDefinition.add((String) propertyName, propertyType);
                });
            });
            jsonBuilder.add(schemaName, schemaDefinition);

        });

        schemas = jsonBuilder.build();
    }

    public void createPaths(Consumer<Map<String, List<PathItem>>> consumer) {
        loadPathsDefinitions();
        consumer.accept(pathsGroup);
    }

    private void loadPathsDefinitions() {
        this.pathsGroup = new LinkedHashMap<>();
        openAPI.getPaths().forEach((pathName, pathItem) -> {
            var endPos = StringUtils.indexOf(pathName, SLASH_CHAR, 1);
            var prefix = endPos >= 0 ? StringUtils.substring(pathName, 0, endPos) : pathName;
            List<PathItem> paths = pathsGroup.getOrDefault(prefix, new ArrayList<>());
            paths.add(pathItem);
            pathsGroup.putIfAbsent(prefix, paths);
        });

    }

    public void createApplicationPath(JsonObject projectInfo) {
        try {
            var className = "ApplicationResource";
            var javaFileBuilder
                = new JavaFileBuilder().setClassName(className)
                    .addImportClass("jakarta.ws.rs.ApplicationPath")
                    .addImportClass("jakarta.ws.rs.core.Application")
                    .addClassAnnotation("ApplicationPath(\"/api\")")
                    .setFileName(className)
                    .setModulePath(projectInfo.getString(APPLICATION))
                    .setModule(APPLICATION)
                    .setFileName(className)
                    .setExtendClass("Application")
                    .setPackage(projectInfo.getString(PACKAGE), APPLICATION, RESOURCES);
            javaFileBuilder.build();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private static class RestAdapterHandlerHolder {

        private static final RestAdapterHandler INSTANCE = new RestAdapterHandler();
    }
}
