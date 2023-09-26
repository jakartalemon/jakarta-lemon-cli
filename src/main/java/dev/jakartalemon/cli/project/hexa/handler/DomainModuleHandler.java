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
package dev.jakartalemon.cli.project.hexa.handler;

import dev.jakartalemon.cli.model.PomModel;
import dev.jakartalemon.cli.project.hexa.AddModelCommand;
import dev.jakartalemon.cli.util.Constants;
import dev.jakartalemon.cli.util.FileClassUtil;
import dev.jakartalemon.cli.util.HttpClientUtil;
import dev.jakartalemon.cli.util.PomUtil;
import jakarta.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import static dev.jakartalemon.cli.util.Constants.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.file.Files;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;

/**
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
@Slf4j
public class DomainModuleHandler {

    private static final int IMPORT_LINE = 2;

    private Map<String, String> importablesMap = Collections.emptyMap();

    private DomainModuleHandler()  {

            importablesMap = HttpClientUtil.getConfigs(Constants.IMPORTABLES);

    }

    public static DomainModuleHandler getInstance() {
        return DomainModuleHandlerHolder.INSTANCE;
    }

    public void createUseCaseTestClass(JsonObject projectInfo,
                                       String className,
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
            lines.add(DEFINE_FIELD_PATTERN.formatted(StringUtils.repeat(SPACE, TAB_SIZE),
                className, classNameInstance));
            lines.add("}");
            FileClassUtil.writeClassFile(projectInfo, TEST, packageName, classTestName, lines,
                DOMAIN);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public Optional<Path> createDomainModule(Path projectPath,
                                             String groupId,
                                             String artifactId,
                                             String version,
                                             String packageName) {

        var modulePom = PomModel.builder().parent(Map.of(
            GROUP_ID, groupId,
            ARTIFACT_ID, artifactId,
            VERSION, version
        )).packaging(JAR).dependencies(List.of(
            LOMBOK_DEPENDENCY, MOCKITO_DEPENDENCY
        )).properties(Map.of(
            MAVEN_COMPILER_RELEASE, JAVA_VERSION
        )).artifactId(DOMAIN);

        var pomPath = PomUtil.getInstance().createPom(
            projectPath.resolve(DOMAIN), modulePom.build()
        );
        pomPath.ifPresent(pom -> {
            log.debug("domain created at {}", pom);
            var parent = pom.getParent();
            PomUtil.getInstance().createJavaProjectStructure(parent, PACKAGE_TEMPLATE.formatted(
                packageName, DOMAIN, REPOSITORY));
            PomUtil.getInstance()
                .createJavaProjectStructure(parent,
                    PACKAGE_TEMPLATE.formatted(packageName, DOMAIN, MODEL));
            PomUtil.getInstance().
                createJavaProjectStructure(parent,
                    PACKAGE_TEMPLATE.formatted(packageName, DOMAIN, USECASE));
        });
        return pomPath;
    }

    private List<String> getClassesInject(JsonObject projectInfo,
                                          JsonObject classDefinition,
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

    public void createUseCaseClass(JsonObject projectInfo,
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
                    lines.add(DEFINE_FIELD_PATTERN.formatted(StringUtils.repeat(SPACE, TAB_SIZE * 2),
                        RETURN, defaultReturnValue));
                }
                lines.add("%s}".formatted(StringUtils.repeat(SPACE, TAB_SIZE)));
                lines.add(EMPTY);
            });
            lines.add("}");

            FileClassUtil.writeClassFile(projectInfo, packageName, className, lines, DOMAIN);
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }

    }

    public Optional<Path> createRepository(JsonObject projectInfo) {
        try (var isr = new InputStreamReader(
            Objects.requireNonNull(AddModelCommand.class.getResourceAsStream(
                "/classes/Repository.java.template")), UTF_8); var br = new BufferedReader(isr)) {
            var packageName
                = PACKAGE_TEMPLATE.formatted(projectInfo.getString(PACKAGE), DOMAIN, REPOSITORY);

            List<String> lines = new ArrayList<>(br.lines().toList());
            lines.add(0, TEMPLATE_2_STRING_COMMA.formatted(PACKAGE, packageName));

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

    public void createClass(JsonObject projectInfo,
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
        lines.add("import lombok.Builder;");
        lines.add("import lombok.AllArgsConstructor;");
        lines.add("import lombok.NoArgsConstructor;");
        lines.add(EMPTY);
        lines.add("@Setter");
        lines.add("@Getter");
        lines.add("@Builder");
        lines.add("@AllArgsConstructor");
        lines.add("@NoArgsConstructor");
        lines.add("public class %s {".formatted(className));
        AtomicReference<String> primaryKeyTypeRef = new AtomicReference<>();

        var fieldsDef = classDef.getJsonObject(FIELDS);
        //creating fields
        fieldsDef.keySet().forEach(field -> {
            var classTypeValue = fieldsDef.get(field);
            String classType = EMPTY;
            if (classTypeValue.getValueType() == JsonValue.ValueType.STRING) {
                classType = fieldsDef.getString(field);

            } else if (classTypeValue.getValueType() == JsonValue.ValueType.OBJECT) {
                var classObjectDef = classTypeValue.asJsonObject();
                var primaryKey = classObjectDef.getBoolean("primaryKey", false);
                classType = classObjectDef.getString("type");
                if (primaryKey) {
                    primaryKeyTypeRef.set(classType);
                }
            }
            lines.
                add(DEFINE_FIELD_PATTERN.formatted(StringUtils.repeat(SPACE, TAB_SIZE), classType,
                    field));
            if (importablesMap.containsKey(classType)) {
                lines.add(6, IMPORT_PACKAGE_TEMPLATE.formatted(importablesMap.get(classType)));
            }
        });

        lines.add("}");
        Optional.ofNullable(primaryKeyTypeRef.get()).ifPresentOrElse(primaryKeyType -> {
            try {
                FileClassUtil.writeClassFile(projectInfo, packageName, className, lines, DOMAIN);
                log.info("{} class Created", className);

                createRepository(projectInfo, repositoryPath, className, classDef, primaryKeyType);
            } catch (IOException ex) {
                log.error(ex.getMessage(), ex);
            }
        }, ()
            -> log.error("You must specify a field that is of type \"primaryKey\" for the {} class",
                className));

    }

    private void createRepository(JsonObject projectInfo,
                                  Path repositoryPath,
                                  String className,
                                  JsonObject classDef,
                                  String primaryKeyType) throws IOException {
        var packageName
            = PACKAGE_TEMPLATE.formatted(projectInfo.getString(PACKAGE), DOMAIN, REPOSITORY);
        var modelPackage = "%s.%s.%s.*".formatted(projectInfo.getString(PACKAGE), DOMAIN, MODEL);
        var fileName = "%sRepository".formatted(className);

        List<String> lines = new ArrayList<>();
        lines.add(TEMPLATE_2_STRING_COMMA.formatted(PACKAGE, packageName));
        lines.add(EMPTY);
        lines.add(IMPORT_PACKAGE_TEMPLATE.formatted(modelPackage));
        lines.add(EMPTY);
        lines.add("public interface %s extends IRepository<%s, %s> {".formatted(fileName, className,
            primaryKeyType));
        if (classDef.containsKey(FINDERS)) {
            classDef.getJsonObject(FINDERS).forEach((finderName, finderDescrip) -> {
                var finderBody = finderDescrip.asJsonObject();
                var parameters = finderBody.containsKey(PARAMETERS)
                             ? finderBody.getJsonArray(PARAMETERS).stream()
                        .map(param -> {
                            var clazz = ((JsonString) param).getString();
                            var parameterName = StringUtils.uncapitalize(clazz);
                            return TEMPLATE_2_STRING.formatted(clazz, parameterName);
                        })
                        .collect(Collectors.joining(COMMA))
                             : EMPTY;
                var isCollection = finderBody.getBoolean("isCollection", false);
                var returnType = "%s<%s>".formatted(isCollection ? "java.util.stream.Stream"
                                                : "java.util.Optional",
                    finderBody.getString(RETURN));
                var method = "%s %s finder%s(%s);".formatted(StringUtils.repeat(SPACE, TAB_SIZE),
                    returnType, StringUtils.capitalize(finderName), parameters);
                lines.add(method);
                lines.add(EMPTY);
            });

        }
        lines.add("}");
        if (importablesMap.containsKey(primaryKeyType)) {
            lines.add(2, IMPORT_PACKAGE_TEMPLATE.formatted(importablesMap.get(primaryKeyType)));
        }
        var repositoryModelPath = repositoryPath.resolve(fileName + ".java");
        Files.write(repositoryModelPath, lines);
    }

    private static class DomainModuleHandlerHolder {

        private static final DomainModuleHandler INSTANCE = new DomainModuleHandler();
    }
}
