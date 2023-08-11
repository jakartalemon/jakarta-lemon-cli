/*
 * Copyright 2023 diego.
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
package dev.jakartalemon.cli.model;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author diego
 */
@Setter
@Getter
@Builder
public class PomModel {

    @Builder.Default
    private String modelVersion = "4.0.0";
    private Map<String, String> parent;
    private String groupId;
    private String artifactId;
    private String version;
    private String packaging;
    private List<Map<String, String>> dependencies;
    private Map<String, String> properties;
    private List<String> modules;

    private BuildModel buildModel;

}
