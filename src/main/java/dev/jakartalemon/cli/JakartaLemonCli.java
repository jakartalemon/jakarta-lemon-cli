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
package dev.jakartalemon.cli;

import dev.jakartalemon.cli.project.CreateProjectCommand;
import dev.jakartalemon.cli.project.hexa.AddEntityCommand;
import dev.jakartalemon.cli.project.hexa.AddModelCommand;
import dev.jakartalemon.cli.project.hexa.AddRestAdapterCommand;
import dev.jakartalemon.cli.project.hexa.AddUseCaseCommand;
import dev.jakartalemon.cli.project.hexa.SetServerCommand;

import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;

/**
 * Jakarta Lemon Main Command Class
 *
 * @author Diego Silva <diego.silva at apuntesdejava.com>
 */
@Command(
    name = "jakartalemon",
    mixinStandardHelpOptions = true,
    version = "jakartalemon cli 1.0",
    description = "Command line interface for manipulation of Jakarta EE projects according to "
    + "the Jakarta Lemon tool.",
    resourceBundle = "messages",
    subcommands = {
        HelpCommand.class,
        CreateProjectCommand.class,
        AddModelCommand.class,
        AddUseCaseCommand.class,
        AddEntityCommand.class,
        AddRestAdapterCommand.class,
        SetServerCommand.class,
    }
)
public class JakartaLemonCli implements Callable<Integer> {

    /**
     * Main method that delegates execution to subcommands
     * @return 0
     * @throws Exception If corrus
     */
    @Override
    public Integer call() throws Exception {

        return 0;
    }

    /**
     * Application main method
     * @param args main args
     */
    public static void main(String... args) {
        int exitCode = new CommandLine(new JakartaLemonCli()).execute(args);
        System.exit(exitCode);
    }

}
