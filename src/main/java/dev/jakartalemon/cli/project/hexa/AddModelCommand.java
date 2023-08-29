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
import dev.jakartalemon.cli.util.FileClassUtil;
import dev.jakartalemon.cli.util.HttpClientUtil;
import dev.jakartalemon.cli.util.JsonFileUtil;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
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
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import static dev.jakartalemon.cli.util.Constants.DOMAIN;
import static dev.jakartalemon.cli.util.Constants.IMPORT_PACKAGE_TEMPLATE;
import static dev.jakartalemon.cli.util.Constants.JAVA;
import static dev.jakartalemon.cli.util.Constants.MAIN;
import static dev.jakartalemon.cli.util.Constants.MODEL;
import static dev.jakartalemon.cli.util.Constants.PACKAGE;
import static dev.jakartalemon.cli.util.Constants.PACKAGE_TEMPLATE;
import static dev.jakartalemon.cli.util.Constants.REPOSITORY;
import static dev.jakartalemon.cli.util.Constants.SRC;
import static dev.jakartalemon.cli.util.Constants.TAB_SIZE;
import static java.nio.charset.StandardCharsets.UTF_8;
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
    public Integer call() {
        return JsonFileUtil.getFileJson(file.toPath())
            .map(structure-> {
                return JsonFileUtil.getProjectInfo().map(projectInfo -> {
                    var repositoryPath = createRepository(projectInfo);
                    structure.forEach(
                        (key, classDef) -> createClass(projectInfo, key, classDef.asJsonObject(),
                            repositoryPath.orElseThrow()));
                    return 0;
                }).orElse(1);
            }).orElse(2);



    }

    private void createClass(JsonObject projectInfo,
        String className,
        JsonObject classDef,
        Path repositoryPath) {

        log.info("Creating {} class", className);
        List<String> lines = new ArrayList<>();
        var packageName
            = PACKAGE_TEMPLATE.formatted(projectInfo.getString(PACKAGE), DOMAIN, MODEL);
        lines.add("package %s;".formatted(packageName));
        lines.add(EMPTY);
        lines.add("import lombok.Getter;");
        lines.add("import lombok.Setter;");
        lines.add(EMPTY);
        lines.add("@Setter");
        lines.add("@Getter");
        lines.add("public class %s {".formatted(className));
        AtomicReference<String> primaryKeyTypeRef = new AtomicReference<>();

        //creating fields
        classDef.keySet().forEach(field -> {
            var classTypeValue = classDef.get(field);
            String classType = EMPTY;
            if (classTypeValue.getValueType() == JsonValue.ValueType.STRING) {
                classType = classDef.getString(field);

            } else if (classTypeValue.getValueType() == JsonValue.ValueType.OBJECT) {
                var classObjectDef = classTypeValue.asJsonObject();
                var primaryKey = classObjectDef.getBoolean("primaryKey", false);
                classType = classObjectDef.getString("type");
                if (primaryKey) {
                    primaryKeyTypeRef.set(classType);
                }
            }
            lines.
                add("%s%s %s;".formatted(StringUtils.repeat(SPACE, TAB_SIZE), classType,
                    field));
            if (importablesMap.containsKey(classType)) {
                lines.add(4, IMPORT_PACKAGE_TEMPLATE.formatted(importablesMap.get(classType)));
            }
        });

        lines.add("}");
        Optional.ofNullable(primaryKeyTypeRef.get()).ifPresentOrElse(primaryKeyType -> {
            try {
                FileClassUtil.writeFile(projectInfo, packageName, className, lines);
                log.info("{} class Created", className);

                createRepository(projectInfo, repositoryPath, className, primaryKeyType);
            } catch (IOException ex) {
                log.error(ex.getMessage(), ex);
            }
        }, ()
            -> log.error("You must specify a field that is of type \"primaryKey\" for the {} class",
            className));

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
        String className,
        String primaryKeyType) throws IOException {
        var packageName
            = PACKAGE_TEMPLATE.formatted(projectInfo.getString(PACKAGE), DOMAIN, REPOSITORY);
        var modelPackage = "%s.%s.%s.%s".formatted(projectInfo.getString(PACKAGE), DOMAIN, MODEL,
            className);
        var fileName = "%sRepository".formatted(className);

        List<String> lines = new ArrayList<>();
        lines.add("%s %s;".formatted(PACKAGE, packageName));
        lines.add(EMPTY);
        lines.add(IMPORT_PACKAGE_TEMPLATE.formatted(modelPackage));
        lines.add(EMPTY);
        lines.add("public interface %s extends IRepository<%s, %s> {".formatted(fileName, className,
            primaryKeyType));
        lines.add("}");
        if (importablesMap.containsKey(primaryKeyType)) {
            lines.add(2, IMPORT_PACKAGE_TEMPLATE.formatted(importablesMap.get(primaryKeyType)));
        }
        var repositoryModelPath = repositoryPath.resolve(fileName + ".java");
        Files.write(repositoryModelPath, lines);
    }

}
