/*
 * Copyright 2023 diego.
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

import dev.jakartalemon.cli.project.hexa.handler.InfrastructureModuleHandler;
import dev.jakartalemon.cli.project.hexa.handler.JpaPersistenceHandler;
import dev.jakartalemon.cli.util.JsonFileUtil;
import jakarta.json.JsonValue;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.io.File;
import java.util.concurrent.Callable;

import static dev.jakartalemon.cli.util.Constants.ENTITIES;
import static dev.jakartalemon.cli.util.Constants.INFRASTRUCTURE;
import static dev.jakartalemon.cli.util.Constants.MAP_TO_MODEL;
import static dev.jakartalemon.cli.util.Constants.PACKAGE;

/**
 * @author Diego Silva <diego.silva at apuntesdejava.com>
 */
@CommandLine.Command(
    name = "addentity",
    resourceBundle = "messages",
    description = "Create a entity model to infrastructure. The model is given from the console "
    + "in JSON format"
)
@Slf4j
public class AddEntityCommand implements Callable<Integer> {

    @CommandLine.Parameters(
        paramLabel = "ENTITY_DEFINITION.json",
        descriptionKey = "entity_definition"
    )
    private File file;

    @Override
    public Integer call() throws Exception {
        var infrastructureModuleHandler = InfrastructureModuleHandler.getInstance();
        infrastructureModuleHandler.createDatabaseConfig(file);
        var jpaPersistenceHandler = JpaPersistenceHandler.getInstance();
        var persistenceUnitName = "persistenceUnit";
        return JsonFileUtil.getFileJson(file.toPath())
            .map(structure -> JsonFileUtil.getProjectInfo().map(projectInfo -> {
            structure.getJsonArray(ENTITIES).stream()
                .map(JsonValue::asJsonObject)
                .forEach(item -> item.forEach(
                (key, classDef) -> {
                    var definition = classDef.asJsonObject();
                    jpaPersistenceHandler.createEntityClass(projectInfo, key, definition);
                    if (definition.containsKey(MAP_TO_MODEL)) {
                        infrastructureModuleHandler.createMapper(
                            projectInfo.getString(PACKAGE),
                            projectInfo.getString(INFRASTRUCTURE),
                            definition.getString(MAP_TO_MODEL),
                            key
                        );
                    }

                }));
            jpaPersistenceHandler.createPersistenceUnit(infrastructureModuleHandler.
                getDataSourceName(), persistenceUnitName);
            jpaPersistenceHandler.savePersistenceXml();

            jpaPersistenceHandler.createEntityManagerProvider(projectInfo, persistenceUnitName);
            infrastructureModuleHandler.createCdiDescriptor(projectInfo.getString(INFRASTRUCTURE));
            return 0;
        }).orElse(1)).orElse(2);
    }

}
