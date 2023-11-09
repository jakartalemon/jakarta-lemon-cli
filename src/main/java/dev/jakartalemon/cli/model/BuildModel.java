package dev.jakartalemon.cli.model;

import jakarta.json.JsonArray;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Build element model from <code>pom.xml</code> file
 *
 * Contains an array of elements called <code>plugins</code>
 *
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
@Setter
@Getter
@Builder
public class BuildModel {

    private JsonArray plugins;
}
