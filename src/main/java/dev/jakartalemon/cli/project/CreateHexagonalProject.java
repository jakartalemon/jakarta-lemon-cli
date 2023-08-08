package dev.jakartalemon.cli.project;

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
import dev.jakartalemon.cli.model.PomModel;
import static dev.jakartalemon.cli.util.Constants.APPLICATION;
import static dev.jakartalemon.cli.util.Constants.ARTIFACT_ID;
import static dev.jakartalemon.cli.util.Constants.DOMAIN;
import static dev.jakartalemon.cli.util.Constants.DTO;
import static dev.jakartalemon.cli.util.Constants.GROUP_ID;
import static dev.jakartalemon.cli.util.Constants.INFRASTRUCTURE;
import static dev.jakartalemon.cli.util.Constants.JAR;
import static dev.jakartalemon.cli.util.Constants.JAVA_VERSION;
import static dev.jakartalemon.cli.util.Constants.MAPPER;
import static dev.jakartalemon.cli.util.Constants.MAVEN_COMPILER_RELEASE;
import static dev.jakartalemon.cli.util.Constants.POM;
import static dev.jakartalemon.cli.util.Constants.PORTS;
import static dev.jakartalemon.cli.util.Constants.PROJECT_GROUP_ID;
import static dev.jakartalemon.cli.util.Constants.PROJECT_VERSION;
import static dev.jakartalemon.cli.util.Constants.REPOSITORY;
import static dev.jakartalemon.cli.util.Constants.SERVICE;
import static dev.jakartalemon.cli.util.Constants.VERSION;
import dev.jakartalemon.cli.util.PomUtil;
import jakarta.json.JsonObject;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

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
        PomUtil.getInstance().createPom(projectPath, projectPom.build()
        );

        createDomainModule(projectPath, groupId, artifactId, version, packageName);
        createApplicationModule(projectPath, groupId, artifactId, version, packageName);
        createInfrastructureModule(projectPath, groupId, artifactId, version, packageName);
        return Optional.empty();
    }

    private void createDomainModule(Path projectPath,
        String groupId,
        String artifactId,
        String version,
        String packageName) {

        var modulePom = PomModel.builder().parent(Map.of(
            GROUP_ID, groupId,
            ARTIFACT_ID, artifactId,
            VERSION, version
        )).packaging(JAR).dependencies(List.of(
            Map.of(
                GROUP_ID, "org.projectlombok",
                ARTIFACT_ID, "lombok",
                VERSION, "${org.projectlombok.version}"
            )
        )).properties(Map.of(
            MAVEN_COMPILER_RELEASE, JAVA_VERSION
        )).artifactId(DOMAIN);

        var pomPath = PomUtil.getInstance().createPom(
            projectPath.resolve(DOMAIN), modulePom.build()
        );
        pomPath.ifPresent(pom -> {
            log.debug("domain created at {}", pom);
            var parent = pom.getParent();
            PomUtil.getInstance().createJavaProjectStructure(parent, packageName + ".domain.dao");
            PomUtil.getInstance().createJavaProjectStructure(parent, packageName + ".domain.model");
            PomUtil.getInstance().
                createJavaProjectStructure(parent, packageName + ".domain.service");
        });
    }

    private void createApplicationModule(Path projectPath,
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
            .modules(List.of(REPOSITORY, SERVICE));
        var pomPath = PomUtil.getInstance().createPom(projectPath.resolve(APPLICATION),
            modulePom.build());
        pomPath.ifPresent(pom -> {
            log.debug("application created at {}", pom.toAbsolutePath());
            createApplicationRepositoryModule(pom.getParent(), groupId, artifactId, version,
                packageName);
            createApplicationServiceModule(pom.getParent(), groupId, artifactId, version,
                packageName);
        });
    }

    private void createApplicationRepositoryModule(Path projectPath,
        String groupId,
        String artifactId,
        String version,
        String packageName) {
        var modulePom = PomModel.builder()
            .parent(Map.of(GROUP_ID, groupId,
                ARTIFACT_ID, artifactId,
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
        String artifactId,
        String version,
        String packageName) {
        var modulePom = PomModel.builder()
            .parent(Map.of(GROUP_ID, groupId,
                ARTIFACT_ID, artifactId,
                VERSION, version
            ))
            .artifactId(SERVICE)
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
                Map.of(
                    GROUP_ID, "jakarta.inject",
                    ARTIFACT_ID, "jakarta.inject-api",
                    VERSION, "2.0.1",
                    "scope", "provided"
                )
            ))
            .properties(
                Map.of(MAVEN_COMPILER_RELEASE, JAVA_VERSION)
            );
        var pomPath = PomUtil.getInstance().createPom(projectPath.resolve(SERVICE),
            modulePom.build());

        pomPath.ifPresent(pom -> {
            log.debug("repository created at {}", pom.toAbsolutePath());
            PomUtil.getInstance().createJavaProjectStructure(pom.getParent(), packageName);
        });
    }

    private void createInfrastructureModule(Path projectPath,
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
                    DTO, MAPPER, PORTS
                )
            );
        var pomPath = PomUtil.getInstance().createPom(projectPath.resolve(INFRASTRUCTURE),
            modulePom.build());

        pomPath.ifPresent(pom -> {
            log.debug("infrastructure created at {}", pom.toAbsolutePath());
            createDtoInfrastructureModule(pom.getParent(), groupId, version,
                packageName);
            createMapperInfrastructureModule(pom.getParent(), groupId, version,
                packageName);
            createPortsInfrastructureModule(pom.getParent(), groupId, version,
                packageName);
        });
    }

    private void createDtoInfrastructureModule(Path projectPath,
        String groupId,
        String version,
        String packageName) {
        var modulePom = PomModel.builder()
            .parent(Map.of(GROUP_ID, groupId,
                ARTIFACT_ID, INFRASTRUCTURE,
                VERSION, version
            ))
            .artifactId(DTO)
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
        var pomPath = PomUtil.getInstance().createPom(projectPath.resolve(DTO),
            modulePom.build());
        pomPath.ifPresent(pom -> {
            log.debug("dto created at {}", pom.toAbsolutePath());
            PomUtil.getInstance().createJavaProjectStructure(pom.getParent(), "%s.%s.dto".formatted(
                packageName, INFRASTRUCTURE));
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
                        GROUP_ID, "org.mapstruct",
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
                        ARTIFACT_ID, DTO,
                        VERSION, PROJECT_VERSION
                    ),
                    Map.of(
                        GROUP_ID, "org.mockito",
                        ARTIFACT_ID, "mockito-junit-jupiter",
                        VERSION, "${mockito.junit.jupiter.version}",
                        "scope", "test"
                    )
                )
            ).properties(
                Map.of(MAVEN_COMPILER_RELEASE, JAVA_VERSION)
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

    private void createPortsInfrastructureModule(Path projectPath,
        String groupId,
        String version,
        String packageName) {
        var modulePom = PomModel.builder()
            .parent(Map.of(GROUP_ID, groupId,
                ARTIFACT_ID, INFRASTRUCTURE,
                VERSION, version
            ))
            .artifactId(PORTS)
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
        var pomPath = PomUtil.getInstance().createPom(projectPath.resolve(PORTS),
            modulePom.build());
        pomPath.ifPresent(pom -> {
            log.debug("ports created at {}", pom.toAbsolutePath());
            PomUtil.getInstance()
                .createJavaProjectStructure(pom.getParent(), "%s.%s.ports".formatted(
                    packageName, INFRASTRUCTURE));
        });
    }

    private static class CreateHexagonalProjectHolder {

        private static final CreateHexagonalProject INSTANCE = new CreateHexagonalProject();
    }
}
