/*
 * Copyright 2023 Diego Silva <diego.silva at apuntesdejava.com>.
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

import dev.jakartalemon.cli.model.BuildModel;
import dev.jakartalemon.cli.model.PomModel;
import dev.jakartalemon.cli.util.PomUtil;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static dev.jakartalemon.cli.project.constants.Archetype.HEXA;
import static dev.jakartalemon.cli.util.Constants.ADAPTERS;
import static dev.jakartalemon.cli.util.Constants.APPLICATION;
import static dev.jakartalemon.cli.util.Constants.ARTIFACT_ID;
import static dev.jakartalemon.cli.util.Constants.DOMAIN;
import static dev.jakartalemon.cli.util.Constants.ENTITIES;
import static dev.jakartalemon.cli.util.Constants.GROUP_ID;
import static dev.jakartalemon.cli.util.Constants.INFRASTRUCTURE;
import static dev.jakartalemon.cli.util.Constants.JAKARTA_INJECT_DEPENDENCY;
import static dev.jakartalemon.cli.util.Constants.JAR;
import static dev.jakartalemon.cli.util.Constants.JAVA_VERSION;
import static dev.jakartalemon.cli.util.Constants.LOMBOK_DEPENDENCY;
import static dev.jakartalemon.cli.util.Constants.MAPPER;
import static dev.jakartalemon.cli.util.Constants.MAVEN_COMPILER_RELEASE;
import static dev.jakartalemon.cli.util.Constants.MOCKITO_DEPENDENCY;
import static dev.jakartalemon.cli.util.Constants.MODEL;
import static dev.jakartalemon.cli.util.Constants.ORG_MAPSTRUCT;
import static dev.jakartalemon.cli.util.Constants.PACKAGE;
import static dev.jakartalemon.cli.util.Constants.PACKAGE_TEMPLATE;
import static dev.jakartalemon.cli.util.Constants.POM;
import static dev.jakartalemon.cli.util.Constants.PROJECT_GROUP_ID;
import static dev.jakartalemon.cli.util.Constants.PROJECT_VERSION;
import static dev.jakartalemon.cli.util.Constants.REPOSITORY;
import static dev.jakartalemon.cli.util.Constants.USECASE;
import static dev.jakartalemon.cli.util.Constants.VERSION;

/**
 * @author Diego Silva <diego.silva at apuntesdejava.com>
 */
@Slf4j
public class CreateHexagonalProject {

    private CreateHexagonalProject() {
    }

    public static CreateHexagonalProject getInstance() {
        return CreateHexagonalProjectHolder.INSTANCE;
    }

    private static class CreateHexagonalProjectHolder {

        private static final CreateHexagonalProject INSTANCE = new CreateHexagonalProject();
    }

    public Optional<JsonObject> createProject(Path projectPath,
        String groupId,
        String artifactId,
        String packageName) {
        var version = "1.0-SNAPSHOT";
        var projectPom = PomModel.builder().groupId(groupId).artifactId(artifactId).version(version)
            .packaging(POM)
            .modules(List.of(DOMAIN, APPLICATION, INFRASTRUCTURE))
            .properties(Map.of(
                "project.build.sourceEncoding", "UTF-8",
                MAVEN_COMPILER_RELEASE, JAVA_VERSION,
                "mockito.junit.jupiter.version", "5.4.0",
                "org.projectlombok.version", "1.18.28",
                "org.mapstruct.version", "1.5.5.Final"
            ));
        PomUtil.getInstance().createPom(projectPath, projectPom.build());

        var domainPath = createDomainModule(projectPath, groupId, artifactId, version, packageName);
        var appPath
            = createApplicationModule(projectPath, groupId, artifactId, version, packageName);
        var infraPath = createInfrastructureModule(projectPath, groupId, artifactId, version,
            packageName);
        var projectInfo = Json.createObjectBuilder()
            .add("archetype", HEXA.name())
            .add(GROUP_ID, groupId)
            .add(ARTIFACT_ID, artifactId)
            .add(PACKAGE, packageName);
        domainPath.ifPresent(domain -> projectInfo.add(DOMAIN, domain.getParent().toAbsolutePath().
            toString()));
        appPath.ifPresent(app -> projectInfo.add(APPLICATION, app.getParent().toAbsolutePath().
            toString()));
        infraPath.ifPresent(
            infra -> projectInfo.add(INFRASTRUCTURE,
                infra.getParent().toAbsolutePath().toString()));
        return Optional.of(projectInfo.build());
    }

