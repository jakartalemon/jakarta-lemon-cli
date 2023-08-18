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
import jakarta.json.Json;
import jakarta.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

import static dev.jakartalemon.cli.util.Constants.DOMAIN;
import static dev.jakartalemon.cli.util.Constants.JAVA;
import static dev.jakartalemon.cli.util.Constants.MAIN;
import static dev.jakartalemon.cli.util.Constants.MODEL;
import static dev.jakartalemon.cli.util.Constants.PACKAGE;
import static dev.jakartalemon.cli.util.Constants.PACKAGE_TEMPLATE;
import static dev.jakartalemon.cli.util.Constants.REPOSITORY;
import static dev.jakartalemon.cli.util.Constants.SRC;
import static dev.jakartalemon.cli.util.Constants.TAB_SIZE;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.Optional;

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

    private final Map<String, String> importablesMap;

    @Parameters(
        paramLabel = "MODEL_DEFINITION.json",
        descriptionKey = "model_definition"
    )
    private File file;

    public AddModelCommand() throws InterruptedException {

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
            var repositoryPath = createRepository(projectInfo);
            structure.forEach(
                (key, classDef) -> createClass(projectInfo, key, classDef.asJsonObject(),
                    repositoryPath.orElseThrow()));

        }

        return 0;
    }

    private void createClass(JsonObject projectInfo,
        String className,
        JsonObject classDef,
        Path repositoryPath) {
        try {
            log.info("Creating {} class", className);
            List<String> lines = new ArrayList<>();
            var packageName =
                PACKAGE_TEMPLATE.formatted(projectInfo.getString(PACKAGE), DOMAIN, MODEL);
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

            var classPackage = Path.of(projectInfo.getString(DOMAIN), SRC, MAIN, JAVA);
            var packageNameList = packageName.split("\\.");
            for (var item : packageNameList) {
                classPackage = classPackage.resolve(item);
            }
            var classPath = classPackage.resolve("%s.java".formatted(className));
            Files.createDirectories(classPath.getParent());
            Files.write(classPath, lines);

            createRepository(projectInfo, repositoryPath, className);
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private Optional<Path> createRepository(JsonObject projectInfo) {
        try (var isr = new InputStreamReader(
            Objects.requireNonNull(AddModelCommand.class.getResourceAsStream(
                "/classes/Repository.java.template")), UTF_8); var br = new BufferedReader(isr)) {
            var packageName
                = PACKAGE_TEMPLATE.formatted(projectInfo.getString(PACKAGE), DOMAIN, REPOSITORY);

            List<String> lines = new ArrayList<>(br.lines().toList());
            lines.add(0, "%s %s;".formatted(PACKAGE, packageName));

            lines.forEach(log::debug);

            var repositoryPackage = Path.of(projectInfo.getString(DOMAIN), SRC, MAIN, JAVA);
            var packageNameList = packageName.split("\\.");
            for (var item : packageNameList) {
                repositoryPackage = repositoryPackage.resolve(item);
            }
            Files.createDirectories(repositoryPackage);
            var repositoryPath = repositoryPackage.resolve("IRepository.java");
            Files.write(repositoryPath, lines);
            return Optional.of(repositoryPath.getParent());
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
        return Optional.empty();

    }

    private void createRepository(JsonObject projectInfo,
        Path repositoryPath,
        String className) throws IOException {
        var packageName
            = PACKAGE_TEMPLATE.formatted(projectInfo.getString(PACKAGE), DOMAIN, REPOSITORY);
        var modelPackage = "%s.%s.%s.%s".formatted(projectInfo.getString(PACKAGE), DOMAIN, MODEL,
            className);
        var fileName = "%sRepository".formatted(className);

        List<String> lines = new ArrayList<>();
        lines.add("%s %s;".formatted(PACKAGE, packageName));
        lines.add(SPACE);
        lines.add("import %s;".formatted(modelPackage));
        lines.add(SPACE);
        lines.add("public interface %s extends IRepository {".formatted(fileName));
        lines.add("}");
        var repositoryModelPath = repositoryPath.resolve(fileName + ".java");
        Files.write(repositoryModelPath, lines);
    }

}
