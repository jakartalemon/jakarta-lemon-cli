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
import dev.jakartalemon.cli.util.DependenciesUtil;
import dev.jakartalemon.cli.util.PomUtil;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static dev.jakartalemon.cli.util.Constants.*;
import static dev.jakartalemon.cli.util.Constants.Archetype.HEXA;

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

    public static Optional<JsonObject> createProject(Path projectPath,
                                                     String groupId,
                                                     String artifactId,
                                                     String packageName)
        throws IOException, URISyntaxException, InterruptedException {

        var lombokVersion = DependenciesUtil.getLastVersionDependency(MAVEN_QUERY_LOMBOK)
            .map(lombokDependency -> lombokDependency.getString(VERSION)).orElse("1.18.30");

        var version = "1.0-SNAPSHOT";
        var projectPom = PomModel.builder().groupId(groupId).artifactId(artifactId).version(version)
            .packaging(POM)
            .modules(List.of(DOMAIN, APPLICATION, INFRASTRUCTURE))
            .properties(Map.of("project.build.sourceEncoding", "UTF-8",
                MAVEN_COMPILER_RELEASE, JAVA_VERSION,
                MOCKITO_JUNIT_JUPITER_VERSION_KEY, MOCKITO_JUNIT_JUPITER_VERSION,
                ORG_MAPSTRUCT_VERSION_KEY, "1.5.5.Final",
                JAKARTA_CDI_API_VERSION_KEY, "4.0.1",
                JAKARTA_ANNOTATION_API_VERSION_KEY, "2.1.0",
                JAKARTA_WS_RS_API_VERSION_KEY, JAKARTA_WS_RS_API_VERSION,
                JAKARTA_SERVLET_VERSION_KEY, "6.0.0",
                PROJECT_LOMBOK_VERSION_KEY, lombokVersion,
                JAKARTA_PERSISTENCE_API_VERSION_KEY,JAKARTA_PERSISTENCE_API_VERSION
            ));
        PomUtil.getInstance().createPom(projectPath, projectPom.build());

        var domainPath = DomainModuleHandler.createDomainModule(projectPath, groupId,
            artifactId, version,
            packageName);
        var appPath = ApplicationModuleHandler.getInstance().createApplicationModule(projectPath,
            groupId,
            artifactId,
            version);

        var infraPath = InfrastructureModuleHandler.
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
