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

import dev.jakartalemon.cli.model.PomModel;
import dev.jakartalemon.cli.util.Constants;
import dev.jakartalemon.cli.util.JsonFileUtil;
import dev.jakartalemon.cli.util.PomUtil;
import dev.jakartalemon.cli.util.RecordFileBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static dev.jakartalemon.cli.util.Constants.APPLICATION;
import static dev.jakartalemon.cli.util.Constants.MODEL;
import static dev.jakartalemon.cli.util.Constants.PACKAGE;

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

    public void createClases(JsonObject definitions) {
        JsonFileUtil.getProjectInfo().ifPresent(projectInfo -> {

            definitions.forEach(
                (className, properties) -> createRecord(className, properties.asJsonObject(),
                                                        projectInfo));
        });
    }

    private void createRecord(String className, JsonObject properties,
                              JsonObject projectInfo) {
        try {
            var packageName = projectInfo.getString(PACKAGE);
            var recordFileBuilder = new RecordFileBuilder()
                .setFileName(className)
                .setPackage(packageName, APPLICATION, MODEL);
            properties.forEach((property, propertyType) -> {
                recordFileBuilder.addVariableDeclaration(
                    openApiType2JavaType(((JsonString) propertyType).getString()), property);
            });

            recordFileBuilder
                .setModulePath(projectInfo.getString(APPLICATION))
                .setClassName(className)
                .setModule(APPLICATION)
                .build();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private String openApiType2JavaType(String openApiType) {
        return switch (openApiType) {
            case "string" -> "String";
            default -> openApiType;
        };
    }

    public Optional<Path> createApplicationModule(Path projectPath,
                                                  String groupId,
                                                  String artifactId,
                                                  String version) {
        PomModel.PomModelBuilder modulePom = PomModel.builder().
            parent(Map.of(Constants.GROUP_ID, groupId, Constants.ARTIFACT_ID, artifactId,
                          Constants.VERSION, version))
            .artifactId(Constants.APPLICATION)
            .packaging(Constants.WAR).
            dependencies(List.of(Map.of(Constants.GROUP_ID, Constants.PROJECT_GROUP_ID,
                                        Constants.ARTIFACT_ID, Constants.DOMAIN, Constants.VERSION,
                                        Constants.PROJECT_VERSION),
                                 Constants.JAKARTA_INJECT_DEPENDENCY));
        Optional<Path> pomPath = PomUtil.getInstance().
            createPom(projectPath.resolve(Constants.APPLICATION), modulePom.build());
        pomPath.ifPresent(pom -> {
            log.debug("application created at {}", pom.toAbsolutePath());
        });
        return pomPath;
    }

    public void addRestDepdendencies() {
    }

    private static class ApplicationModuleHandlerHolder {

        private static final ApplicationModuleHandler INSTANCE = new ApplicationModuleHandler();
    }
}
