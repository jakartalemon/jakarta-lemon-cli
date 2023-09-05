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
package dev.jakartalemon.cli.util;

import java.util.List;
import java.util.Map;

/**
 * @author diego
 */
public class Constants {

    public static final String IMPORTABLES = "importables";
    public static final String DATABASES = "databases";
    public static final String GROUP_ID = "groupId";
    public static final String PARAMETERS = "parameters";
    public static final String PACKAGE = "package";
    public static final String FINDERS = "finders";
    public static final String ARTIFACT_ID = "artifactId";
    public static final String FIELDS = "fields";
    public static final String INJECTS = "injects";
    public static final String COLON = ":";
    public static final String COMMA = ",";
    public static final String VERSION = "version";
    public static final String APPLICATION = "application";
    public static final String DOMAIN = "domain";
    public static final String INFRASTRUCTURE = "infrastructure";
    public static final String REPOSITORY = "repository";
    public static final String PROJECT_GROUP_ID = "${project.groupId}";
    public static final String PROJECT_VERSION = "${project.version}";
    public static final String MAVEN_COMPILER_RELEASE = "maven.compiler.release";
    public static final String ENTITIES = "entities";
    public static final String POM = "pom";
    public static final String JAR = "jar";
    public static final String ADAPTERS = "adapters";
    public static final String USECASE = "usecase";
    public static final String TEMPLATE_2_STRING="%s %s";
    public static final String TEMPLATE_2_STRING_COMMA="%s %s;";
    public static final String MODEL = "model";
    public static final String MAPPER = "mapper";
    public static final String JAVA_VERSION = "17";
    public static final String ORG_MAPSTRUCT = "org.mapstruct";
    public static final String SRC = "src";
    public static final String MAIN = "main";
    public static final String TEST = "test";
    public static final String JAVA = "java";
    public static final String PACKAGE_TEMPLATE = "%s.%s.%s";
    public static final String PUBLIC = "public";
    public static final String RETURN = "return";
    public static final String IMPORT_PACKAGE_TEMPLATE = "import %s;";
    public static final int TAB_SIZE = 4;
    public static final String SCOPE = "scope";
    public static final String JAKARTA_LEMON_HOST_BASE = "https://jakartalemon.dev";
    public static final String JAKARTA_LEMON_CONFIG_URL
        = JAKARTA_LEMON_HOST_BASE + "/lemon-cli-config.json";
    public static final List<String> CLASSES_IMPORT_TEST = List.of(
        "org.junit.jupiter.api.extension.ExtendWith",
        "org.mockito.InjectMocks",
        "org.mockito.Mock",
        "org.mockito.junit.jupiter.MockitoExtension");

    public static final String DEPENDENCY = "dependency";
    public static final String PROJECT_INFO_JSON = "project_info.json";
    public static final Map<String, String> LOMBOK_DEPENDENCY = Map.of(
        GROUP_ID, "org.projectlombok",
        ARTIFACT_ID, "lombok",
        VERSION, "${org.projectlombok.version}"
    );
    public static final Map<String, String> MOCKITO_DEPENDENCY = Map.of(
        GROUP_ID, "org.mockito",
        ARTIFACT_ID, "mockito-junit-jupiter",
        VERSION, "${mockito.junit.jupiter.version}",
        SCOPE, "test"
    );
    public static final Map<String, String> JAKARTA_INJECT_DEPENDENCY = Map.of(
        GROUP_ID, "jakarta.inject",
        ARTIFACT_ID, "jakarta.inject-api",
        VERSION, "2.0.1",
        SCOPE, "provided"
    );
    public static final String DOCS = "docs";
    public final static String DEPENDENCY_GROUP_ID = "groupId";
    public final static String DEPENDENCY_ARTIFACT_ID = "artifactId";
    public final static String DEPENDENCY_VERSION = "version";
    public static final String G_KEY = "g";
    public static final String A_KEY = "a";
    public static final String LATEST_VERSION = "latestVersion";

    public static final String RESPONSE = "response";
    /**
     * It is the query string to look up the dependencies in the Maven repository.
     * <a href="https://central.sonatype.org/search/rest-api-guide/">REST API Maven Query</a>
     * Value {@code https://search.maven.org/solrsearch/select?q=}
     */

    public static final String QUERY_MAVEN_URL = "https://search.maven.org/solrsearch/select?";
    private Constants() {
    }
}
