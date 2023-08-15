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

import dev.jakartalemon.cli.util.Constants;
import static dev.jakartalemon.cli.util.Constants.DOMAIN;
import static dev.jakartalemon.cli.util.Constants.PACKAGE;
import static dev.jakartalemon.cli.util.Constants.SERVICE;
import static dev.jakartalemon.cli.util.Constants.TAB_SIZE;
import dev.jakartalemon.cli.util.HttpClientUtil;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import picocli.CommandLine;

/**
 *
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
@CommandLine.Command(
    name = "addservice",
    resourceBundle = "messages",
    description = "Create a domain service. The service is given from the console in JSON format"
)
@Slf4j
public class AddServiceCommand implements Callable<Integer> {

    @CommandLine.Parameters(
        paramLabel = "SERVICE_DEFINITION.json",
        descriptionKey = "service_definition"
    )
    private File file;
    private Map<String, String> importablesMap;

    public AddServiceCommand() throws InterruptedException {
        importablesMap = HttpClientUtil.getConfigs(Constants.IMPORTABLES);

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
                (key, classDef) -> createServiceClass(projectInfo, key, classDef.asJsonObject()));

        }

        return 0;
    }

    private void createServiceClass(JsonObject projectInfo,
        String className,
        JsonObject methodsDefinitions) {
        try {
            log.info("Creating {} service class", className);
            List<String> lines = new ArrayList<>();
            var packageName = "%s.%s.%s".formatted(projectInfo.getString(PACKAGE), DOMAIN, SERVICE);
            lines.add("package %s;".formatted(packageName));
            lines.add(EMPTY);
            lines.add("public class %s {".formatted(className));
            lines.add(EMPTY);
            methodsDefinitions.keySet().forEach(methodName -> {
                var methodSignature = "%spublic void %s () {".formatted(StringUtils.repeat(SPACE,
                    TAB_SIZE),
                    methodName);
                lines.add(methodSignature);
                lines.add("%s}".formatted(StringUtils.repeat(SPACE, TAB_SIZE)));
                lines.add(EMPTY);
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