    private Optional<Path> createDomainModule(Path projectPath,
        String groupId,
        String artifactId,
        String version,
        String packageName) {

        var modulePom = PomModel.builder().parent(Map.of(
            GROUP_ID, groupId,
            ARTIFACT_ID, artifactId,
            VERSION, version
        )).packaging(JAR).dependencies(List.of(
            LOMBOK_DEPENDENCY,MOCKITO_DEPENDENCY
        )).properties(Map.of(
            MAVEN_COMPILER_RELEASE, JAVA_VERSION
        )).artifactId(DOMAIN);

        var pomPath = PomUtil.getInstance().createPom(
            projectPath.resolve(DOMAIN), modulePom.build()
        );
        pomPath.ifPresent(pom -> {
            log.debug("domain created at {}", pom);
            var parent = pom.getParent();
            PomUtil.getInstance().createJavaProjectStructure(parent, PACKAGE_TEMPLATE.formatted(
                packageName, DOMAIN, REPOSITORY));
            PomUtil.getInstance()
                .createJavaProjectStructure(parent,
                    PACKAGE_TEMPLATE.formatted(packageName, DOMAIN, MODEL));
            PomUtil.getInstance().
                createJavaProjectStructure(parent,
                    PACKAGE_TEMPLATE.formatted(packageName ,DOMAIN,USECASE));
        });
        return pomPath;
    }

    private Optional<Path> createApplicationModule(Path projectPath,
        String groupId,
        String artifactId,
        String version,
        String packageName) {
        var modulePom = PomModel.builder()
            .parent(Map.of(
                GROUP_ID, groupId,
                ARTIFACT_ID, artifactId,
                VERSION, version)
            ).artifactId(APPLICATION)
            .packaging(POM)
            .modules(List.of(REPOSITORY, USECASE));
        var pomPath = PomUtil.getInstance().createPom(projectPath.resolve(APPLICATION),
            modulePom.build());
        pomPath.ifPresent(pom -> {
            log.debug("application created at {}", pom.toAbsolutePath());
            createApplicationRepositoryModule(pom.getParent(), groupId, version,
                packageName);
            createApplicationServiceModule(pom.getParent(), groupId, version,
                packageName);
        });
        return pomPath;
    }

    private void createApplicationRepositoryModule(Path projectPath,
        String groupId,
        String version,
        String packageName) {
        var modulePom = PomModel.builder()
            .parent(Map.of(GROUP_ID, groupId,
                ARTIFACT_ID, APPLICATION,
                VERSION, version
            ))
            .artifactId(REPOSITORY)
            .packaging(JAR)
            .dependencies(List.of(
                Map.of(
                    GROUP_ID, PROJECT_GROUP_ID,
                    ARTIFACT_ID, DOMAIN,
                    VERSION, PROJECT_VERSION
                )
            ))
            .properties(
                Map.of(MAVEN_COMPILER_RELEASE, JAVA_VERSION)
            );
        var pomPath = PomUtil.getInstance().createPom(projectPath.resolve(REPOSITORY),
            modulePom.build());

        pomPath.ifPresent(pom -> {
            log.debug("repository created at {}", pom.toAbsolutePath());
            PomUtil.getInstance().createJavaProjectStructure(pom.getParent(), packageName);
        });
    }

    private void createApplicationServiceModule(Path projectPath,
        String groupId,
        String version,
        String packageName) {
        var modulePom = PomModel.builder()
            .parent(Map.of(GROUP_ID, groupId,
                ARTIFACT_ID, APPLICATION,
                VERSION, version
            ))
            .artifactId(USECASE)
            .packaging(JAR)
            .dependencies(List.of(
                Map.of(
                    GROUP_ID, PROJECT_GROUP_ID,
                    ARTIFACT_ID, DOMAIN,
                    VERSION, PROJECT_VERSION
                ),
                Map.of(
                    GROUP_ID, PROJECT_GROUP_ID,
                    ARTIFACT_ID, REPOSITORY,
                    VERSION, PROJECT_VERSION
                ),
                JAKARTA_INJECT_DEPENDENCY
            ))
            .properties(
                Map.of(MAVEN_COMPILER_RELEASE, JAVA_VERSION)
            );
        var pomPath = PomUtil.getInstance().createPom(projectPath.resolve(USECASE),
            modulePom.build());

        pomPath.ifPresent(pom -> {
            log.debug("repository created at {}", pom.toAbsolutePath());
            PomUtil.getInstance().createJavaProjectStructure(pom.getParent(), packageName);
        });
    }

    private Optional<Path> createInfrastructureModule(Path projectPath,
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
            .packaging(POM)
            .modules(
                List.of(
                    ENTITIES, MAPPER, ADAPTERS
                )
            );
        var pomPath = PomUtil.getInstance().createPom(projectPath.resolve(INFRASTRUCTURE),
            modulePom.build());

        pomPath.ifPresent(pom -> {
            log.debug("infrastructure created at {}", pom.toAbsolutePath());
            createEntityInfrastructureModule(pom.getParent(), groupId, version,
                packageName);
            createMapperInfrastructureModule(pom.getParent(), groupId, version,
                packageName);
            createAdaptersInfrastructureModule(pom.getParent(), groupId, version,
                packageName);
        });
        return pomPath;
    }

