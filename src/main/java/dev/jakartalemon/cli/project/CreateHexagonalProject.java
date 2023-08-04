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
        String artifactId) {
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

        createDomainModule(projectPath, groupId, artifactId, version);
        createApplicationModule(projectPath, groupId, artifactId, version);
        return Optional.empty();
    }

    private void createDomainModule(Path projectPath,
        String groupId,
        String artifactId,
        String version) {

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
        log.debug("domain created at {}", pomPath.orElse(null));
    }

    private void createApplicationModule(Path projectPath,
        String groupId,
        String artifactId,
        String version) {
        var modulePom = PomModel.builder()
            .parent(Map.of(
                "groupId", groupId,
                "artifactId", artifactId,
                "version", version)
            ).artifactId(artifactId)
            .packaging("pom")
            .modules(List.of("repository", "service"));
        var pomPath = PomUtil.getInstance().createPom(projectPath.resolve("application"),
            modulePom.build());
        log.debug("domain created at {}", pomPath.orElse(null));
    }

    private static class CreateHexagonalProjectHolder {

        private static final CreateHexagonalProject INSTANCE = new CreateHexagonalProject();
    }
}
