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

import com.camucode.gen.ClassDefinitionBuilder;
import com.camucode.gen.DefinitionBuilder;
import com.camucode.gen.JavaFileBuilder;
import com.camucode.gen.MethodDefinitionBuilder;
import com.camucode.gen.ParameterDefinition;
import com.camucode.gen.ParameterDefinitionBuilder;
import com.camucode.gen.type.AnnotationTypeBuilder;
import com.camucode.gen.type.ClassTypeBuilder;
import com.camucode.gen.type.NativeTypeBuilder;
import com.camucode.gen.values.Modifier;
import dev.jakartalemon.cli.model.ResourceDto;
import dev.jakartalemon.cli.util.Constants;
import dev.jakartalemon.cli.util.OpenApiUtil;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponses;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static dev.jakartalemon.cli.util.Constants.APPLICATION;
import static dev.jakartalemon.cli.util.Constants.COMMA_SPACE;
import static dev.jakartalemon.cli.util.Constants.JAVA;
import static dev.jakartalemon.cli.util.Constants.MAIN;
import static dev.jakartalemon.cli.util.Constants.MODEL;
import static dev.jakartalemon.cli.util.Constants.OPEN_API_EXAMPLES;
import static dev.jakartalemon.cli.util.Constants.OPEN_API_IN_PATH;
import static dev.jakartalemon.cli.util.Constants.OPEN_API_TYPE;
import static dev.jakartalemon.cli.util.Constants.OPEN_API_TYPES;
import static dev.jakartalemon.cli.util.Constants.PACKAGE;
import static dev.jakartalemon.cli.util.Constants.PACKAGE_TEMPLATE;
import static dev.jakartalemon.cli.util.Constants.RESOURCES;
import static dev.jakartalemon.cli.util.Constants.SLASH_CHAR;
import static dev.jakartalemon.cli.util.Constants.SRC;
import static dev.jakartalemon.cli.util.OpenApiUtil.getType;
import static io.swagger.models.Method.DELETE;
import static io.swagger.models.Method.GET;
import static io.swagger.models.Method.POST;
import static io.swagger.models.Method.PUT;

