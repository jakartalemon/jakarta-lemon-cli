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

import dev.jakartalemon.cli.util.Constants;
import static dev.jakartalemon.cli.util.Constants.APPLICATION;
import static dev.jakartalemon.cli.util.Constants.PACKAGE;
import static dev.jakartalemon.cli.util.Constants.RESOURCES;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
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
import dev.jakartalemon.cli.util.OpenApiUtil;
import static dev.jakartalemon.cli.util.OpenApiUtil.getType;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.parameters.Parameter;
import jakarta.json.JsonValue;
import java.io.IOException;
import java.util.AbstractMap;

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

    public void createResourcesPath(Map<String, List<PathItem>> pathDefinitions,
                                    JsonObject projectInfo) {
        log.info("Creating resources");
        pathDefinitions.forEach((pathName, contents) -> createRestResourceBody(pathName, contents,
            projectInfo));
    }

    private void createRestResourceBody(String pathName,
                                        List<PathItem> contents,
                                        JsonObject projectInfo) {
        String className = StringUtils.capitalize(StringUtils.substringAfter(pathName, SLASH_CHAR)) + "Resource";
        JavaFileBuilder javaFileBuilder = new JavaFileBuilder().setClassName(className).
            addImportClass("jakarta.ws.rs.Path").
            addImportClass("jakarta.ws.rs.container.AsyncResponse").
            addImportClass("jakarta.ws.rs.container.Suspended").
            addImportClass("java.util.concurrent.ArrayBlockingQueue").
            addImportClass("java.util.concurrent.BlockingQueue").
            addVariableDeclaration("BlockingQueue<AsyncResponse>", "SUSPENDED", null, false, true,
                "new ArrayBlockingQueue<>(5)").
            addClassAnnotation("Path(\"%s\")".formatted(pathName));
        contents.forEach(pathItem -> {
            Optional.ofNullable(pathItem.getGet()).
                ifPresent(getOperation -> createMethod(getOperation, javaFileBuilder, "GET"));
            Optional.ofNullable(pathItem.getPost()).
                ifPresent(postOperation -> createMethod(postOperation, javaFileBuilder, "POST"));
            Optional.ofNullable(pathItem.getDelete()).
                ifPresent(deleteOperation -> createMethod(deleteOperation, javaFileBuilder, "DELETE"));
            Optional.ofNullable(pathItem.getPut()).
                ifPresent(putOperation -> createMethod(putOperation, javaFileBuilder, "PUT"));
        });
        try {
            javaFileBuilder.setModulePath(projectInfo.getString(APPLICATION)).setModule(APPLICATION).
                setFileName(className).
                setPackage(projectInfo.getString(PACKAGE), APPLICATION, RESOURCES).build();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void createMethod(Operation operation,
                              JavaFileBuilder javaFileBuilder,
                              String method) {

        String operationId = operation.getOperationId();
        List<Parameter> parameters = operation.getParameters();

        List<String> annotations = new ArrayList<>();
        annotations.add(method);
        Optional.ofNullable(operation.getResponses()).
            ifPresent(responses -> {
                javaFileBuilder.addImportClass("jakarta.ws.rs.Produces");
                responses.forEach((key, response) -> {
                    Content content = response.getContent();
                    String annotation = StringUtils.join(content.keySet().stream().
                        map(str -> ('"' + str + '"')).toList(), Constants.COMMA);
                    annotations.add("Produces({%s})".formatted(annotation));
                });
            });
        Optional.ofNullable(operation.getRequestBody())
            .ifPresent(requestBody -> {
                javaFileBuilder.addImportClass("jakarta.ws.rs.Consumes");
                Optional.ofNullable(requestBody.getContent()).ifPresent(content -> {
                    String annotation = StringUtils.join(content.keySet().stream().
                        map(str -> ('"' + str + '"')).toList(), Constants.COMMA);
                    annotations.add("Consumes({%s})".formatted(annotation));
                });

            });
        javaFileBuilder.addMethod(operationId,
            convertParamsToList(parameters), "void", null,
            annotations).addImportClass("jakarta.ws.rs." + method);
    }

    private List<Map.Entry<String, JsonValue>> convertParamsToList(List<Parameter> parameters) {
        List<Map.Entry<String, JsonValue>> list = new ArrayList<>();
        Optional.ofNullable(parameters).
            ifPresent(parametersList -> parametersList.forEach(parameter -> {
            String entryName = parameter.getName();
            String type = OpenApiUtil.getType(parameter.getSchema());
            Map.Entry<String, JsonValue> item = new AbstractMap.SimpleImmutableEntry<>(entryName,
                Json.createValue(type));
            list.add(item);
        }));
        return list;
    }

    private static class RestAdapterHandlerHolder {

        private static final RestAdapterHandler INSTANCE = new RestAdapterHandler();
    }
}
