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
    public static final String TYPE = "type";
    public static final String INJECTS = "injects";
    public static final String COLON = ":";
    public static final String COMMA = ",";
    public static final String COMMA_SPACE = ", ";
    public static final String VERSION = "version";
    public static final String APPLICATION = "application";
    public static final String DOMAIN = "domain";
    public static final String INFRASTRUCTURE = "infrastructure";
    public static final String REPOSITORY = "repository";
    public static final String PROJECT_GROUP_ID = "${project.groupId}";
    public static final String PROJECT_VERSION = "${project.version}";
    public static final String MAVEN_COMPILER_RELEASE = "maven.compiler.release";
    public static final String ENTITIES = "entities";
    public static final String ENTITY = "entity";
    public static final String POM = "pom";
    public static final String POM_XML = "pom.xml";
    public static final String JAR = "jar";
    public static final String WAR = "war";
    public static final String ADAPTERS = "adapters";
    public static final String USECASE = "usecase";
    public static final String TEMPLATE_2_STRING = "%s %s";
    public static final String TEMPLATE_2_STRING_COMMA = "%s %s;";
    public static final String MODEL = "model";
    public static final String MAPPER = "mapper";
    public static final String PRIMARY_KEY = "primaryKey";
    public static final String JAVA_VERSION = "17";
    public static final String ORG_MAPSTRUCT = "org.mapstruct";
    public static final String SRC = "src";
    public static final String MAIN = "main";
    public static final String NAME = "name";
    public static final String RESOURCES = "resources";
    public static final String TEST = "test";
    public static final String JAVA = "java";
    public static final String PACKAGE_TEMPLATE = "%s.%s.%s";
    public static final String PUBLIC = "public";
    public static final String RETURN = "return";
    public static final String IMPORT_PACKAGE_TEMPLATE = "import %s;";
    public static final int TAB_SIZE = 4;
    public static final String SCOPE = "scope";
    public static final String STRING_TYPE = "String";
    public static final String INTEGER_TYPE = "Integer";
    public static final String BOOLEAN_TYPE = "Boolean";
    public static final String COLUMN_ANNOTATION = "Column";
    public static final String JOIN_COLUMN_ANNOTATION = "JoinColumn";
    public static final String ANNOTATION_PROPS = "annotations-props";
    public static final char SLASH = '/';
    public static final String JAKARTA_LEMON_HOST_BASE = "https://jakartalemon.dev";
    public static final String JAKARTA_LEMON_CONFIG_URL
        = JAKARTA_LEMON_HOST_BASE + "/lemon-cli-config.json";
    public static final List<String> CLASSES_IMPORT_TEST = List.of(
        "org.junit.jupiter.api.extension.ExtendWith",
        "org.mockito.InjectMocks",
        "org.mockito.Mock",
        "org.mockito.junit.jupiter.MockitoExtension");

    public static final String DEPENDENCY = "dependency";
    public static final String PLUGIN = "plugin";
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
    public static final Map<String, String> INFRASTRUCTURE_DEPENDENCY = Map.of(
        GROUP_ID, PROJECT_GROUP_ID,
        ARTIFACT_ID, INFRASTRUCTURE,
        VERSION, PROJECT_VERSION
    );
    public static final String DOCS = "docs";
    public static final String DOT = ".";
    public static final String DEPENDENCY_GROUP_ID = "groupId";
    public static final String DEPENDENCY_ARTIFACT_ID = "artifactId";
    public static final String DEPENDENCY_VERSION = "version";
    public static final String G_KEY = "g";
    public static final String A_KEY = "a";
    public static final String LATEST_VERSION = "latestVersion";
    public static final String MANY_TO_ONE = "manyToOne";
    public static final String ONE_TO_ONE = "oneToOne";

    public static final String DEFINE_FIELD_PATTERN = "%s%s %s;";
    public static final String MAVEN_QUERY_PAYARA_MICRO
        = "q=g:fish.payara.extras+AND+a:payara-micro";
    public static final String MAVEN_QUERY_PERSISTENCE_API
        = "q=g:jakarta.persistence+AND+a:jakarta.persistence-api";
    public static final String MAVEN_QUERY_TRANSACTION_API
        = "q=g:jakarta.transaction+AND+a:jakarta.transaction-api";
    public static final String MAVEN_QUERY_JAKARTA_WS_RS_API
        = "q=g:jakarta.ws.rs+AND+a:jakarta.ws.rs-api";
    public static final String MAVEN_QUERY_RXJAVA
        = "q=g:io.reactivex.rxjava3+AND+a:rxjava";
    public static final String MAVEN_QUERY_LOMBOK
        = "q=g:org.projectlombok+AND+a:lombok";

    public static final String RESPONSE = "response";
    /**
     * It is the query string to look up the dependencies in the Maven repository.
     * <a href="https://central.sonatype.org/search/rest-api-guide/">REST API Maven Query</a>
     * Value {@code https://search.maven.org/solrsearch/select?q=}
     */

    public static final String QUERY_MAVEN_URL = "https://search.maven.org/solrsearch/select?";

    public static final String XMLNS = "xmlns";
    public static final String XMLNS_XSI = "xmlns:xsi";
    public static final String XMLNS_XSI_INSTANCE = "http://www.w3.org/2001/XMLSchema-instance";

    public static final String PRIVATE_FINAL_VARIABLE_TEMPLATE = "%sprivate final %s;";
    public static final String PRIVATE_VARIABLE_TEMPLATE = "%sprivate %s;";
    public static final String PRIVATE_VARIABLE_STATIC_FINAL_TEMPLATE
        = "%sprivate static final %s;";
    // JAKARTA SPECS
    public static final String JAKARTA_ENTERPRISE = "jakarta.enterprise";
    public static final String JAKARTA_ANNOTATION = "jakarta.annotation";
    public static final String JAKARTA_CDI_API_VERSION_KEY = "jakarta.enterprise.cdi-api.version";
    public static final String JAKARTA_ANNOTATION_API_VERSION_KEY
        = "jakarta.annotation-api.version";
    public static final String JAKARTA_ANNOTATION_API = "jakarta.annotation-api";
    public static final String JAKARTA_CDI_API = "jakarta.enterprise.cdi-api";
    public static final String JAKARTA_WS_RS = "jakarta.ws.rs";
    public static final String JAKARTA_WS_RS_API = "jakarta.ws.rs-api";
    public static final String JAKARTA_WS_RS_API_VERSION_KEY = "jakarta.ws.rs-api.version";
    public static final String JAKARTA_SERVLET = "jakarta.servlet";
    public static final String JAKARTA_SERVLET_VERSION_KEY = "jakarta.servlet.version";
    public static final String PROJECT_LOMBOK_VERSION_KEY = "org.projectlombok.version";

    public static final char SLASH_CHAR = '/';
    public static final char DOUBLE_QUOTES_CHAR = '"';
    public static final String PATH_PARAM = "path";
    public static final String CONFIGURATION = "configuration";
    public static final String META_INF = "META-INF";
    public static final String MAP_TO_MODEL = "mapToModel";
    //VERSION DEFAULT
    public static final String PAYARA_VERSION_DEFAULT = "6.2023.10";
    // OPENAPI
    public static final String OPEN_API_IN_QUERY = "query";
    public static final String OPEN_API_IN_PATH = "path";
    public static final String OPEN_API_IN = "in";
    public static final String OPEN_API_TYPE = "type";
    public static final String OPEN_API_EXAMPLES = "examples";
    public static final String OPEN_API_TYPES = "types";
    public static final String ANNOTATION_FIELD = "annotation";

    private Constants() {
    }

    /**
     *
     * @author Diego Silva <diego.silva at apuntesdejava.com>
     */
    public enum Archetype {
        MVC, HEXA, REST, JSF
    }

}
