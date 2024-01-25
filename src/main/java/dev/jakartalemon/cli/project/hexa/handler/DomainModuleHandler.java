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

import com.camucode.gen.DefinitionBuilder;
import com.camucode.gen.DefinitionBuilderWithMethods;
import com.camucode.gen.FieldDefinitionBuilder;
import com.camucode.gen.MethodDefinitionBuilder;
import com.camucode.gen.type.ClassType;
import com.camucode.gen.type.ClassTypeBuilder;
import com.camucode.gen.values.Modifier;
import dev.jakartalemon.cli.model.PomModel;
import dev.jakartalemon.cli.project.hexa.AddModelCommand;
import dev.jakartalemon.cli.util.Constants;
import dev.jakartalemon.cli.util.FileClassUtil;
import dev.jakartalemon.cli.util.HttpClientUtil;
import dev.jakartalemon.cli.util.JavaFileBuilder;
import dev.jakartalemon.cli.util.PomUtil;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static dev.jakartalemon.cli.util.Constants.ARTIFACT_ID;
import static dev.jakartalemon.cli.util.Constants.CLASSES_IMPORT_TEST;
import static dev.jakartalemon.cli.util.Constants.COLON;
import static dev.jakartalemon.cli.util.Constants.DOMAIN;
import static dev.jakartalemon.cli.util.Constants.DOT;
import static dev.jakartalemon.cli.util.Constants.FIELDS;
import static dev.jakartalemon.cli.util.Constants.FINDERS;
import static dev.jakartalemon.cli.util.Constants.GROUP_ID;
import static dev.jakartalemon.cli.util.Constants.INJECTS;
import static dev.jakartalemon.cli.util.Constants.JAR;
import static dev.jakartalemon.cli.util.Constants.JAVA;
import static dev.jakartalemon.cli.util.Constants.JAVA_VERSION;
import static dev.jakartalemon.cli.util.Constants.LOMBOK_DEPENDENCY;
import static dev.jakartalemon.cli.util.Constants.MAIN;
import static dev.jakartalemon.cli.util.Constants.MAVEN_COMPILER_RELEASE;
import static dev.jakartalemon.cli.util.Constants.MOCKITO_DEPENDENCY;
import static dev.jakartalemon.cli.util.Constants.MODEL;
import static dev.jakartalemon.cli.util.Constants.PACKAGE;
import static dev.jakartalemon.cli.util.Constants.PACKAGE_TEMPLATE;
import static dev.jakartalemon.cli.util.Constants.PARAMETERS;
import static dev.jakartalemon.cli.util.Constants.PRIMARY_KEY;
import static dev.jakartalemon.cli.util.Constants.REPOSITORY;
import static dev.jakartalemon.cli.util.Constants.RETURN;
import static dev.jakartalemon.cli.util.Constants.SRC;
import static dev.jakartalemon.cli.util.Constants.TEMPLATE_2_STRING_COMMA;
import static dev.jakartalemon.cli.util.Constants.TEST;
import static dev.jakartalemon.cli.util.Constants.USECASE;
import static dev.jakartalemon.cli.util.Constants.VERSION;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.EMPTY;

