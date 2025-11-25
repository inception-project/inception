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
package de.tudarmstadt.ukp.inception.assistant.retriever;

import static de.tudarmstadt.ukp.inception.assistant.model.MChatRoles.SYSTEM;
import static java.time.ZoneOffset.UTC;
import static java.time.format.FormatStyle.MEDIUM;
import static java.util.Arrays.asList;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.assistant.model.MTextMessage;

@Order(10000)
public class CurrentDateRetriever
    implements Retriever
{
    @Override
    public String getId()
    {
        return CurrentDateRetriever.class.getSimpleName();
    }

    @Override
    public boolean accepts(Project aContext)
    {
        return true;
    }

    @Override
    public List<MTextMessage> retrieve(Project aProject, MTextMessage aMessage)
    {
        var dtf = DateTimeFormatter.ofLocalizedDate(MEDIUM);
        return asList(MTextMessage.builder() //
                .withActor("Current date provider") //
                .withRole(SYSTEM).internal().ephemeral() //
                .withMessage("The current date is " + LocalDateTime.now(UTC).format(dtf)) //
                .build());
    }
}
