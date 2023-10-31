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
import dev.jakartalemon.cli.util.JavaFileBuilder;
import dev.jakartalemon.cli.util.OpenApiUtil;
import dev.jakartalemon.cli.util.PomUtil;
import dev.jakartalemon.cli.util.RecordFileBuilder;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.Parameter;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static dev.jakartalemon.cli.util.Constants.APPLICATION;
import static dev.jakartalemon.cli.util.Constants.MAVEN_QUERY_JAKARTA_WS_RS_API;
import static dev.jakartalemon.cli.util.Constants.MODEL;
import static dev.jakartalemon.cli.util.Constants.PACKAGE;
import static dev.jakartalemon.cli.util.Constants.RESOURCES;
import static dev.jakartalemon.cli.util.Constants.SLASH_CHAR;
import static dev.jakartalemon.cli.util.OpenApiUtil.openApiType2JavaType;

/**
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
@Slf4j
public class ApplicationModuleHandler {

    private ApplicationModuleHandler() {
    }

    public static ApplicationModuleHandler getInstance() {
        return ApplicationModuleHandlerHolder.INSTANCE;
    }

    public void createRecords(JsonObject definitions,
                              JsonObject projectInfo) {
        definitions.forEach(
            (className, properties) -> createRecord(className, properties.asJsonObject(),
                projectInfo));
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
        var className = StringUtils.capitalize(StringUtils.substringAfter(pathName, SLASH_CHAR))
            + "Resource";
        var javaFileBuilder
            = new JavaFileBuilder().setClassName(className)
                .addImportClass("jakarta.ws.rs.Path")
                .addImportClass("jakarta.ws.rs.container.AsyncResponse")
                .addImportClass("jakarta.ws.rs.container.Suspended")
                .addImportClass("java.util.concurrent.ArrayBlockingQueue")
                .addImportClass("java.util.concurrent.BlockingQueue")
                .addVariableDeclaration("BlockingQueue<AsyncResponse>", "SUSPENDED", null, false,
                    true,
                    "new ArrayBlockingQueue<>(5)")
                .addClassAnnotation("Path(\"%s\")".formatted(pathName));
        contents.forEach(pathItem -> {
            Optional.ofNullable(pathItem.getGet()).ifPresent(getOperation -> {
                var operationId = getOperation.getOperationId();
                var parameters = getOperation.getParameters();
                var responses = getOperation.getResponses();

                javaFileBuilder.addMethod(operationId, convertParamsToList(parameters), "void",
                    null, List.of("GET"))
                    .addImportClass("jakarta.ws.rs.GET");

            });
            Optional.ofNullable(pathItem.getPost()).ifPresent(postOperation -> {
                var operationId = postOperation.getOperationId();
                var parameters = postOperation.getParameters();
                var responses = postOperation.getResponses();

                javaFileBuilder.addMethod(operationId, convertParamsToList(parameters), "void",
                    null, List.of("POST"))
                    .addImportClass("jakarta.ws.rs.POST");

            });
            Optional.ofNullable(pathItem.getDelete()).ifPresent(deleteOperation -> {

            });
            Optional.ofNullable(pathItem.getPut()).ifPresent(putOperation -> {

            });
        });
        try {
            javaFileBuilder
                .setModulePath(projectInfo.getString(APPLICATION))
                .setModule(APPLICATION)
                .setFileName(className)
                .setPackage(projectInfo.getString(PACKAGE), APPLICATION, RESOURCES)
                .build();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private List<Map.Entry<String, JsonValue>> convertParamsToList(List<Parameter> parameters) {
        List<Map.Entry<String, JsonValue>> list = new ArrayList<>();
        Optional.ofNullable(parameters)
            .ifPresent(parametersList -> parametersList.forEach(parameter -> {
            var entryName = parameter.getName();
            var type = OpenApiUtil.getType(parameter.getSchema());
            Map.Entry<String, JsonValue> item
                = new AbstractMap.SimpleImmutableEntry<>(entryName, Json.createValue(type));
            list.add(item);
        }));
        return list;
    }

    private void createRecord(String className,
                              JsonObject properties,
                              JsonObject projectInfo) {
        try {
            var packageName = projectInfo.getString(PACKAGE);
            var recordFileBuilder = new RecordFileBuilder().setFileName(className)
                .setPackage(packageName, APPLICATION, MODEL);
            properties.forEach((property, propertyType) -> {
                recordFileBuilder.addVariableDeclaration(
                    openApiType2JavaType(((JsonString) propertyType).getString()), property);
            });

            recordFileBuilder.setModulePath(projectInfo.getString(APPLICATION))
                .setClassName(className).setModule(APPLICATION).build();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public Optional<Path> createApplicationModule(Path projectPath,
                                                  String groupId,
                                                  String artifactId,
                                                  String version) {
        PomModel.PomModelBuilder modulePom = PomModel.builder().parent(
            Map.of(
                Constants.GROUP_ID, groupId,
                Constants.ARTIFACT_ID, artifactId,
                Constants.VERSION, version)
        ).artifactId(Constants.APPLICATION)
            .packaging(Constants.WAR)
            .properties(
                Map.of(
                    "endorsed.dir", "${project.build.directory}/endorsed",
                    "project.build.sourceEncoding", "UTF-8"
                )
            )
            .dependencies(
                List.of(
                    Map.of(
                        Constants.GROUP_ID, Constants.PROJECT_GROUP_ID,
                        Constants.ARTIFACT_ID, Constants.DOMAIN,
                        Constants.VERSION, Constants.PROJECT_VERSION
                    ),
                    Constants.JAKARTA_INJECT_DEPENDENCY,
                    Constants.INFRASTRUCTURE_DEPENDENCY
                )
            ).buildModel(
                BuildModel.builder()
                    .plugins(
                        Json.createArrayBuilder()
                            .add(
                                Json.createObjectBuilder()
                                    .add(Constants.GROUP_ID, "org.apache.maven.plugins")
                                    .add(Constants.ARTIFACT_ID, "maven-compiler-plugin")
                                    .add(Constants.VERSION, "3.11.0")
                                    .add(Constants.CONFIGURATION, Json.createObjectBuilder().add(
                                        "compilerArguments", Json.createObjectBuilder()
                                            .add("endorseddirs", "${endorsed.dir}")))
                            )
                            .add(
                                Json.createObjectBuilder()
                                    .add(Constants.GROUP_ID, "org.apache.maven.plugins")
                                    .add(Constants.ARTIFACT_ID, "maven-war-plugin")
                                    .add(Constants.VERSION, "3.4.0")
                                    .add(Constants.CONFIGURATION, Json.createObjectBuilder(Map.of(
                                        "failOnMissingWebXml", "false")
                                    ))
                            )
                            .build()
                    )
                    .build());
        Optional<Path> pomPath = PomUtil.getInstance()
            .createPom(projectPath.resolve(Constants.APPLICATION), modulePom.build());
        pomPath.ifPresent(pom -> {
            log.debug("application created at {}", pom.toAbsolutePath());
        });
        return pomPath;
    }

    public void addRestDependencies() {
        try {
            DependenciesUtil.getLastVersionDependency(
                MAVEN_QUERY_JAKARTA_WS_RS_API).ifPresent(dependency -> PomUtil.getInstance()
                .addDependency(dependency, APPLICATION));
        } catch (InterruptedException | IOException | URISyntaxException e) {
            log.error(e.getMessage(), e);
        }
    }

   

    private static class ApplicationModuleHandlerHolder {

        private static final ApplicationModuleHandler INSTANCE = new ApplicationModuleHandler();
    }
}
