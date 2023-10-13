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

import dev.jakartalemon.cli.util.JsonFileUtil;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.function.Consumer;

import static dev.jakartalemon.cli.util.Constants.COMPONENTS;
import static dev.jakartalemon.cli.util.Constants.PROPERTIES;
import static dev.jakartalemon.cli.util.Constants.SCHEMAS;

/**
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
@Slf4j
public class RestAdapterHandler {

    private JsonObject openApiJson;
    private JsonObject schemas;

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
        JsonFileUtil.getFileJson(file.toPath()).ifPresent(json -> this.openApiJson = json);
    }

    public void createComponents(Consumer<JsonObject> createClasses) {
        loadComponentsDefinitions();
        createClasses.accept(schemas);

    }


    private void loadComponentsDefinitions() {
        if (openApiJson.containsKey(COMPONENTS) &&
            openApiJson.getJsonObject(COMPONENTS).containsKey(SCHEMAS)) {
            var schemasDefinitions = openApiJson.getJsonObject(COMPONENTS).getJsonObject(SCHEMAS);
            var jsonBuilder = Json.createObjectBuilder();
            schemasDefinitions.forEach((key, body) -> {
                var bodyObject = body.asJsonObject();
                if (bodyObject.containsKey(PROPERTIES)) {
                    var propsEntrySet = bodyObject.getJsonObject(PROPERTIES).entrySet();
                    var props = Json.createObjectBuilder();
                    propsEntrySet.forEach((property) -> {
                        var type = property.getValue().asJsonObject().get("type");
                        var propertyType =
                            ((JsonString) (type.getValueType() == JsonValue.ValueType.ARRAY
                                ? type.asJsonArray().get(0)
                                : type)).getString();
                        props.add(property.getKey(), propertyType);
                    });
                    jsonBuilder.add(key, props);
                }
            });
            schemas = jsonBuilder.build();
        }
    }

    private static class RestAdapterHandlerHolder {

        private static final RestAdapterHandler INSTANCE = new RestAdapterHandler();
    }
}
