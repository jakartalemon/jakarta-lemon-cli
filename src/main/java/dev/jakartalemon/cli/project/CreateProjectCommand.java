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
import dev.jakartalemon.cli.project.constants.Archetype;
import jakarta.json.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import picocli.CommandLine;

/**
 *
 * @author Diego Silva <diego.silva at apuntesdejava.com>
 */
@CommandLine.Command(
    name = "createproject",
    resourceBundle = "messages",
    description = "Create Jakarta EE projects using some pattern indicated in the parameters."
)
public class CreateProjectCommand implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(CreateProjectCommand.class.getName());

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

    @Override
    public void run() {
        try {

            var projectPath = Path.of(projectName);
            var created = Files.createDirectories(projectPath);
            if (verbose) {
                LOGGER.info(String.format("%s created", created));
            }
            var archetype = Archetype.valueOf(archetypeOption.toUpperCase());
            switch (archetype) {
                case HEXA:
                    var projectInfo = CreateHexagonalProject.getInstance()
                        .createProject(created, groupId, artifactId);
                    break;
                default:
                    throw new AssertionError();
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

}
