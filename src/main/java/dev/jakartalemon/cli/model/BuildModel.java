package dev.jakartalemon.cli.model;

import jakarta.json.JsonArray;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class BuildModel {
    private JsonArray plugins;
}
