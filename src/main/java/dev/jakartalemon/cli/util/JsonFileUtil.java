/*
 * Copyright 2023 Diego Silva <diego.silva at apuntesdejava.com>.
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

import jakarta.json.Json;
import jakarta.json.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Slf4j
public class JsonFileUtil {

    private JsonFileUtil() {
    }

    public static Optional<JsonObject> getProjectInfo() {
        var projectInfoPath = Path.of(Constants.PROJECT_INFO_JSON);
        return getFileJson(projectInfoPath);
    }

    public static Optional<JsonObject> getFileJson(Path path) {

        if (!Files.exists(path)) {
            log.error("File not found: {}", path.getFileName());
            return Optional.empty();
        }
        try (
            var projectInfoReader = Json.createReader(
                Files.newBufferedReader(path))) {

            return Optional.of(projectInfoReader.readObject());
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return Optional.empty();
        }
    }
}
