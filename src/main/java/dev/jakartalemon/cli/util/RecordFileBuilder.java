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

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static dev.jakartalemon.cli.util.Constants.COMMA_SPACE;
import static dev.jakartalemon.cli.util.Constants.PRIVATE_FINAL_VARIABLE_TEMPLATE;
import static dev.jakartalemon.cli.util.Constants.PRIVATE_VARIABLE_TEMPLATE;
import static dev.jakartalemon.cli.util.Constants.TAB_SIZE;
import static dev.jakartalemon.cli.util.Constants.TEMPLATE_2_STRING;
import static org.apache.commons.lang3.StringUtils.SPACE;

public class RecordFileBuilder {
    private Map<String, String> importablesMap;
    private final List<String> variablesList = new ArrayList<>();
    private String packageName;
    private String packageValue;
    private String className;
    private String modulePath;
    private String fileName;
    private String resource;
    private String module;

    public RecordFileBuilder() {
        importablesMap = HttpClientUtil.getConfigs(Constants.IMPORTABLES);
    }

    public RecordFileBuilder setPackage(String appPackage,
                                        String moduleName,
                                        String submoduleName) {
        this.packageName = Constants.PACKAGE_TEMPLATE.formatted(appPackage, moduleName,
                                                                submoduleName);
        this.packageValue = Constants.TEMPLATE_2_STRING_COMMA.formatted(Constants.PACKAGE,
                                                                        packageName);
        return this;
    }

    public RecordFileBuilder addVariableDeclaration(String variableType,
                                                    String variableName) {
        var declarations = TEMPLATE_2_STRING.formatted(variableType, variableName);
        variablesList.add(declarations);
        return this;
    }

    public RecordFileBuilder setClassName(String className) {
        this.className = className;
        return this;
    }

    public RecordFileBuilder setModulePath(String modulePath) {

        this.modulePath = modulePath;
        return this;
    }

    public RecordFileBuilder setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public RecordFileBuilder setResource(String resource) {
        this.resource = resource;
        return this;
    }
    public RecordFileBuilder setModule(String module) {

        this.module = module;
        return this;
    }
    public void build() throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(packageValue);
        lines.add(StringUtils.EMPTY);
        var variableConstructors = String.join(COMMA_SPACE, variablesList);
        lines.add("public record %s (%s) {".formatted(className, variableConstructors));
        lines.add(StringUtils.EMPTY);
        lines.add("}");
        FileClassUtil.writeClassFile(modulePath, resource, packageName, fileName, lines);
    }
}
