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
            .packaging("pom")
            .modules(List.of("domain", "application", "infrastructure"))
            .properties(Map.of(
                "project.build.sourceEncoding", "UTF-8",
                "maven.compiler.release", "17",
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
            "groupId", groupId,
            "artifactId", artifactId,
            "version", version
        )).packaging("jar").dependencies(List.of(
            Map.of(
                "groupId", "org.projectlombok",
                "artifactId", "lombok",
                "version", "${org.projectlombok.version}"
            )
        )).properties(Map.of(
            "maven.compiler.release", "17"
        )).artifactId("domain");

        var pomPath = PomUtil.getInstance().createPom(
            projectPath.resolve("domain"), modulePom.build()
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
                "groupId", groupId,
                "artifactId", artifactId,
                "version", version)
            ).artifactId("application")
            .packaging("pom")
            .modules(List.of("repository", "service"));
        var pomPath = PomUtil.getInstance().createPom(projectPath.resolve("application"),
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
            .parent(Map.of("groupId", groupId,
                "artifactId", artifactId,
                "version", version
            ))
            .artifactId("repository")
            .packaging("jar")
            .dependencies(List.of(
                Map.of(
                    "groupId", "${project.groupId}",
                    "artifactId", "domain",
                    "version", "${project.version}"
                )
            ))
            .properties(
                Map.of("maven.compiler.release", "17")
            );
        var pomPath = PomUtil.getInstance().createPom(projectPath.resolve("repository"),
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
            .parent(Map.of("groupId", groupId,
                "artifactId", artifactId,
                "version", version
            ))
            .artifactId("service")
            .packaging("jar")
            .dependencies(List.of(
                Map.of(
                    "groupId", "${project.groupId}",
                    "artifactId", "domain",
                    "version", "${project.version}"
                ),
                Map.of(
                    "groupId", "${project.groupId}",
                    "artifactId", "repository",
                    "version", "${project.version}"
                ),
                Map.of(
                    "groupId", "jakarta.inject",
                    "artifactId", "jakarta.inject-api",
                    "version", "2.0.1",
                    "scope", "provided"
                )
            ))
            .properties(
                Map.of("maven.compiler.release", "17")
            );
        var pomPath = PomUtil.getInstance().createPom(projectPath.resolve("service"),
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
            .parent(Map.of("groupId", groupId,
                "artifactId", artifactId,
                "version", version
            ))
            .artifactId("infrastructure")
            .packaging("pom")
            .modules(
                List.of(
                    "dto", "mapper", "ports"
                )
            );
        var pomPath = PomUtil.getInstance().createPom(projectPath.resolve("infrastructure"),
            modulePom.build());

        pomPath.ifPresent(pom -> {
            log.debug("infrastructure created at {}", pom.toAbsolutePath());
            createDtoInfrastructureModule(pom.getParent(), groupId, "infrastructure", version,
                packageName);
            createMapperInfrastructureModule(pom.getParent(), groupId, "infrastructure", version,
                packageName);
            createPortsInfrastructureModule(pom.getParent(), groupId, "infrastructure", version,
                packageName);
        });
    }

    private void createDtoInfrastructureModule(Path projectPath,
        String groupId,
        String artifactId,
        String version,
        String packageName) {
        var modulePom = PomModel.builder()
            .parent(Map.of("groupId", groupId,
                "artifactId", artifactId,
                "version", version
            ))
            .artifactId("dto")
            .packaging("jar")
            .dependencies(
                List.of(
                    Map.of(
                        "groupId", "org.projectlombok",
                        "artifactId", "lombok",
                        "version", "${org.projectlombok.version}"
                    )
                )
            ).properties(
                Map.of("maven.compiler.release", "17")
            );
        var pomPath = PomUtil.getInstance().createPom(projectPath.resolve("dto"),
            modulePom.build());
        pomPath.ifPresent(pom -> {
            log.debug("dto created at {}", pom.toAbsolutePath());
            PomUtil.getInstance().createJavaProjectStructure(pom.getParent(), "%s.%s.dto".formatted(
                packageName, artifactId));
        });
    }

    private void createMapperInfrastructureModule(Path projectPath,
        String groupId,
        String artifactId,
        String version,
        String packageName) {
        var modulePom = PomModel.builder()
            .parent(Map.of("groupId", groupId,
                "artifactId", artifactId,
                "version", version
            ))
            .artifactId("mapper")
            .packaging("jar")
            .dependencies(
                List.of(
                    Map.of(
                        "groupId", "org.mapstruct",
                        "artifactId", "mapstruct",
                        "version", "${org.mapstruct.version}"
                    ),
                    Map.of(
                        "groupId", "${project.groupId}",
                        "artifactId", "domain",
                        "version", "${project.version}"
                    ),
                    Map.of(
                        "groupId", "${project.groupId}",
                        "artifactId", "dto",
                        "version", "${project.version}"
                    ),
                    Map.of(
                        "groupId", "org.mockito",
                        "artifactId", "mockito-junit-jupiter",
                        "version", "${mockito.junit.jupiter.version}",
                        "scope","test"
                    )
                )
            ).properties(
                Map.of("maven.compiler.release", "17")
            );
        var pomPath = PomUtil.getInstance().createPom(projectPath.resolve("mapper"),
            modulePom.build());
        pomPath.ifPresent(pom -> {
            log.debug("mapper created at {}", pom.toAbsolutePath());
            PomUtil.getInstance().createJavaProjectStructure(pom.getParent(), "%s.%s.mapper".formatted(
                packageName, artifactId));
        });
    }

    private void createPortsInfrastructureModule(Path projectPath,
        String groupId,
        String artifactId,
        String version,
        String packageName) {
        var modulePom = PomModel.builder()
            .parent(Map.of("groupId", groupId,
                "artifactId", artifactId,
                "version", version
            ))
            .artifactId("ports")
            .packaging("jar")
            .dependencies(
                List.of(
                    Map.of(
                        "groupId", "org.projectlombok",
                        "artifactId", "lombok",
                        "version", "${org.projectlombok.version}"
                    )
                )
            ).properties(
                Map.of("maven.compiler.release", "17")
            );
        var pomPath = PomUtil.getInstance().createPom(projectPath.resolve("ports"),
            modulePom.build());
        pomPath.ifPresent(pom -> {
            log.debug("ports created at {}", pom.toAbsolutePath());
            PomUtil.getInstance().createJavaProjectStructure(pom.getParent(), "%s.%s.ports".formatted(
                packageName, artifactId));
        });
    }

    private static class CreateHexagonalProjectHolder {

        private static final CreateHexagonalProject INSTANCE = new CreateHexagonalProject();
    }
}
