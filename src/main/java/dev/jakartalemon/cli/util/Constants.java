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

/**
 *
 * @author diego
 */
public class Constants {

    private Constants() {
    }
    public static final String GROUP_ID = "groupId";
    public static final String ARTIFACT_ID = "artifactId";
    public static final String VERSION = "version";
    public static final String APPLICATION = "application";
    public static final String DOMAIN = "domain";
    public static final String INFRASTRUCTURE = "infrastructure";
    public static final String REPOSITORY = "repository";
    public static final String PROJECT_GROUP_ID = "${project.groupId}";
    public static final String PROJECT_VERSION = "${project.version}";
    public static final String MAVEN_COMPILER_RELEASE = "maven.compiler.release";
    public static final String DTO = "dto";
    public static final String POM = "pom";
    public static final String JAR = "jar";
    public static final String PORTS = "ports";
    public static final String SERVICE = "service";
    public static final String MAPPER = "mapper";
    public static final String JAVA_VERSION = "17";
}