    private void createEntityInfrastructureModule(Path projectPath,
        String groupId,
        String version,
        String packageName) {
        var modulePom = PomModel.builder()
            .parent(Map.of(GROUP_ID, groupId,
                ARTIFACT_ID, INFRASTRUCTURE,
                VERSION, version
            ))
            .artifactId(ENTITIES)
            .packaging(JAR)
            .dependencies(
                List.of(
                    Map.of(
                        GROUP_ID, "org.projectlombok",
                        ARTIFACT_ID, "lombok",
                        VERSION, "${org.projectlombok.version}"
                    )
                )
            ).properties(
                Map.of(MAVEN_COMPILER_RELEASE, JAVA_VERSION)
            );
        var pomPath = PomUtil.getInstance().createPom(projectPath.resolve(ENTITIES),
            modulePom.build());
        pomPath.ifPresent(pom -> {
            log.debug("entities created at {}", pom.toAbsolutePath());
            PomUtil.getInstance()
                .createJavaProjectStructure(pom.getParent(), PACKAGE_TEMPLATE.formatted(
                    packageName, INFRASTRUCTURE, ENTITIES));
        });
    }

    private void createMapperInfrastructureModule(Path projectPath,
        String groupId,
        String version,
        String packageName) {
        var modulePom = PomModel.builder()
            .parent(Map.of(GROUP_ID, groupId,
                ARTIFACT_ID, INFRASTRUCTURE,
                VERSION, version
            ))
            .artifactId(MAPPER)
            .packaging(JAR)
            .dependencies(
                List.of(
                    Map.of(
                        GROUP_ID, ORG_MAPSTRUCT,
                        ARTIFACT_ID, "mapstruct",
                        VERSION, "${org.mapstruct.version}"
                    ),
                    Map.of(
                        GROUP_ID, PROJECT_GROUP_ID,
                        ARTIFACT_ID, DOMAIN,
                        VERSION, PROJECT_VERSION
                    ),
                    Map.of(
                        GROUP_ID, PROJECT_GROUP_ID,
                        ARTIFACT_ID, ENTITIES,
                        VERSION, PROJECT_VERSION
                    ),
                    MOCKITO_DEPENDENCY
                )
            ).properties(
                Map.of(MAVEN_COMPILER_RELEASE, JAVA_VERSION)
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
                    ).build()
            );
        var pomPath = PomUtil.getInstance().createPom(projectPath.resolve(MAPPER),
            modulePom.build());
        pomPath.ifPresent(pom -> {
            log.debug("mapper created at {}", pom.toAbsolutePath());
            PomUtil.getInstance()
                .createJavaProjectStructure(pom.getParent(), "%s.%s.mapper".formatted(
                    packageName, INFRASTRUCTURE));
        });
    }

    private void createAdaptersInfrastructureModule(Path projectPath,
        String groupId,
        String version,
        String packageName) {
        var modulePom = PomModel.builder()
            .parent(Map.of(GROUP_ID, groupId,
                ARTIFACT_ID, INFRASTRUCTURE,
                VERSION, version
            ))
            .artifactId(ADAPTERS)
            .packaging(JAR)
            .dependencies(
                List.of(
                    LOMBOK_DEPENDENCY,
                    Map.of(
                        GROUP_ID, PROJECT_GROUP_ID,
                        ARTIFACT_ID, DOMAIN,
                        VERSION, PROJECT_VERSION
                    ),
                    Map.of(
                        GROUP_ID, PROJECT_GROUP_ID,
                        ARTIFACT_ID, USECASE,
                        VERSION, PROJECT_VERSION
                    ),
                    Map.of(
                        GROUP_ID, PROJECT_GROUP_ID,
                        ARTIFACT_ID, ENTITIES,
                        VERSION, PROJECT_VERSION
                    ),
                    Map.of(
                        GROUP_ID, PROJECT_GROUP_ID,
                        ARTIFACT_ID, MAPPER,
                        VERSION, PROJECT_VERSION
                    )
                )
            ).properties(
                Map.of(MAVEN_COMPILER_RELEASE, JAVA_VERSION)
            );
        var pomPath = PomUtil.getInstance().createPom(projectPath.resolve(ADAPTERS),
            modulePom.build());
        pomPath.ifPresent(pom -> {
            log.debug("adapters created at {}", pom.toAbsolutePath());
            PomUtil.getInstance()
                .createJavaProjectStructure(pom.getParent(), PACKAGE_TEMPLATE.formatted(
                    packageName, INFRASTRUCTURE, ADAPTERS));
        });
    }

}
