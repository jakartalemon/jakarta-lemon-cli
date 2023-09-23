/*
 * Copyright 2022 Apuntes de Java.
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

import jakarta.json.Json;
import jakarta.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import jakarta.json.JsonReader;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;

import static dev.jakartalemon.cli.util.Constants.A_KEY;
import static dev.jakartalemon.cli.util.Constants.DEPENDENCY_ARTIFACT_ID;
import static dev.jakartalemon.cli.util.Constants.DEPENDENCY_GROUP_ID;
import static dev.jakartalemon.cli.util.Constants.DEPENDENCY_VERSION;
import static dev.jakartalemon.cli.util.Constants.DOCS;
import static dev.jakartalemon.cli.util.Constants.G_KEY;
import static dev.jakartalemon.cli.util.Constants.LATEST_VERSION;
import static dev.jakartalemon.cli.util.Constants.QUERY_MAVEN_URL;
import static dev.jakartalemon.cli.util.Constants.RESPONSE;

@Slf4j
public class DependenciesUtil {
    private DependenciesUtil() {

    }

    /**
     * Gets the latest version of a dependency given by the query string.
     *
     * @param query Query string that is sent to the Maven API
     * @return JSON object with the dependency found, or {@link Optional#empty()} if not found.
     * @throws java.lang.InterruptedException
     * @throws java.io.IOException
     * @throws java.net.URISyntaxException
     */
    public static Optional<JsonObject> getLastVersionDependency(String query)
        throws InterruptedException, IOException, URISyntaxException {
        String uri = QUERY_MAVEN_URL + query;
        var jsonResp = HttpClientUtil.getJson(uri, JsonReader::readObject);
        var responseJson = jsonResp.getJsonObject(RESPONSE);
        var docsJson = responseJson.getJsonArray(DOCS);
        var docJson = docsJson.get(0).asJsonObject();
        return Optional.of(Json.createObjectBuilder()
            .add(DEPENDENCY_GROUP_ID, docJson.getString(G_KEY))
            .add(DEPENDENCY_ARTIFACT_ID, docJson.getString(A_KEY))
            .add(DEPENDENCY_VERSION, docJson.getString(LATEST_VERSION))
            .build());

    }
}
