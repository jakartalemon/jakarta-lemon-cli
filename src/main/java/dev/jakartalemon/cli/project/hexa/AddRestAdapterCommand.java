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
package dev.jakartalemon.cli.project.hexa;

import dev.jakartalemon.cli.project.hexa.handler.ApplicationModuleHandler;
import dev.jakartalemon.cli.project.hexa.handler.RestAdapterHandler;

import java.io.File;

import dev.jakartalemon.cli.util.JsonFileUtil;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

/**
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
@CommandLine.Command(
    name = "addrestadapter",
    resourceBundle = "messages",
    description = "Create a REST adapter based on Jakarta EE"
)
@Slf4j
public class AddRestAdapterCommand implements Runnable {

    @CommandLine.Parameters(
        paramLabel = "openapi.json",
        descriptionKey = "openapi_definition"
    )
    private File file;

    @Override
    public void run() {
        var appModuleHandler = ApplicationModuleHandler.getInstance();
        appModuleHandler.addRestDependencies();
        var restAdapterHandler = RestAdapterHandler.getInstance();
        restAdapterHandler.loadOpenApiDefinition(file);
        JsonFileUtil.getProjectInfo().ifPresent(projectInfo -> {
            restAdapterHandler.createComponents(
                definitions -> appModuleHandler.createRecords(definitions, projectInfo));
            restAdapterHandler.createPaths(
                pathDefinitions -> restAdapterHandler.createResourcesPath(pathDefinitions,
                    projectInfo));
            restAdapterHandler.createApplicationPath(projectInfo);
        });

    }

}
