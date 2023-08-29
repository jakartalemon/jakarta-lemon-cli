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

import dev.jakartalemon.cli.util.FileClassUtil;
import dev.jakartalemon.cli.util.JsonFileUtil;
import jakarta.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static dev.jakartalemon.cli.util.Constants.DOMAIN;
import static dev.jakartalemon.cli.util.Constants.PACKAGE;
import static dev.jakartalemon.cli.util.Constants.PACKAGE_TEMPLATE;
import static dev.jakartalemon.cli.util.Constants.REPOSITORY;
import static dev.jakartalemon.cli.util.Constants.TAB_SIZE;
import static dev.jakartalemon.cli.util.Constants.USECASE;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;

/**
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
@CommandLine.Command(
    name = "addusecase",
    resourceBundle = "messages",
    description = "Create a use case. The use case is given from the console in JSON format"
)
@Slf4j
public class AddUseCaseCommand implements Callable<Integer> {

    @CommandLine.Parameters(
        paramLabel = "USECASE_DEFINITION.json",
        descriptionKey = "usecase_definition"
    )
    private File file;

    public AddUseCaseCommand() throws InterruptedException {
        //  Map<String, String> importablesMap = HttpClientUtil.getConfigs(Constants.IMPORTABLES);

    }

    @Override
    public Integer call() throws Exception {
        return JsonFileUtil.getFileJson(file.toPath())
            .map(structure -> JsonFileUtil.getProjectInfo().map(projectInfo -> {
                structure.forEach(
                    (key, classDef) -> createUseCaseClass(projectInfo, key,
                        classDef.asJsonObject()));
                return 0;
            }).orElse(1)).orElse(2);
    }

    private void createUseCaseClass(JsonObject projectInfo,
        String className,
        JsonObject classDefinition) {
        try {
            log.info("Creating {} use case class", className);
            List<String> lines = new ArrayList<>();
            var packageName
                = PACKAGE_TEMPLATE.formatted(projectInfo.getString(PACKAGE), DOMAIN, USECASE);
            lines.add("package %s;".formatted(packageName));
            lines.add(EMPTY);
            List<String> classesInject = new ArrayList<>();
            if (classDefinition.containsKey("injects")) {
                var injects = classDefinition.getJsonArray("injects");
                for (var i = 0; i < injects.size(); i++) {
                    var inject = injects.getString(i);
                    var importInject =
                        "import %s.%s.%s.%s;".formatted(projectInfo.getString(PACKAGE),
                            DOMAIN, REPOSITORY, inject);
                    lines.add(importInject);
                    classesInject.add(inject);
                }
                lines.add("import jakarta.inject.Inject;");
                lines.add(EMPTY);
            }
            lines.add("public class %s {".formatted(className));
            lines.add(EMPTY);
            if (!classesInject.isEmpty()) {
                classesInject.forEach(classInject -> {
                    lines.add("%s@Inject".formatted(StringUtils.repeat(SPACE, TAB_SIZE)));
                    var variableName = StringUtils.uncapitalize(classInject);
                    lines.add("%sprivate %s %s;".formatted(StringUtils.repeat(SPACE, TAB_SIZE),
                        classInject, variableName));
                    lines.add(EMPTY);
                });
                lines.add(EMPTY);
            }

            var methods = classDefinition.getJsonObject("methods");

            methods.keySet().forEach(methodName -> {
                var method = methods.getJsonObject(methodName);
                var returnValue = method.getString("return", "void");
                var methodSignature = "%spublic %s %s () {".formatted(StringUtils.repeat(
                    SPACE, TAB_SIZE), returnValue, methodName);
                lines.add(methodSignature);
                lines.add("%s}".formatted(StringUtils.repeat(SPACE, TAB_SIZE)));
                lines.add(EMPTY);
            });
            lines.add("}");

            FileClassUtil.writeFile(projectInfo, packageName, className, lines);
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }

    }

}
