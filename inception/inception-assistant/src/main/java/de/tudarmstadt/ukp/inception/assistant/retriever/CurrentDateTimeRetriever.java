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

import static de.tudarmstadt.ukp.inception.assistant.model.MAssistantChatRoles.SYSTEM;
import static java.util.Arrays.asList;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;

import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.assistant.documents.DocumentContextRetriever;
import de.tudarmstadt.ukp.inception.assistant.model.MAssistantTextMessage;

@Order(10000)
public class CurrentDateTimeRetriever
    implements Retriever
{
    @Override
    public String getId()
    {
        return DocumentContextRetriever.class.getSimpleName();
    }

    @Override
    public boolean accepts(Project aContext)
    {
        return true;
    }
    
    @Override
    public List<MAssistantTextMessage> retrieve(String aSessionOwner, Project aProject,
            MAssistantTextMessage aMessage)
    {
        var dtf = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);
        return asList(MAssistantTextMessage.builder() //
                .withRole(SYSTEM).internal() //
                .withMessage("The current time is " + LocalDateTime.now(ZoneOffset.UTC).format(dtf)) //
                .build());
    }
}
