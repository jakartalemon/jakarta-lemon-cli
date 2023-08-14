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
import jakarta.json.Json;
import jakarta.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static dev.jakartalemon.cli.util.Constants.DOMAIN;
import static dev.jakartalemon.cli.util.Constants.JAKARTA_LEMON_CONFIG_URL;
import static dev.jakartalemon.cli.util.Constants.MODEL;
import static dev.jakartalemon.cli.util.Constants.PACKAGE;
import static dev.jakartalemon.cli.util.Constants.TAB_SIZE;
import dev.jakartalemon.cli.util.HttpClientUtil;
import jakarta.json.JsonReader;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;

/**
 * @author Diego Silva <diego.silva at apuntesdejava.com>
 */
@Command(
    name = "addmodel",
    resourceBundle = "messages",
    description = "Create a domain model. The model is given from the console in JSON format"
)
@Slf4j
public class AddModelCommand implements Callable<Integer> {

    private static final String IMPORTABLES = "importables";

    @Parameters(
        paramLabel = "MODEL_DEFINITION.json",
        descriptionKey = "model_definition"
    )
    private File file;
    private Map<String, String> importablesMap = new LinkedHashMap<>();

    public AddModelCommand() throws InterruptedException {

        try {
            var config = HttpClientUtil.getJson(JAKARTA_LEMON_CONFIG_URL, JsonReader::readObject);
            var importablesJson = config.getJsonObject(IMPORTABLES);
            importablesJson.keySet().forEach(key -> importablesMap.put(key, importablesJson.
                getString(key)));
        } catch (IOException | URISyntaxException ex) {
            log.error(ex.getMessage(), ex);

        }
    }

    @Override
    public Integer call() throws Exception {
        var projectInfoPath = Path.of(Constants.PROJECT_INFO_JSON);

        if (!Files.exists(projectInfoPath)) {
            log.error("File not found: {}", projectInfoPath.getFileName());
            return 1;
        }

        if (!Files.exists(file.toPath())) {
            log.error("File not found: {}", file);
            return 2;
        }
        try (var jsonReader = Json.createReader(Files.newBufferedReader(file.toPath()));
            var projectInfoReader = Json.createReader(Files.newBufferedReader(projectInfoPath))) {
            var structure = jsonReader.readObject();
            var projectInfo = projectInfoReader.readObject();
            structure.forEach(
                (key, classDef) -> createClass(projectInfo, key, classDef.asJsonObject()));

        }

        return 0;
    }

    private void createClass(JsonObject projectInfo,
        String className,
        JsonObject classDef) {
        try {
            log.info("Creating {} class", className);
            List<String> lines = new ArrayList<>();
            var packageName = "%s.%s.%s".formatted(projectInfo.getString(PACKAGE), DOMAIN, MODEL);
            lines.add("package %s;".formatted(packageName));
            lines.add(EMPTY);
            lines.add("import lombok.Getter;");
            lines.add("import lombok.Setter;");
            lines.add(EMPTY);
            lines.add("@Setter");
            lines.add("@Getter");
            lines.add("public class %s {".formatted(className));

            //creating fields
            classDef.keySet().forEach(field -> {
                var classType = classDef.getString(field);
                lines.
                    add("%s%s %s;".formatted(StringUtils.repeat(SPACE, TAB_SIZE), classType,
                        field));
                if (importablesMap.containsKey(classType)) {
                    lines.add(4, "import %s;".formatted(importablesMap.get(classType)));
                }
            });

            lines.add("}");

            var classPackage = Path.of(projectInfo.getString(DOMAIN), "src", "main", "java");
            var packageNameList = packageName.split("\\.");
            for (var item : packageNameList) {
                classPackage = classPackage.resolve(item);
            }
            var classPath = classPackage.resolve("%s.java".formatted(className));
            Files.createDirectories(classPath.getParent());
            Files.write(classPath, lines);
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

}
