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

import dev.jakartalemon.cli.project.hexa.handler.PayaraHandler;
import dev.jakartalemon.cli.util.JsonFileUtil;
import java.io.IOException;
import java.net.URISyntaxException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import picocli.CommandLine;

/**
 *
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
@CommandLine.Command(
    name = "setserver",
    resourceBundle = "messages",
    description = "Set server"
)
@Slf4j
public class SetServerCommand implements Runnable {

    @CommandLine.Parameters(
        paramLabel = "setserver.parameter",
        descriptionKey = "setserver_parameter"
    )
    private String serverName;

    @Override
    public void run() {
        switch (StringUtils.upperCase(serverName)) {
            case "PAYARA_MICRO":
            case "PAYARAMICRO":
                setPayaraMicro();
                break;
        }
    }

    private void setPayaraMicro() {
        JsonFileUtil.getProjectInfo().ifPresent(projectInfo -> {
            try {
                var payaraHandler = PayaraHandler.getInstance();
                payaraHandler.addPayaraMicroPlugin();
            } catch (InterruptedException | IOException | URISyntaxException ex) {
                log.error(ex.getMessage(), ex);
            }
        });
    }

}
