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

            return Optional.of( projectInfoReader.readObject());
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return Optional.empty();
        }
    }
}