/**
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
@Slf4j
public class RestAdapterHandler {

    private JsonObject schemas;
    private OpenAPI openAPI;
    private Map<String, List<ResourceDto>> pathsGroup;

    private String modelPackage;

    private RestAdapterHandler() {
    }

    public static RestAdapterHandler getInstance() {
        return RestAdapterHandlerHolder.INSTANCE;
    }

    /**
     * Loads the definitions of the REST services that must be created.
     *
     * @param file JSON file with the definitions of the REST services in OpenAPI format
     */
    public void loadOpenApiDefinition(File file) {
        var swaggerParseResult
            = new OpenAPIParser().readLocation(file.getAbsolutePath(), null, null);
        this.openAPI = swaggerParseResult.getOpenAPI();
        Optional.ofNullable(swaggerParseResult.getMessages())
            .ifPresent(messages -> messages.forEach(log::error));

    }

    public void createComponents(Consumer<JsonObject> createClasses) {
        loadComponentsDefinitions();
        createClasses.accept(schemas);

    }

    private void loadComponentsDefinitions() {
        var jsonBuilder = Json.createObjectBuilder();
        openAPI.getComponents().getSchemas().forEach((schemaName, schema) -> {
            var schemaDefinition = Json.createObjectBuilder();
            Optional.ofNullable(schema.getProperties())
                .ifPresent(properties -> properties.forEach((propertyName, property) -> {
                var propertySchema = (Schema<?>) property;
                var propertyType = getType(propertySchema);
                var fieldContent = Json.createObjectBuilder().add(OPEN_API_TYPE, propertyType);
                Optional.ofNullable(propertySchema.getExamples()).ifPresent(examples -> {
                    fieldContent.add(OPEN_API_EXAMPLES, Json.createArrayBuilder(examples.stream().
                        map(
                            Object::toString).toList()));
                });
                Optional.ofNullable(propertySchema.getTypes()).ifPresent(types -> {
                    fieldContent.add(OPEN_API_TYPES, Json.createArrayBuilder(types));
                });

                schemaDefinition.add((String) propertyName, fieldContent);
            }));
            jsonBuilder.add(schemaName, schemaDefinition);

        });

        schemas = jsonBuilder.build();
    }

    public void createResourcesPaths(JsonObject projectInfo) {
        loadPathsDefinitions();
        log.info("Creating resources");
        pathsGroup.forEach((pathName, contents) -> createRestResourceBody(pathName, contents,
            projectInfo));
    }

    private void loadPathsDefinitions() {
        this.pathsGroup = new LinkedHashMap<>();

        openAPI.getPaths().forEach((pathName, pathItem) -> {
            var endPos = StringUtils.indexOf(pathName, SLASH_CHAR, 1);
            var prefix = endPos >= 0 ? StringUtils.substring(pathName, 0, endPos) : pathName;
            List<ResourceDto> paths = pathsGroup.getOrDefault(prefix, new ArrayList<>());
            paths.add(new ResourceDto(pathName, pathItem));

            pathsGroup.putIfAbsent(prefix, paths);
        });

    }

    public void createApplicationPath(JsonObject projectInfo) {
        try {
            var packageName = PACKAGE_TEMPLATE.formatted(projectInfo.getString(PACKAGE), APPLICATION, RESOURCES);
            var applicationPathAnnotation = AnnotationTypeBuilder.newBuilder()
                .classType(
                    ClassTypeBuilder.newBuilder()
                        .className("ApplicationPath")
                        .packageName("jakarta.ws.rs")
                        .build()
                )
                .addAttribute("value", "/api")
                .build();
            var applicationType = ClassTypeBuilder.newBuilder()
                .className("Application")
                .packageName("jakarta.ws.rs.core")
                .build();
            var applicationResourceDefinition = DefinitionBuilder
                .createClassBuilder(packageName, "ApplicationResource")
                .classExtended(applicationType)
                .addAnnotationType(applicationPathAnnotation)
                .addModifier(Modifier.PUBLIC)
                .build();
            var destinationPath = Paths.get(projectInfo.getString(APPLICATION), SRC, MAIN, JAVA);
            var javaFile = JavaFileBuilder.createBuilder(applicationResourceDefinition,
                destinationPath)
                .build();
            javaFile.writeFile();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void createRestResourceBody(String pathName,
                                        List<ResourceDto> contents,
                                        JsonObject projectInfo) {
        try {
            var packageName = PACKAGE_TEMPLATE.formatted(projectInfo.getString(PACKAGE), APPLICATION, RESOURCES);
            var pathResourceAnnotation = AnnotationTypeBuilder.newBuilder()
                .classType(
                    ClassTypeBuilder.newBuilder()
                        .packageName("jakarta.ws.rs")
                        .className("Path")
                        .build()
                )
                .addAttribute("value", pathName)
                .build();
            String className = StringUtils.capitalize(StringUtils.substringAfter(pathName, SLASH_CHAR)) + "Resource";
            var classResourceBuilder = DefinitionBuilder.createClassBuilder(packageName, className)
                .addAnnotationType(pathResourceAnnotation)
                .addModifier(Modifier.PUBLIC);

            contents.forEach(dto -> {
                var pathItem = dto.pathItem();
                var parameters = pathItem.getParameters();
                var subResourceName = StringUtils.substringAfter(dto.pathName(), pathName);
                Optional.ofNullable(pathItem.getGet()).
                    ifPresent(getOperation -> createMethod(getOperation,
                    (ClassDefinitionBuilder) classResourceBuilder,
                    GET.name(),
                    parameters,
                    subResourceName));
                Optional.ofNullable(pathItem.getPost()).
                    ifPresent(postOperation -> createMethod(postOperation,
                    (ClassDefinitionBuilder) classResourceBuilder, POST.name(),
                    parameters, subResourceName));
                Optional.ofNullable(pathItem.getDelete()).
                    ifPresent(
                        deleteOperation -> createMethod(deleteOperation,
                            (ClassDefinitionBuilder) classResourceBuilder, DELETE.name(),
                            parameters, subResourceName));
                Optional.ofNullable(pathItem.getPut()).
                    ifPresent(putOperation -> createMethod(putOperation,
                    (ClassDefinitionBuilder) classResourceBuilder,
                    PUT.name(),
                    parameters,
                    subResourceName));
            });

            var destinationPath = Paths.get(projectInfo.getString(APPLICATION), SRC, MAIN, JAVA);
            var javaFile = JavaFileBuilder.createBuilder(classResourceBuilder.build(), destinationPath)
                .build();
            javaFile.writeFile();

        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void createMethod(Operation operation,
                              ClassDefinitionBuilder classResourceBuilder, String method,
                              List<Parameter> parameters,
                              String resourceName) {
        var methodDefinitionBuilder = MethodDefinitionBuilder
            .createBuilder()
            .addModifier(Modifier.PUBLIC)
            .name(operation.getOperationId());

        methodDefinitionBuilder.addAnnotationType(
            AnnotationTypeBuilder.newBuilder()
                .classType(
                    ClassTypeBuilder.newBuilder()
                        .packageName("jakarta.ws.rs")
                        .className(method)
                        .build()
                )
                .build()
        );
        Optional.ofNullable(operation.getResponses()).
            ifPresent(responses -> {
                var producesAnnotationBuilder = AnnotationTypeBuilder.newBuilder()
                    .classType(
                        ClassTypeBuilder.newBuilder()
                            .className("Produces")
                            .packageName("jakarta.ws.rs")
                            .build()
                    );

                responses.forEach((key, response) -> {
                    Content content = response.getContent();
                    String annotation = StringUtils.join(content.keySet().stream().
                        map("%s"::formatted).toList(),
                        Constants.COMMA);
                    producesAnnotationBuilder.addAttribute("value", annotation);
                });
                methodDefinitionBuilder.addAnnotationType(producesAnnotationBuilder.build());
            });
        Optional.ofNullable(operation.getRequestBody())
            .ifPresent(requestBody -> {
                var consumesAnnotationBuilder = AnnotationTypeBuilder.newBuilder()
                    .classType(
                        ClassTypeBuilder.newBuilder()
                            .className("Consumes")
                            .packageName("jakarta.ws.rs")
                            .build()
                    );
                Optional.ofNullable(requestBody.getContent()).ifPresent(content -> {
                    String annotation = StringUtils.join(content.keySet().stream().
                        map(("%s")::formatted).toList(),
                        Constants.COMMA);
                    consumesAnnotationBuilder.addAttribute("value", annotation);
                });
                methodDefinitionBuilder.addAnnotationType(consumesAnnotationBuilder.build());

            });
        var params = createParametersToList(parameters, resourceName, operation, methodDefinitionBuilder);
        methodDefinitionBuilder.parameters(params);
        var body = new StringBuilder();
        Optional.ofNullable(operation.getResponses()).ifPresent(responses -> createResponses(
            responses, classResourceBuilder, body));
        classResourceBuilder.addMethod(methodDefinitionBuilder.build());
//        javaFileBuilder.addMethod(operationId, params.build(), "void", null, annotations, body.
//                toString())
    }

    private Collection<ParameterDefinition> createParametersToList(List<Parameter> parameters,
                                                                   String resourceName,
                                                                   Operation operation,
                                                                   MethodDefinitionBuilder methodDefinitionBuilder) {
        var params = convertParamsToList(parameters, resourceName, methodDefinitionBuilder);
        Optional.ofNullable(operation.getRequestBody()).ifPresent(requestBody -> {
            var paramsRequestBody
                = requestBody.getContent().values().stream().map(mediaType -> StringUtils.
                substringAfterLast(
                    mediaType.getSchema().get$ref(),
                    SLASH_CHAR)).toList();
            paramsRequestBody.forEach(item -> {
//                javaFileBuilder.addImportClass(modelPackage + '.' + item);
                //               params.add(StringUtils.uncapitalize(item), item);
            });
        });
        return params;
    }

    private void createResponses(ApiResponses responses,
                                 ClassDefinitionBuilder classResourceBuilder, StringBuilder body) {
        responses.forEach((key, response) -> {
            if (response.getContent() != null) {
                var content = response.getContent();
                content.values().forEach(mediaType -> {
                    if (mediaType.getSchema() != null
                        && mediaType.getSchema().get$ref() != null) {
                        var ref = mediaType.getSchema().get$ref();
                        var schemaName = StringUtils.substringAfterLast(ref, SLASH_CHAR);
                        //javaFileBuilder.addImportClass(modelPackage + '.' + schemaName);
                        var schemaFields = schemas.get(schemaName);
                        List<String> values = new ArrayList<>();
                        schemaFields.asJsonObject().values().forEach(value -> {
                            if (value.asJsonObject().containsKey(OPEN_API_EXAMPLES)) {
                                value.asJsonObject().getJsonArray(OPEN_API_EXAMPLES).stream()
                                    .map(val -> ((JsonString) val).getString())
                                    .findAny()
                                    .ifPresent(values::add);
                            }
                        });
                        var constructorsParameters = String.join(COMMA_SPACE, values);
                        body.append("Flowable.just(new %s(%s))".formatted(schemaName,
                            constructorsParameters));
                        body.append(".subscribe(asyncResponse::resume);");
                    }
                });
            }
        });
    }

    private Collection<ParameterDefinition> convertParamsToList(List<Parameter> parameters,
                                                                String resourceName,
                                                                MethodDefinitionBuilder methodDefinitionBuilder) {
        ParameterDefinitionBuilder parameterDefinitionBuilder = ParameterDefinitionBuilder.newBuilder();
        Optional.ofNullable(parameters).
            ifPresent(parametersList -> parametersList.forEach(parameter -> {
            String entryName = parameter.getName();
            String type
                = OpenApiUtil.openApiType2JavaType(OpenApiUtil.getType(parameter.getSchema()));
            if (StringUtils.equals(parameter.getIn(), OPEN_API_IN_PATH)) {
                methodDefinitionBuilder.addAnnotationType(
                    AnnotationTypeBuilder.newBuilder()
                        .addAttribute("value", resourceName)
                        .classType(
                            ClassTypeBuilder.newBuilder()
                                .className("Path")
                                .packageName("jakarta.ws.rs")
                                .build()
                        )
                        .build()
                );
                parameterDefinitionBuilder.addAnnotation(
                    AnnotationTypeBuilder.newBuilder()
                        .classType(
                            ClassTypeBuilder.newBuilder()
                                .packageName("jakarta.ws.rs")
                                .className("PathParam")
                                .build()
                        )
                        .addAttribute("value", entryName)
                        .build()
                );

            }
            parameterDefinitionBuilder.parameterName(entryName)
                .parameterType(NativeTypeBuilder.newBuilder().name(type).build());
            //params.put(entryName, paramDetail);

        }));
        return Set.of(parameterDefinitionBuilder.build());
    }

    public void setModelPackage(JsonObject projectInfo) {
        modelPackage = Constants.PACKAGE_TEMPLATE.formatted(projectInfo.getString(PACKAGE),
            APPLICATION, MODEL);

    }

    private static class RestAdapterHandlerHolder {

        private static final RestAdapterHandler INSTANCE = new RestAdapterHandler();
    }
}
