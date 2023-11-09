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
import jakarta.json.Json;
import jakarta.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.json.JsonString;
import jakarta.json.JsonValue;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import static dev.jakartalemon.cli.util.Constants.*;

import dev.jakartalemon.cli.util.JavaFileBuilder;

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

    public static DomainModuleHandler getInstance() {
        return DomainModuleHandlerHolder.INSTANCE;
    }

    private final Map<String, String> importablesMap;

    private DomainModuleHandler() {

        importablesMap = HttpClientUtil.getConfigs(Constants.IMPORTABLES);

    }

    public void createUseCaseTestClass(JsonObject projectInfo,
                                       String className,
                                       JsonObject classDefinition) {
        try {
            var javaFileBuilder = new JavaFileBuilder();
            javaFileBuilder.setPackage(projectInfo.getString(PACKAGE), DOMAIN, USECASE);
            List<String> classesInject = getClassesInject(projectInfo, classDefinition,
                javaFileBuilder);
            CLASSES_IMPORT_TEST.forEach(javaFileBuilder::addImportClass);
            var classTestName = "%sTest".formatted(className);
            javaFileBuilder.addClassAnnotation("ExtendWith(MockitoExtension.class)");
            javaFileBuilder.setClassName(classTestName);

            classesInject.forEach(classInject -> {
                var variableName = StringUtils.uncapitalize(classInject);
                javaFileBuilder.addVariableDeclaration(classInject, variableName, "Mock");
            });

            var classNameInstance = StringUtils.uncapitalize(className);
            javaFileBuilder.addVariableDeclaration(className, classNameInstance, "InjectMocks")
                .setModulePath(projectInfo.getString(DOMAIN))
                .setFileName(classTestName)
                .setResource(TEST)
                .build();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public static Optional<Path> createDomainModule(Path projectPath,
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
            FileClassUtil.createJavaProjectStructure(parent, PACKAGE_TEMPLATE.formatted(
                packageName, DOMAIN, REPOSITORY));
            FileClassUtil
                .createJavaProjectStructure(parent,
                    PACKAGE_TEMPLATE.formatted(packageName, DOMAIN, MODEL));
            FileClassUtil.
                createJavaProjectStructure(parent,
                    PACKAGE_TEMPLATE.formatted(packageName, DOMAIN,
                        USECASE));
        });
        return pomPath;
    }

    private List<String> getClassesInject(JsonObject projectInfo,
                                          JsonObject classDefinition,
                                          JavaFileBuilder javaFileBuilder) {
        List<String> classesInject = new ArrayList<>();
        if (classDefinition.containsKey(INJECTS)) {
            var injects = classDefinition.getJsonArray(INJECTS);
            for (var i = 0; i < injects.size(); i++) {
                var inject = injects.getString(i);
                var importInject
                    = "%s.%s.%s.%s".formatted(projectInfo.getString(PACKAGE),
                        DOMAIN, REPOSITORY, inject);
                javaFileBuilder.addImportClass(importInject);
                classesInject.add(inject);
            }
        }
        return classesInject;
    }

    public void createUseCaseClass(JsonObject projectInfo,
                                   String className,
                                   JsonObject classDefinition) {
        try {
            log.info("Creating {} use case class", className);
            var javaFileBuilder = new JavaFileBuilder();
            javaFileBuilder.setPackage(projectInfo.getString(PACKAGE), DOMAIN, USECASE)
                .addImportClass("%s.%s.%s.*".formatted(projectInfo.getString(PACKAGE), DOMAIN,
                    MODEL)).setClassName(className);
            List<String> classesInject = getClassesInject(projectInfo, classDefinition,
                javaFileBuilder);
            if (!classesInject.isEmpty()) {
                classesInject.forEach(classInject -> {
                    var variableName = StringUtils.uncapitalize(classInject);
                    javaFileBuilder.addVariableDeclaration(classInject, variableName, null, true);
                });

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
                var parameters = method.entrySet().stream()
                    .filter(entry -> !entry.getKey().equals(RETURN))
                    .collect(Json::createObjectBuilder,
                        (accumulator, item) -> accumulator.add(item.getKey(), item.getValue()),
                        (accumulator, item) -> accumulator.add("last", item)).build();
                javaFileBuilder.addMethod(methodName, parameters, returnValue, defaultReturnValue);

            });

            javaFileBuilder
                .setModulePath(projectInfo.getString(DOMAIN))
                .setFileName(className)
                .build();
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

        try {
            log.info("Creating {} class", className);
            var javaFileBuilder = new JavaFileBuilder();
            javaFileBuilder.setPackage(projectInfo.getString(PACKAGE), DOMAIN, MODEL)
                .addImportClass("lombok.Getter")
                .addImportClass("lombok.Setter")
                .addImportClass("lombok.Builder")
                .addImportClass("lombok.AllArgsConstructor")
                .addImportClass("lombok.NoArgsConstructor")
                .addClassAnnotation("Setter")
                .addClassAnnotation("Getter")
                .addClassAnnotation("Builder")
                .addClassAnnotation("AllArgsConstructor")
                .addClassAnnotation("NoArgsConstructor")
                .setClassName(className);
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
                javaFileBuilder.addVariableDeclaration(classType, field, null);

            });
            javaFileBuilder
                .setModulePath(projectInfo.getString(DOMAIN))
                .setFileName(className)
                .build();
            Optional.ofNullable(primaryKeyTypeRef.get()).ifPresent(primaryKeyType -> {
                try {
                    createRepository(projectInfo, repositoryPath, className, classDef,
                        primaryKeyType);
                } catch (IOException ex) {
                    log.error(ex.getMessage(), ex);
                }
            });
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }

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
                    returnType,
                    StringUtils.capitalize(finderName),
                    parameters);
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
