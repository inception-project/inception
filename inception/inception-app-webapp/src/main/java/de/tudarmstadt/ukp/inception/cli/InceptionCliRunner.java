/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.cli;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.support.logging.Logging;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IFactory;

@ConditionalOnNotWebApplication
@Component
public class InceptionCliRunner
    implements CommandLineRunner, ExitCodeGenerator, ApplicationContextAware
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final InceptionCliCommand rootCommand;
    private final IFactory factory;
    private final RepositoryProperties repoProperties;

    private ApplicationContext context;

    private int exitCode;

    public InceptionCliRunner(InceptionCliCommand aRootCommand, IFactory aFactory,
            RepositoryProperties aRepoProperties)
    {
        rootCommand = aRootCommand;
        factory = aFactory;
        repoProperties = aRepoProperties;
    }

    @Override
    public void run(String... args) throws Exception
    {
        try {
            MDC.put(Logging.KEY_REPOSITORY_PATH,
                    repoProperties.getPath().getAbsolutePath().toString());

            var cl = new CommandLine(rootCommand, factory);

            for (var cmdBeanEntry : context.getBeansWithAnnotation(Command.class).entrySet()) {
                if (!(cmdBeanEntry.getValue() instanceof Callable)) {
                    log.error("Ignoring CLI command [{}] which does not implement Callable",
                            cmdBeanEntry.getKey());
                    continue;
                }

                if (cmdBeanEntry.getValue() == rootCommand) {
                    continue;
                }

                cl.addSubcommand(cmdBeanEntry.getValue());
            }

            exitCode = cl.execute(args);
        }
        finally {
            MDC.remove(Logging.KEY_REPOSITORY_PATH);
        }
    }

    @Override
    public int getExitCode()
    {
        return exitCode;
    }

    @Override
    public void setApplicationContext(ApplicationContext aApplicationContext)
    {
        context = aApplicationContext;
    }
}
