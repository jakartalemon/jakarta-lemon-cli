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
import dev.jakartalemon.cli.util.FileClassUtil;
import dev.jakartalemon.cli.util.HttpClientUtil;
import dev.jakartalemon.cli.util.JsonFileUtil;
import jakarta.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static dev.jakartalemon.cli.util.Constants.CLASSES_IMPORT_TEST;
import static dev.jakartalemon.cli.util.Constants.COLON;
import static dev.jakartalemon.cli.util.Constants.COMMA;
import static dev.jakartalemon.cli.util.Constants.DOMAIN;
import static dev.jakartalemon.cli.util.Constants.IMPORT_PACKAGE_TEMPLATE;
import static dev.jakartalemon.cli.util.Constants.INJECTS;
import static dev.jakartalemon.cli.util.Constants.MODEL;
import static dev.jakartalemon.cli.util.Constants.PACKAGE;
import static dev.jakartalemon.cli.util.Constants.PACKAGE_TEMPLATE;
import static dev.jakartalemon.cli.util.Constants.PUBLIC;
import static dev.jakartalemon.cli.util.Constants.REPOSITORY;
import static dev.jakartalemon.cli.util.Constants.RETURN;
import static dev.jakartalemon.cli.util.Constants.TAB_SIZE;
import static dev.jakartalemon.cli.util.Constants.TEMPLATE_2_STRING;
import static dev.jakartalemon.cli.util.Constants.TEST;
import static dev.jakartalemon.cli.util.Constants.USECASE;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static dev.jakartalemon.cli.util.Constants.TEMPLATE_2_STRING_COMMA;

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

    private static final int IMPORT_LINE = 2;
    private final Map<String, String> importablesMap;
    @CommandLine.Parameters(
        paramLabel = "USECASE_DEFINITION.json",
        descriptionKey = "usecase_definition"
    )
    private File file;

    public AddUseCaseCommand() throws InterruptedException {
        importablesMap = HttpClientUtil.getConfigs(Constants.IMPORTABLES);
    }

    @Override
    public Integer call() throws Exception {
        return JsonFileUtil.getFileJson(file.toPath())
            .map(structure -> JsonFileUtil.getProjectInfo().map(projectInfo -> {
                structure.forEach(
                    (key, classDef) -> {
                        createUseCaseClass(projectInfo, key,
                            classDef.asJsonObject());
                        createUseCaseTestClass(projectInfo, key,
                            classDef.asJsonObject());
                    });
                return 0;
            }).orElse(1)).orElse(2);
    }

    private void createUseCaseTestClass(JsonObject projectInfo, String className,
        JsonObject classDefinition) {
        try {
            List<String> lines = new ArrayList<>();
            var packageName
                = PACKAGE_TEMPLATE.formatted(projectInfo.getString(PACKAGE), DOMAIN, USECASE);
            lines.add(TEMPLATE_2_STRING_COMMA.formatted(PACKAGE, packageName));
            lines.add(EMPTY);
            List<String> classesInject = getClassesInject(projectInfo, classDefinition, lines);
            CLASSES_IMPORT_TEST.forEach(
                importClass -> lines.add("import %s;".formatted(importClass)));
            lines.add(EMPTY);
            var classTestName = "%sTest".formatted(className);
            lines.add("@ExtendWith(MockitoExtension.class)");
            lines.add("%s class %s {".formatted(PUBLIC, classTestName));
            lines.add(EMPTY);

            classesInject.forEach(classInject -> {
                lines.add("%s@Mock".formatted(StringUtils.repeat(SPACE, TAB_SIZE)));
                var variableName = StringUtils.uncapitalize(classInject);
                var declarations = TEMPLATE_2_STRING.formatted(classInject, variableName);
                lines.add(TEMPLATE_2_STRING_COMMA.formatted(StringUtils.repeat(SPACE, TAB_SIZE),
                    declarations));
            });
            lines.add(EMPTY);

            lines.add("%s@InjectMocks".formatted(StringUtils.repeat(SPACE, TAB_SIZE)));
            var classNameInstance = StringUtils.uncapitalize(className);
            lines.add("%s%s %s;".formatted(StringUtils.repeat(SPACE, TAB_SIZE),
                className, classNameInstance));
            lines.add("}");
            FileClassUtil.writeClassFile(projectInfo, packageName, classTestName, lines, TEST);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void createUseCaseClass(JsonObject projectInfo,
        String className,
        JsonObject classDefinition) {
        try {
            log.info("Creating {} use case class", className);
            List<String> lines = new ArrayList<>();
            var packageName
                = PACKAGE_TEMPLATE.formatted(projectInfo.getString(PACKAGE), DOMAIN, USECASE);
            lines.add(TEMPLATE_2_STRING_COMMA.formatted(PACKAGE, packageName));
            lines.add(EMPTY);
            lines.add(
                "import %s.%s.%s.*;".formatted(projectInfo.getString(PACKAGE), DOMAIN, MODEL));
            List<String> classesInject = getClassesInject(projectInfo, classDefinition, lines);
            lines.add("%s class %s {".formatted(PUBLIC, className));
            lines.add(EMPTY);
            if (!classesInject.isEmpty()) {
                List<String> constructorsParameters = new ArrayList<>();
                classesInject.forEach(classInject -> {
                    var variableName = StringUtils.uncapitalize(classInject);
                    var declarations = TEMPLATE_2_STRING.formatted(classInject, variableName);
                    lines.add("%sprivate final %s;".formatted(StringUtils.repeat(SPACE, TAB_SIZE),
                        declarations));
                    constructorsParameters.add(declarations);
                    lines.add(EMPTY);
                });
                lines.add(EMPTY);
                if (!constructorsParameters.isEmpty()) {
                    var parameterDeclaration = String.join(COMMA, constructorsParameters);
                    lines.add("%s%s %s (%s){".formatted(StringUtils.repeat(
                        SPACE, TAB_SIZE), PUBLIC, className, parameterDeclaration));
                    classesInject.forEach(classInject -> {
                        var variableName = StringUtils.uncapitalize(classInject);
                        lines.add("%sthis.%s = %s;".formatted(StringUtils.repeat(
                            SPACE, TAB_SIZE * 2), variableName, variableName));
                    });
                    lines.add("%s}".formatted(StringUtils.repeat(
                        SPACE, TAB_SIZE)));

                    lines.add(EMPTY);
                }
            }

            var methods = classDefinition.getJsonObject("methods");

            methods.keySet().forEach(methodName -> {
                var method = methods.getJsonObject(methodName);
                var returnValue = method.getString(RETURN, "void");
                String defaultReturnValue = null;
                if (returnValue.contains(COLON)) {
                    var returns = returnValue.split(COLON);
                    returnValue = returns[0];
                    defaultReturnValue = returns[1];
                }
                var parameters = method.keySet().stream()
                    .filter(methodNameKey -> !methodNameKey.equals(RETURN))
                    .map(methodNameKey -> {
                        var parameterType = method.getString(methodNameKey);
                        if (importablesMap.containsKey(parameterType)) {
                            lines.add(IMPORT_LINE, IMPORT_PACKAGE_TEMPLATE.formatted(
                                importablesMap.get(parameterType)));
                        }
                        return TEMPLATE_2_STRING.formatted(parameterType,
                            methodNameKey);
                    }).collect(Collectors.joining(COMMA));

                var methodSignature = "%s%s %s %s (%s) {".formatted(StringUtils.repeat(
                    SPACE, TAB_SIZE), PUBLIC, returnValue, methodName, parameters);
                lines.add(methodSignature);
                if (defaultReturnValue != null) {
                    lines.add("%s%s %s;".formatted(StringUtils.repeat(SPACE, TAB_SIZE * 2),
                        RETURN, defaultReturnValue));
                }
                lines.add("%s}".formatted(StringUtils.repeat(SPACE, TAB_SIZE)));
                lines.add(EMPTY);
            });
            lines.add("}");

            FileClassUtil.writeClassFile(projectInfo, packageName, className, lines);
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }

    }

    private List<String> getClassesInject(JsonObject projectInfo, JsonObject classDefinition,
        List<String> lines) {
        List<String> classesInject = new ArrayList<>();
        if (classDefinition.containsKey(INJECTS)) {
            var injects = classDefinition.getJsonArray(INJECTS);
            for (var i = 0; i < injects.size(); i++) {
                var inject = injects.getString(i);
                var importInject
                    = "import %s.%s.%s.%s;".formatted(projectInfo.getString(PACKAGE),
                    DOMAIN, REPOSITORY, inject);
                lines.add(importInject);
                classesInject.add(inject);
            }
            lines.add(EMPTY);
        }
        return classesInject;
    }

}
