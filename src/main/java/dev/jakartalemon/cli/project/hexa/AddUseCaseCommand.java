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
package dev.jakartalemon.cli.project.hexa;

import dev.jakartalemon.cli.project.hexa.handler.DomainModuleHandler;
import dev.jakartalemon.cli.util.JsonFileUtil;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

import java.io.File;
import java.util.concurrent.Callable;

/**
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
@CommandLine.Command(
    name = "addusecase",
    resourceBundle = "messages",
    description = "Create a use case. The use case is given from the console in JSON format"
)
@Slf4j
public class AddUseCaseCommand implements Callable<Integer> {

    @CommandLine.Parameters(
        paramLabel = "USECASE_DEFINITION.json",
        descriptionKey = "usecase_definition"
    )
    private File file;

    public AddUseCaseCommand()   {

    }

    @Override
    public Integer call() throws Exception {
        var domainModuleHandler = DomainModuleHandler.getInstance();
        return JsonFileUtil.getFileJson(file.toPath())
            .map(structure -> JsonFileUtil.getProjectInfo().map(projectInfo -> {
            structure.forEach(
                (key, classDef) -> {
                    domainModuleHandler.createUseCaseClass(projectInfo, key,
                        classDef.asJsonObject());
                    domainModuleHandler.createUseCaseTestClass(projectInfo, key,
                        classDef.asJsonObject());
                });
            return 0;
        }).orElse(1)).orElse(2);
    }

}
