/*
 * Copyright 2023 Diego Silva <diego.silva at apuntesdejava.com>.
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

import dev.jakartalemon.cli.model.PomModel;
import dev.jakartalemon.cli.util.DocumentXmlUtil.ElementBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import lombok.extern.slf4j.Slf4j;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static dev.jakartalemon.cli.util.Constants.ARTIFACT_ID;
import static dev.jakartalemon.cli.util.Constants.GROUP_ID;
import static dev.jakartalemon.cli.util.Constants.VERSION;

/**
 * @author Diego Silva <diego.silva at apuntesdejava.com>
 */
@Slf4j
public class PomUtil {

    private PomUtil() {
    }

    public static PomUtil getInstance() {
        return PomUtilHolder.INSTANCE;
    }

    /**
     * @param modulePath the value of modulePath
     * @param pomModel
     * @return
     */
    public Optional<Path> createPom(Path modulePath,
        PomModel pomModel) {
        try {
            Files.createDirectories(modulePath);
            var pomPath = modulePath.resolve("pom.xml");
            var pomXml = DocumentXmlUtil.newDocument();
            var projectElemBuilder = ElementBuilder.newInstance("project")
                .addAttribute("xmlns", "http://maven.apache.org/POM/4.0.0")
                .addAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance")
                .addAttribute("xsi:schemaLocation",
                    "http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0"
                        + ".xsd");
            Optional.ofNullable(pomModel.getModelVersion()).ifPresent(
                modelVersion -> projectElemBuilder.addChild(ElementBuilder.newInstance(
                        "modelVersion")
                    .setTextContent(modelVersion)));
            //creando groupId
            Optional.ofNullable(pomModel.getGroupId()).ifPresent(groupId -> projectElemBuilder.
                addChild(
                    ElementBuilder.newInstance(GROUP_ID)
                        .setTextContent(groupId)));
            //creando parent
            Optional.ofNullable(pomModel.getParent()).ifPresent(parent -> {
                var parentElementBuilder = ElementBuilder.newInstance("parent");
                projectElemBuilder.addChild(parentElementBuilder);
                parent.forEach(
                    (key, value) -> parentElementBuilder.addChild(ElementBuilder.newInstance(key)
                        .setTextContent(value)));
            });
            Optional.ofNullable(pomModel.getVersion()).ifPresent(version -> projectElemBuilder.
                addChild(
                    ElementBuilder.newInstance(VERSION)
                        .setTextContent(version)));
            projectElemBuilder
                .addChild(ElementBuilder.newInstance(ARTIFACT_ID).setTextContent(pomModel.
                    getArtifactId()))
                .addChild(ElementBuilder.newInstance("packaging").setTextContent(pomModel.
                    getPackaging()));
            //creando modules
            Optional.ofNullable(pomModel.getModules()).ifPresent(modules -> {
                var modulesElementBuilder = ElementBuilder.newInstance("modules");
                projectElemBuilder.addChild(modulesElementBuilder);
                modules.forEach(module -> modulesElementBuilder.addChild(
                    ElementBuilder.newInstance("module").setTextContent(module)
                ));
            });
            //creando dependencias
            Optional.ofNullable(pomModel.getDependencies()).ifPresent(dependencies -> {
                var dependenciesElementBuilder = ElementBuilder.newInstance("dependencies");
                projectElemBuilder.addChild(dependenciesElementBuilder);

                dependencies.forEach(dependency -> {
                    var dependencyElementBuilder = ElementBuilder.newInstance("dependency");
                    dependenciesElementBuilder.addChild(dependencyElementBuilder);

                    dependency.forEach((key, value) -> dependencyElementBuilder.addChild(
                        ElementBuilder.newInstance(key)
                            .setTextContent(value)));
                });
            });
            //creando properties
            Optional.ofNullable(pomModel.getProperties()).ifPresent(properties -> {
                var propsElementBuilder = ElementBuilder.newInstance("properties");
                projectElemBuilder.addChild(propsElementBuilder);
                properties.forEach(
                    (key, value) -> propsElementBuilder.addChild(ElementBuilder.newInstance(key)
                        .setTextContent(value)));
            });
            //creado seccion build
            Optional.ofNullable(pomModel.getBuildModel()).ifPresent(build -> {
                var buildElem = ElementBuilder.newInstance("build");
                projectElemBuilder.addChild(buildElem);
                Optional.ofNullable(build.getPlugins()).ifPresent(plugins -> {
                    var pluginsElem = ElementBuilder.newInstance("plugins");
                    buildElem.addChild(pluginsElem);
                    plugins.stream().map(JsonValue::asJsonObject).forEach(plugin -> {
                        var pluginElem = ElementBuilder.newInstance("plugin");
                        pluginsElem.addChild(pluginElem);
                        insertValues(plugin, pluginElem);

                    });

                });

            });
            pomXml.appendChild(projectElemBuilder.build(pomXml));
            DocumentXmlUtil.saveDocument(pomPath, pomXml);
            log.info("{} saved", pomPath.toAbsolutePath());
            return Optional.of(pomPath);
        } catch (IOException | ParserConfigurationException ex) {
            log.error(ex.getMessage(), ex);
        }
        return Optional.empty();

    }

    private void insertValues(JsonObject jsonValues, ElementBuilder elementBuilder) {

        var entrySet = jsonValues.entrySet();
        entrySet.forEach(entry -> {
            var element = ElementBuilder.newInstance(entry.getKey());
            elementBuilder.addChild(element);
            if (entry.getValue().getValueType() == JsonValue.ValueType.STRING)
                element.setTextContent(jsonValues.getString(entry.getKey()));
            else if (entry.getValue().getValueType() == JsonValue.ValueType.OBJECT) {
                insertValues(jsonValues.getJsonObject(entry.getKey()), element);

            }
        });
    }

    public void createJavaProjectStructure(Path sourcePath, String... packagesName) {
        try {
            var created
                =
                Files.createDirectories(sourcePath.resolve("src").resolve("main").resolve("java"));
            Files.createDirectories(sourcePath.resolve("src").resolve("main").resolve("resources"));
            Files.createDirectories(sourcePath.resolve("src").resolve("test").resolve("java"));
            Files.createDirectories(sourcePath.resolve("src").resolve("test").resolve("resources"));
            for (var packageName : packagesName) {
                var packagesDir = packageName.split("\\.");
                var packagePath = created;
                for (var packageDir : packagesDir) {
                    packagePath = packagePath.resolve(packageDir);
                }
                Files.createDirectories(packagePath);
            }
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }

    }

    private static class PomUtilHolder {

        private static final PomUtil INSTANCE = new PomUtil();
    }
}