/**
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
@Slf4j
public class DomainModuleHandler {

    private static final String[] PRIMITIVE_TYPES = {
        "byte", "Byte",
        "short", "Short",
        "int", "Integer",
        "long", "Long",
        "float", "Float",
        "double", "Double",
        "boolean", "Boolean",
        "char", "Character",
        "String",
    };

    public static DomainModuleHandler getInstance() {
        return DomainModuleHandlerHolder.INSTANCE;
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
                .setResource(TEST)
                .build();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
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

    public void createClass(JsonObject projectInfo, String className, JsonObject classDef) {

        try {
            log.info("Creating {} class", className);
            AtomicReference<String> primaryKeyTypeRef = new AtomicReference<>();
            var packageName = PACKAGE_TEMPLATE.formatted(projectInfo.getString(PACKAGE), DOMAIN, MODEL);
            var fields = classDef.getJsonObject(FIELDS)
                .entrySet()
                .stream()
                .map(entry -> {
                    var fieldName = entry.getKey();
                    var fieldType = getType(entry.getValue());
                    var fieldDefinitionBuilder = FieldDefinitionBuilder.createBuilder()
                        .fieldName(fieldName)
                        .addModifier(Modifier.PRIVATE)
                        .setter(true)
                        .getter(true);
                    if (isPrimitiveType(fieldType)) {
                        fieldDefinitionBuilder.nativeType(fieldType);
                    } else {
                        var importableClass = importablesMap.getOrDefault(fieldType, fieldType);
                        var typePackageName = importableClass.contains(DOT) ? StringUtils.substringBeforeLast(
                            importableClass, DOT) : EMPTY;
                        var typeClassName = importableClass.contains(DOT) ? StringUtils.substringAfterLast(
                            importableClass,
                            DOT) : importableClass;
                        fieldDefinitionBuilder.classType(ClassType.createClassTypeWithPackageAndName(typePackageName,
                            typeClassName));
                    }
                    if (isPrimaryKey(entry.getValue())) {
                        primaryKeyTypeRef.set(fieldType);
                    }
                    return fieldDefinitionBuilder.build();
                }).toList();
            var definition = DefinitionBuilder.createClassBuilder(packageName, className)
                .addModifier(Modifier.PUBLIC)
                .addFields(fields)
                .build();

            var destinationPath = Paths.get(projectInfo.getString(DOMAIN), SRC, MAIN, JAVA);
            var javaFile = com.camucode.gen.JavaFileBuilder.createBuilder(definition, destinationPath).build();
            javaFile.writeFile();

            createRepository(projectInfo);

            Optional.ofNullable(primaryKeyTypeRef.get()).ifPresent(primaryKeyType -> {
                try {
                    createRepository(projectInfo, className, classDef,
                        primaryKeyType);
                } catch (IOException ex) {
                    log.error(ex.getMessage(), ex);
                }
            });
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }

    }

    private boolean isPrimitiveType(String typeName) {
        return ArrayUtils.contains(PRIMITIVE_TYPES, typeName);
    }

    private String getType(JsonValue jsonValue) {
        if (jsonValue.getValueType() == JsonValue.ValueType.STRING) {
            return ((JsonString) jsonValue).getString();
        }
        return jsonValue.asJsonObject().getString("type", EMPTY);
    }

    private boolean isPrimaryKey(JsonValue jsonValue) {
        if (jsonValue.getValueType() == JsonValue.ValueType.OBJECT) {
            return jsonValue.asJsonObject().getBoolean(PRIMARY_KEY, false);
        }
        return false;
    }

    private void createRepository(JsonObject projectInfo, String className, JsonObject classDef, String primaryKeyType)
        throws IOException {
        var modelPackage = "%s.%s.%s".formatted(projectInfo.getString(PACKAGE), DOMAIN, MODEL);
        var interfaceName = className + "Repository";
        var packageName
            = PACKAGE_TEMPLATE.formatted(projectInfo.getString(PACKAGE), DOMAIN, REPOSITORY);

        var definitionBuilder = DefinitionBuilder
            .createInterfaceBuilder(packageName, interfaceName)
            .addInterfaceExtend(
                ClassTypeBuilder.newBuilder().className("IRepository")
                    .addGeneric("T",
                        ClassTypeBuilder.newBuilder().className(className).packageName(modelPackage).build())
                    .addGeneric("ID", primaryKeyType)
                    .build()
            )
            .addModifier(Modifier.PUBLIC);
        if (classDef.containsKey(FINDERS)) {
            var methodsSet = classDef.getJsonObject(FINDERS).entrySet().stream().map(entry -> {

                var finderBody = entry.getValue().asJsonObject();
                var finderName = "find" + StringUtils.capitalize(entry.getKey());

                var methodBuilder = MethodDefinitionBuilder.createBuilder()
                    .name(finderName)
                    .isAbstract(true);
                var returnType = ClassTypeBuilder.newBuilder()
                    .addGeneric("T", finderBody.getString(RETURN));
                var isCollection = finderBody.getBoolean("isCollection", false);
                if (isCollection) {
                    returnType.packageName("java.util.stream").className("Stream");
                } else {
                    returnType.packageName("java.util").className("Optional");
                }
                if (finderBody.containsKey(PARAMETERS)) {
                    finderBody.getJsonArray(PARAMETERS).stream().map(JsonString.class::cast).map(JsonString::getString)
                        .forEach(parameter -> {
                            if (!isPrimitiveType(parameter)) {
                                ClassType parameterType = ClassTypeBuilder.newBuilder()
                                    .className(parameter)
                                    .packageName(modelPackage)
                                    .build();
                                String parameterName = StringUtils.uncapitalize(parameter);
                                methodBuilder.addParameter(parameterName, parameterType);
                            }
                        });
                }

                return methodBuilder.returnType(returnType.build()).build();
            }).collect(toSet());
            if (definitionBuilder instanceof DefinitionBuilderWithMethods definitionBuilderWithMethods) {
                definitionBuilderWithMethods.addMethods(methodsSet);
            }
        }
        if (definitionBuilder != null) {
            var definition = definitionBuilder.build();
            var destinationPath = Paths.get(projectInfo.getString(DOMAIN), SRC, MAIN, JAVA);
            var javaFile = com.camucode.gen.JavaFileBuilder.createBuilder(definition, destinationPath).build();
            javaFile.writeFile();
        }

    }

    private static class DomainModuleHandlerHolder {

        private static final DomainModuleHandler INSTANCE = new DomainModuleHandler();
    }
}
