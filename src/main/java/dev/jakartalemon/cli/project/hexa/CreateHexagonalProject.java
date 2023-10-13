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

import dev.jakartalemon.cli.project.hexa.handler.ApplicationModuleHandler;
import dev.jakartalemon.cli.project.hexa.handler.DomainModuleHandler;
import dev.jakartalemon.cli.project.hexa.handler.InfrastructureModuleHandler;
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
import static dev.jakartalemon.cli.util.Constants.APPLICATION;
import static dev.jakartalemon.cli.util.Constants.ARTIFACT_ID;
import static dev.jakartalemon.cli.util.Constants.DOMAIN;
import static dev.jakartalemon.cli.util.Constants.GROUP_ID;
import static dev.jakartalemon.cli.util.Constants.INFRASTRUCTURE;
import static dev.jakartalemon.cli.util.Constants.JAKARTA_ANOTATION_API_VERSION_KEY;
import static dev.jakartalemon.cli.util.Constants.JAKARTA_CDI_API_VERSION_KEY;
import static dev.jakartalemon.cli.util.Constants.JAKARTA_SERVLET_VERSION_KEY;
import static dev.jakartalemon.cli.util.Constants.JAKARTA_WS_RS_API_VERSION_KEY;
import static dev.jakartalemon.cli.util.Constants.JAVA_VERSION;
import static dev.jakartalemon.cli.util.Constants.MAVEN_COMPILER_RELEASE;
import static dev.jakartalemon.cli.util.Constants.PACKAGE;
import static dev.jakartalemon.cli.util.Constants.POM;

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
                "org.mapstruct.version", "1.5.5.Final",
                JAKARTA_CDI_API_VERSION_KEY, "4.0.1",
                JAKARTA_ANOTATION_API_VERSION_KEY, "2.1.0",
                JAKARTA_WS_RS_API_VERSION_KEY, "3.1.0",
                JAKARTA_SERVLET_VERSION_KEY, "6.0.0"
            ));
        PomUtil.getInstance().createPom(projectPath, projectPom.build());

        var domainPath = DomainModuleHandler.getInstance().createDomainModule(projectPath, groupId,
            artifactId, version, packageName);
        var appPath = ApplicationModuleHandler.getInstance().createApplicationModule(projectPath,
            groupId, artifactId, version);

        var infraPath = InfrastructureModuleHandler.getInstance().
            createInfrastructureModule(projectPath,
                groupId, artifactId, version,
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

}
