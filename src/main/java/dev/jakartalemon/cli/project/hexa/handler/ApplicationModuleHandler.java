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
import com.camucode.gen.JavaFileBuilder;
import com.camucode.gen.values.Modifier;
import dev.jakartalemon.cli.model.BuildModel;
import dev.jakartalemon.cli.model.PomModel;
import dev.jakartalemon.cli.util.Constants;
import dev.jakartalemon.cli.util.DependenciesUtil;
import dev.jakartalemon.cli.util.PomUtil;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static dev.jakartalemon.cli.util.Constants.APPLICATION;
import static dev.jakartalemon.cli.util.Constants.JAVA;
import static dev.jakartalemon.cli.util.Constants.MAIN;
import static dev.jakartalemon.cli.util.Constants.MAVEN_QUERY_JAKARTA_WS_RS_API;
import static dev.jakartalemon.cli.util.Constants.MAVEN_QUERY_RXJAVA;
import static dev.jakartalemon.cli.util.Constants.MODEL;
import static dev.jakartalemon.cli.util.Constants.OPEN_API_TYPE;
import static dev.jakartalemon.cli.util.Constants.PACKAGE;
import static dev.jakartalemon.cli.util.Constants.PACKAGE_TEMPLATE;
import static dev.jakartalemon.cli.util.Constants.SRC;
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

    private void createRecord(String className,
                              JsonObject properties,
                              JsonObject projectInfo) {
        try {
            var packageName = PACKAGE_TEMPLATE.formatted(projectInfo.getString(PACKAGE), APPLICATION,
                MODEL);
            var recordBuilder = DefinitionBuilder.createRecordBuilder(packageName, className);
            var fields = properties.entrySet().stream().map(entry -> {
                var fieldName = entry.getKey();
                var propertyType = entry.getValue().asJsonObject().getString(OPEN_API_TYPE);
                var javaType = openApiType2JavaType(propertyType);
                return FieldDefinitionBuilder.createBuilder().fieldName(fieldName)
                    .nativeType(javaType)
                    .build();
            }).toList();
            recordBuilder.addFields(fields).addModifier(Modifier.PUBLIC);
            var destinationPath = Paths.get(projectInfo.getString(APPLICATION), SRC, MAIN, JAVA);
            var javaFile = JavaFileBuilder.createBuilder(recordBuilder.build(), destinationPath)
                .build();
            javaFile.writeFile();
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
            DependenciesUtil.getLastVersionDependency(
                MAVEN_QUERY_RXJAVA).ifPresent(dependency -> PomUtil.getInstance()
                .addDependency(dependency, APPLICATION));
        } catch (InterruptedException | IOException | URISyntaxException e) {
            log.error(e.getMessage(), e);
        }
    }

    private static class ApplicationModuleHandlerHolder {

        private static final ApplicationModuleHandler INSTANCE = new ApplicationModuleHandler();
    }
}
