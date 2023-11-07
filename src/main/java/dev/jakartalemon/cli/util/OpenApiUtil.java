package dev.jakartalemon.cli.util;

import io.swagger.v3.oas.models.media.Schema;
import java.util.Optional;

public class OpenApiUtil {

    public static String getType(
        Schema<?> schema) {
        return Optional.ofNullable(schema.getType()).
            orElseGet(() -> schema.getTypes() != null ? schema.getTypes().stream().findFirst().
            orElse(null) : null);
    }

    public static String openApiType2JavaType(String openApiType) {
        return switch (openApiType) {
            case "string" ->
                "String";
            default ->
                openApiType;
        };
    }
}
