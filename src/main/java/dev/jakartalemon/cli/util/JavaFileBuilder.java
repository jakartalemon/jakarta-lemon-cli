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
package dev.jakartalemon.cli.util;

import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static dev.jakartalemon.cli.util.Constants.COMMA_SPACE;
import static dev.jakartalemon.cli.util.Constants.DEFINE_FIELD_PATTERN;
import static dev.jakartalemon.cli.util.Constants.PRIVATE_FINAL_VARIABLE_TEMPLATE;
import static dev.jakartalemon.cli.util.Constants.PRIVATE_VARIABLE_STATIC_FINAL_TEMPLATE;
import static dev.jakartalemon.cli.util.Constants.PRIVATE_VARIABLE_TEMPLATE;
import static dev.jakartalemon.cli.util.Constants.PUBLIC;
import static dev.jakartalemon.cli.util.Constants.RETURN;
import static dev.jakartalemon.cli.util.Constants.TAB_SIZE;
import static dev.jakartalemon.cli.util.Constants.TEMPLATE_2_STRING;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;

/**
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
public class JavaFileBuilder {

    private String packageValue;
    private final Set<String> importsList = new HashSet<>();
    private final List<String> classAnnotationsList = new ArrayList<>();
    private final List<String> variablesList = new ArrayList<>();
    private final List<String> methodsList = new ArrayList<>();
    private final Set<String[]> constructorsVariablesSet = new LinkedHashSet<>();
    private String className;
    private String modulePath;
    private String module;
    private String resource;
    private String fileName;
    private String packageName;
    private String extendClass;
    private final Map<String, String> importablesMap;

    public JavaFileBuilder() {
        importablesMap = HttpClientUtil.getConfigs(Constants.IMPORTABLES);
    }

    public JavaFileBuilder setPackage(String appPackage,
                                      String moduleName,
                                      String submoduleName) {
        this.packageName = Constants.PACKAGE_TEMPLATE.formatted(appPackage, moduleName,
                                                                submoduleName);
        this.packageValue = Constants.TEMPLATE_2_STRING_COMMA.formatted(Constants.PACKAGE,
                                                                        packageName);
        return this;
    }

    public JavaFileBuilder addImportClass(String importClass) {
        importsList.add(importClass);
        return this;
    }

    public JavaFileBuilder addClassAnnotation(String classAnnotation) {
        classAnnotationsList.add(classAnnotation);
        return this;
    }

    public JavaFileBuilder setExtendClass(String extendClass) {
        this.extendClass = extendClass;
        return this;
    }

    public JavaFileBuilder setClassName(String className) {
        this.className = className;
        return this;
    }

    public JavaFileBuilder addVariableDeclaration(String variableType,
                                                  String variableName,
                                                  String variableAnnotation) {
        return addVariableDeclaration(variableType, variableName, variableAnnotation, false);
    }

    public JavaFileBuilder addVariableDeclaration(String variableType,
                                                  String variableName,
                                                  String variableAnnotation,
                                                  boolean isConstructor) {
        return addVariableDeclaration(variableType, variableName, variableAnnotation, isConstructor,
                                      false, null);
    }

    public JavaFileBuilder addVariableDeclaration(String variableType,
                                                  String variableName,
                                                  String variableAnnotation,
                                                  boolean isConstructor,
                                                  boolean isFinal,
                                                  String variableInit) {
        Optional.ofNullable(variableAnnotation).ifPresent(annotation -> variablesList.add("%s@%s".
                                                                                              formatted(
                                                                                                  StringUtils.repeat(
                                                                                                      SPACE,
                                                                                                      TAB_SIZE),
                                                                                                  annotation)));
        var declarations = TEMPLATE_2_STRING.formatted(variableType, variableName)
            + (StringUtils.isNotEmpty(variableInit) ? (" = " + variableInit) : EMPTY);
        var variableDeclarationTemplate = isFinal ? PRIVATE_VARIABLE_STATIC_FINAL_TEMPLATE :
            (isConstructor
                ? PRIVATE_FINAL_VARIABLE_TEMPLATE
                : PRIVATE_VARIABLE_TEMPLATE);
        var variableDeclaration =
            variableDeclarationTemplate.formatted(StringUtils.repeat(SPACE, TAB_SIZE),
                                                  declarations);

        variablesList.add(variableDeclaration);
        if (isConstructor) {
            constructorsVariablesSet.add(new String[]{variableType, variableName});
        }
        if (importablesMap.containsKey(variableType)) {
            importsList.add(importablesMap.get(variableType));
        }
        return this;
    }

    public JavaFileBuilder setModulePath(String modulePath) {

        this.modulePath = modulePath;
        return this;
    }

    public JavaFileBuilder setModule(String module) {

        this.module = module;
        return this;
    }

    public JavaFileBuilder setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public JavaFileBuilder setResource(String resource) {
        this.resource = resource;
        return this;
    }

    public void build() throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(packageValue);
        lines.add(StringUtils.EMPTY);
        importsList.forEach(importItem -> lines.add("import %s;".formatted(importItem)));
        lines.add(StringUtils.EMPTY);
        lines.addAll(classAnnotationsList.stream().map(annotation -> '@' + annotation).toList());
        if (StringUtils.isBlank(extendClass)) {
            lines.add("public class %s {".formatted(className));
        } else {
            lines.add("public class %s extends %s {".formatted(className, extendClass));
        }
        lines.add(StringUtils.EMPTY);
        lines.addAll(variablesList);
        if (!constructorsVariablesSet.isEmpty()) {
            lines.addAll(buildConstructor());
        }
        if (!methodsList.isEmpty()) {
            lines.add(StringUtils.EMPTY);
            lines.addAll(methodsList);
        }
        lines.add("}");

        FileClassUtil.writeClassFile(modulePath, resource, packageName, fileName, lines);
    }

    public JavaFileBuilder addMethod(String methodName,
                                     List<Map.Entry<String, JsonValue>> params,
                                     String returnValue,
                                     String defaultReturnValue) {
        return addMethod(methodName, params, returnValue, defaultReturnValue, null);

    }

    public JavaFileBuilder addMethod(String methodName,
                                     List<Map.Entry<String, JsonValue>> params,
                                     String returnValue,
                                     String defaultReturnValue,
                                     List<String> annotations) {
        var parameters = params.stream().map(param -> {
            var paramType = ((JsonString) param.getValue()).getString();
            if (importablesMap.containsKey(paramType)) {
                addImportClass(paramType);
            }
            return TEMPLATE_2_STRING.formatted(paramType, param.getKey());

        }).collect(Collectors.joining(COMMA_SPACE));
        Optional.ofNullable(annotations).ifPresent(annotationsList -> {
            methodsList.add(String.join(System.lineSeparator(),
                                        annotationsList.stream()
                                            .map(annotation -> "%s@%s".formatted(StringUtils.repeat(
                                                SPACE, TAB_SIZE), annotation)).toList()));
        });
        var methodSignature = "%s%s %s %s (%s) {".formatted(StringUtils.repeat(
            SPACE, TAB_SIZE), PUBLIC, returnValue, methodName, parameters);
        methodsList.add(methodSignature);
        if (defaultReturnValue != null) {
            methodsList.add(DEFINE_FIELD_PATTERN.
                                formatted(StringUtils.repeat(SPACE, TAB_SIZE * 2),
                                          RETURN, defaultReturnValue));
        }
        methodsList.add("%s}".formatted(StringUtils.repeat(SPACE, TAB_SIZE)));
        return this;
    }

    private List<String> buildConstructor() {
        List<String> lines = new ArrayList<>();
        var constructorParameters = constructorsVariablesSet.stream().map(
            variable -> variable[0] + SPACE + variable[1]).collect(Collectors.joining(COMMA_SPACE));
        lines.add("%s%s %s (%s){".formatted(StringUtils.repeat(SPACE, TAB_SIZE), PUBLIC, className,
                                            constructorParameters));
        constructorsVariablesSet.forEach(variable
                                             -> lines.add(
            "%sthis.%2$s = %2$s;".formatted(StringUtils.repeat(
                SPACE, TAB_SIZE * 2), variable[1])));
        lines.add("%s}".formatted(StringUtils.repeat(
            SPACE, TAB_SIZE)));
        return lines;

    }

}
