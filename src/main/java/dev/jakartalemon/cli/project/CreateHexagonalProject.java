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
import dev.jakartalemon.cli.util.PomUtil;
import jakarta.json.JsonObject;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * @author Diego Silva <diego.silva at apuntesdejava.com>
 */
public class CreateHexagonalProject {

    private static final Logger LOGGER = Logger.getLogger(CreateHexagonalProject.class.getName());

    private CreateHexagonalProject() {
    }

    public static CreateHexagonalProject getInstance() {
        return CreateHexagonalProjectHolder.INSTANCE;
    }

    public Optional<JsonObject> createProject(Path projectPath, String groupId, String artifactId) {
        var version = "1.0-SNAPSHOT";
        PomUtil.getInstance().createPom(projectPath,
            List.of("domain", "application", "infrastructure"),
            null, groupId,
            artifactId, version,
            "pom",
            null,
            Map.of(
                "project.build.sourceEncoding", "UTF-8",
                "maven.compiler.release", "17",
                "mockito.junit.jupiter.version", "5.4.0",
                "org.projectlombok.version", "1.18.28",
                "org.mapstruct.version", "1.5.5.Final"
            )
        );

        createDomainModule(projectPath.resolve("domain"), groupId, artifactId, version);
        return Optional.empty();
    }

    private void createDomainModule(Path modulePath, String groupId, String artifactId,
        String version) {
        PomUtil.getInstance().createPom(
            modulePath,
            Map.of(
                "groupId", groupId,
                "artifactId", artifactId,
                "version", version
            ),
            "domain",
            "jar",
            List.of(
                Map.of(
                    "groupId", "org.projectlombok",
                    "artifactId", "lombok",
                    "version", "${org.projectlombok.version}"
                )
            ),
            Map.of(
                "maven.compiler.release", "17"
            )
        );
    }

    private static class CreateHexagonalProjectHolder {

        private static final CreateHexagonalProject INSTANCE = new CreateHexagonalProject();
    }
}
