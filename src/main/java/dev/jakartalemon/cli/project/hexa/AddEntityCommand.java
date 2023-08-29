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

import dev.jakartalemon.cli.util.Constants;
import dev.jakartalemon.cli.util.HttpClientUtil;
import dev.jakartalemon.cli.util.JsonFileUtil;
import jakarta.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.io.File;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * @author Diego Silva <diego.silva at apuntesdejava.com>
 */
@CommandLine.Command(
    name = "addentity",
    resourceBundle = "messages",
    description = "Create a entity model to infrastructure. The model is given from the console " +
        "in JSON format"
)
@Slf4j
public class AddEntityCommand implements Callable<Integer> {
    private final Map<String, String> importablesMap;
    private final JsonObject databasesConfigs;

    @CommandLine.Parameters(
        paramLabel = "ENTITY_DEFINITION.json",
        descriptionKey = "entity_definition"
    )
    private File file;

    public AddEntityCommand() throws InterruptedException {
        importablesMap = HttpClientUtil.getConfigs(Constants.IMPORTABLES);
        databasesConfigs = HttpClientUtil.getDatabasesConfigs().orElseThrow();
    }

    @Override
    public Integer call() throws Exception {
        createDatabaseConfig();
        return JsonFileUtil.getFileJson(file.toPath())
            .map(structure -> JsonFileUtil.getProjectInfo().map(projectInfo -> {
                structure.forEach(
                    (key, classDef) -> createEntityClass(projectInfo, key,
                        classDef.asJsonObject()));
                return 0;
            }).orElse(1)).orElse(2);
    }

    private void createDatabaseConfig() {
    }

    private void createEntityClass(JsonObject projectInfo, String key, JsonObject jsonObject) {
    }
}
