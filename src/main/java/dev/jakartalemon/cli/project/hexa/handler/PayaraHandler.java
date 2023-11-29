/*
 * Copyright 2023 Diego Silva mailto:diego.silva@apuntesdejava.com.
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
package dev.jakartalemon.cli.project.hexa.handler;

import static dev.jakartalemon.cli.util.Constants.APPLICATION;
import static dev.jakartalemon.cli.util.Constants.ARTIFACT_ID;
import static dev.jakartalemon.cli.util.Constants.CONFIGURATION;
import static dev.jakartalemon.cli.util.Constants.GROUP_ID;
import static dev.jakartalemon.cli.util.Constants.JAVA_VERSION;
import static dev.jakartalemon.cli.util.Constants.MAVEN_COMPILER_RELEASE;
import static dev.jakartalemon.cli.util.Constants.MAVEN_QUERY_PAYARA_MICRO;
import static dev.jakartalemon.cli.util.Constants.PAYARA_VERSION_DEFAULT;
import static dev.jakartalemon.cli.util.Constants.VERSION;
import dev.jakartalemon.cli.util.DependenciesUtil;
import dev.jakartalemon.cli.util.PomUtil;
import jakarta.json.Json;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 *
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
public class PayaraHandler {

    private PayaraHandler() {
    }

    public static PayaraHandler getInstance() {
        return PayaraHandlerHolder.INSTANCE;
    }

    public void addPayaraMicroPlugin() throws InterruptedException, IOException, URISyntaxException {
        var payaraVersion = DependenciesUtil.
            getLastVersionDependency(MAVEN_QUERY_PAYARA_MICRO)
            .map(dependency -> dependency.getString(VERSION))
            .orElse(PAYARA_VERSION_DEFAULT);
        PomUtil.getInstance()
            .addProperty(Json.createObjectBuilder()
                .add("payara.version", payaraVersion)
                .build());
        PomUtil.getInstance()
            .addProperty(Json.createObjectBuilder()
                .add(MAVEN_COMPILER_RELEASE, JAVA_VERSION)
                .build(), APPLICATION
            )
            .addPlugin(Json.createObjectBuilder()
                .add(GROUP_ID, "fish.payara.maven.plugins")
                .add(ARTIFACT_ID, "payara-micro-maven-plugin")
                .add(VERSION, "2.0")
                .add(CONFIGURATION, Json.createObjectBuilder()
                    .add("payaraVersion", "${payara.version}")
                    .add("artifactItem",
                        Json.createObjectBuilder()
                            .add(GROUP_ID, "fish.payara.extras")
                            .add(ARTIFACT_ID, "payara-micro")
                            .add(VERSION, "${payara.version}")
                    )
                    .add("deployWar", "false")
                )
                .build(), APPLICATION);

    }

    private static class PayaraHandlerHolder {

        private static final PayaraHandler INSTANCE = new PayaraHandler();
    }
}
