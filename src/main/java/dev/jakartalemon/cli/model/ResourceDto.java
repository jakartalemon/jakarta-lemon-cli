package dev.jakartalemon.cli.model;

import io.swagger.v3.oas.models.PathItem;

public record ResourceDto(String pathName, PathItem pathItem) {
}
