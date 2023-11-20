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
package dev.jakartalemon.cli.project;

import dev.jakartalemon.cli.JakartaLemonCli;
import dev.jakartalemon.cli.project.hexa.CreateHexagonalProject;
import dev.jakartalemon.cli.util.Constants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import picocli.CommandLine;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

import static dev.jakartalemon.cli.util.Constants.PROJECT_INFO_JSON;

/**
 * Subcommand to create projects
 *
 * @author Diego Silva <diego.silva at apuntesdejava.com>
 */
@CommandLine.Command(
    name = "createproject",
    resourceBundle = "messages",
    description = "Create Jakarta EE projects using some pattern indicated in the parameters."
)
@Slf4j
public class CreateProjectCommand implements Runnable {

    @CommandLine.Parameters(
        index = "0",
        descriptionKey = "project.name"
    )
    private String projectName;

    @CommandLine.Option(
        names = {"-a", "--arch"},
        defaultValue = "MVC",
        descriptionKey = "project.arch"
    )
    private String archetypeOption;

    @CommandLine.Option(
        names = {"-g", "--groupId"},
        descriptionKey = "project.groupId"
    )
    private String groupId;

    @CommandLine.Option(
        names = {"-p", "--package"},
        descriptionKey = "project.package"
    )
    private String packageName;

    @CommandLine.Option(
        names = {"-v", "--verbose"},
        descriptionKey = "options.verbose"
    )
    private boolean verbose;

    @CommandLine.Option(
        names = {"-i", "--artifactId"},
        descriptionKey = "project.artifactId"
    )
    private String artifactId;

    @CommandLine.ParentCommand
    private JakartaLemonCli jakartaLemonCli;

    /**
     * Main method with which the subcommand is executed
     */
    @Override
    public void run() {
        try {
            var projectPath = Path.of(projectName);
            var projectInfoPath = projectPath.resolve(PROJECT_INFO_JSON);
            var created = Files.createDirectories(projectPath);
            if (verbose) {
                log.info("{} created", created);
            }
            var archetype = Constants.Archetype.valueOf(archetypeOption.toUpperCase());
            if (StringUtils.isBlank(packageName)) {
                packageName = groupId + '.' + artifactId;
            }
            switch (archetype) {
                case HEXA ->
                    CreateHexagonalProject.getInstance()
                        .createProject(created, groupId, artifactId, packageName).ifPresent(
                        projectInfo -> {
                            try {
                                Files.writeString(projectInfoPath, projectInfo.toString());
                            } catch (IOException ex) {
                                log.error(ex.getMessage(), ex);
                            }
                        });
                case JSF -> {
                }
                case MVC -> {
                }
                case REST -> {
                }
            }
        } catch (IOException | URISyntaxException | InterruptedException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

}
