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
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.stereotype.Component;

import picocli.CommandLine.Command;

@ConditionalOnNotWebApplication
@Component
@Command(name = "inception", mixinStandardHelpOptions = true)
public class InceptionCliCommand
    implements Callable<Integer>
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public Integer call() throws Exception
    {
        log.error(
                "When you start the application with no parameters, the server should be started. No idea how you got here...");

        return 0;
    }
}
