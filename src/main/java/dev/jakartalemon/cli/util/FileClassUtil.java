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

import jakarta.json.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static dev.jakartalemon.cli.util.Constants.DOMAIN;
import static dev.jakartalemon.cli.util.Constants.JAVA;
import static dev.jakartalemon.cli.util.Constants.MAIN;
import static dev.jakartalemon.cli.util.Constants.SRC;

public class FileClassUtil {
    private FileClassUtil() {

    }

    public static void writeFile(JsonObject projectInfo, String packageName,
        String className, List<String> lines) throws IOException {
        var classPackage = Path.of(projectInfo.getString(DOMAIN), SRC, MAIN, JAVA);
        var packageNameList = packageName.split("\\.");
        for (var item : packageNameList) {
            classPackage = classPackage.resolve(item);
        }
        var classPath = classPackage.resolve("%s.java".formatted(className));
        Files.createDirectories(classPath.getParent());
        Files.write(classPath, lines);

    }
}