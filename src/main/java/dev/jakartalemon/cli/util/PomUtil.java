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

import dev.jakartalemon.cli.util.DocumentXmlUtil.ElementBuilder;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Diego Silva <diego.silva at apuntesdejava.com>
 */
public class PomUtil {

    private static final Logger LOGGER = Logger.getLogger(PomUtil.class.getName());

    private PomUtil() {
    }

    public static PomUtil getInstance() {
        return PomUtilHolder.INSTANCE;
    }

    public void createPom(Path modulePath, Map<String, String> parentMap, String artifactId,
        String packaging, List<Map<String, String>> dependenciesList,
        Map<String, String> propertiesMap) {
        createPom(modulePath, null, parentMap, null, artifactId, null, packaging, dependenciesList,
            propertiesMap);
    }

    /**
     *
     * @param modulePath       the value of modulePath
     * @param modulesList          the value of modulesList
     * @param parentMap      the value of parentMap
     * @param groupId        the value of groupId
     * @param artifactId       the value of artifactId
     * @param version        the value of version
     * @param packaging the value of packaging
     * @param dependenciesList    the value of dependenciesList
     * @param propertiesMap the value of propertiesMap
     */
    public void createPom(Path modulePath, List<String> modulesList, Map<String, String> parentMap, String groupId, String artifactId, String version, String packaging, List<Map<String, String>> dependenciesList, Map<String, String> propertiesMap) {
        try {
            Files.createDirectories(modulePath);
            var pomPath = modulePath.resolve("pom.xml");
            var pomXml = DocumentXmlUtil.newDocument();
            var projectElemBuilder = ElementBuilder.newInstance("project")
                .addAttribute("xmlns", "http://maven.apache.org/POM/4.0.0")
                .addAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance")
                .addAttribute("xsi:schemaLocation",
                    "http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd")
                .addChild(ElementBuilder.newInstance("modelVersion")
                    .setTextContent("4.0.0"));
            //creando groupId
            Optional.ofNullable(groupId).ifPresent(grpId -> projectElemBuilder.addChild(ElementBuilder.newInstance("groupId")
                .setTextContent(grpId)));
            //creando parent
            Optional.ofNullable(parentMap).ifPresent(parent -> {
                var parentElementBuilder = ElementBuilder.newInstance("parent");
                projectElemBuilder.addChild(parentElementBuilder);
                parent.forEach(
                    (key, value) -> parentElementBuilder.addChild(ElementBuilder.newInstance(key)
                        .setTextContent(value)));
            });
            Optional.ofNullable(version).ifPresent(ver -> projectElemBuilder.addChild(ElementBuilder.newInstance("version")
                .setTextContent(ver)));
            projectElemBuilder
                .addChild(ElementBuilder.newInstance("artifactId").setTextContent(artifactId))
                .addChild(ElementBuilder.newInstance("packaging").setTextContent(packaging));
            //creando modules
            Optional.ofNullable(modulesList).ifPresent(modules -> {
                var modulesElementBuilder = ElementBuilder.newInstance("modules");
                projectElemBuilder.addChild(modulesElementBuilder);
                modules.forEach(module -> modulesElementBuilder.addChild(
                    ElementBuilder.newInstance("module").setTextContent(module)
                ));
            });
            //creando dependencias
            Optional.ofNullable(dependenciesList).ifPresent(dependencies -> {
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
            Optional.ofNullable(propertiesMap).ifPresent(properties -> {
                var propsElementBuilder = ElementBuilder.newInstance("properties");
                projectElemBuilder.addChild(propsElementBuilder);
                properties.forEach(
                    (key, value) -> propsElementBuilder.addChild(ElementBuilder.newInstance(key)
                        .setTextContent(value)));
            });
            pomXml.appendChild(projectElemBuilder.build(pomXml));
            DocumentXmlUtil.saveDocument(pomPath, pomXml);
            LOGGER.info("%s saved".formatted(pomPath.toAbsolutePath()));
        } catch (IOException | ParserConfigurationException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }

    }

    private static class PomUtilHolder {

        private static final PomUtil INSTANCE = new PomUtil();
    }
}
