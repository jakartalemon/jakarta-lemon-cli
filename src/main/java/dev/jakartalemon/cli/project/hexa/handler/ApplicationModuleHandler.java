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

import dev.jakartalemon.cli.model.PomModel;
import dev.jakartalemon.cli.util.Constants;
import dev.jakartalemon.cli.util.PomUtil;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
@Slf4j
public class ApplicationModuleHandler {
    
    private ApplicationModuleHandler() {
    }
    
    public static ApplicationModuleHandler getInstance() {
        return ApplicationModuleHandlerHolder.INSTANCE;
    }

    public Optional<Path> createApplicationModule(Path projectPath,
                                                  String groupId,
                                                  String artifactId,
                                                  String version,
                                                  String packageName) {
        PomModel.PomModelBuilder modulePom = PomModel.builder().
            parent(Map.of(Constants.GROUP_ID, groupId, Constants.ARTIFACT_ID, artifactId,
            Constants.VERSION, version)).artifactId(Constants.APPLICATION).packaging(Constants.JAR).
            dependencies(List.of(Map.of(Constants.GROUP_ID, Constants.PROJECT_GROUP_ID,
            Constants.ARTIFACT_ID, Constants.DOMAIN, Constants.VERSION, Constants.PROJECT_VERSION),
            Constants.JAKARTA_INJECT_DEPENDENCY));
        Optional<Path> pomPath = PomUtil.getInstance().
            createPom(projectPath.resolve(Constants.APPLICATION), modulePom.build());
        pomPath.ifPresent(pom -> {
            log.debug("application created at {}", pom.toAbsolutePath());
            /*var parent = pom.getParent();
            PomUtil.getInstance().createJavaProjectStructure(parent, PACKAGE_TEMPLATE.formatted(
            packageName, APPLICATION, REPOSITORY));
            PomUtil.getInstance()
            .createJavaProjectStructure(parent,
            PACKAGE_TEMPLATE.formatted(packageName, APPLICATION, MODEL));
            PomUtil.getInstance().
            createJavaProjectStructure(parent,
            PACKAGE_TEMPLATE.formatted(packageName, APPLICATION, USECASE));*/
        });
        return pomPath;
    }
    
    private static class ApplicationModuleHandlerHolder {

        private static final ApplicationModuleHandler INSTANCE = new ApplicationModuleHandler();
    }
}
