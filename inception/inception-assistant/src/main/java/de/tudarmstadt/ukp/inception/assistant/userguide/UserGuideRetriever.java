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
package de.tudarmstadt.ukp.inception.assistant.userguide;

import static de.tudarmstadt.ukp.inception.assistant.model.MChatRoles.SYSTEM;
import static java.util.Arrays.asList;

import java.util.List;

import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.assistant.ChatContext;
import de.tudarmstadt.ukp.inception.assistant.documents.DocumentContextRetriever;
import de.tudarmstadt.ukp.inception.assistant.model.MTextMessage;
import de.tudarmstadt.ukp.inception.assistant.retriever.Retriever;

@Order(1000)
public class UserGuideRetriever
    implements Retriever
{
    private final UserGuideQueryService documentationIndexingService;

    public UserGuideRetriever(UserGuideQueryService aDocumentationIndexingService)
    {
        documentationIndexingService = aDocumentationIndexingService;
    }

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
    public List<MTextMessage> retrieve(ChatContext aAssistant, MTextMessage aMessage)
    {
        var messageBody = new StringBuilder();
        var passages = documentationIndexingService.query(aMessage.message(), 3, 0.8);
        for (var passage : passages) {
            messageBody.append("\n```user-manual\n") //
                    .append(passage) //
                    .append("\n```\n\n");
        }

        MTextMessage message;
        if (messageBody.isEmpty()) {
            message = MTextMessage.builder() //
                    .withActor("User guide") //
                    .withRole(SYSTEM).internal() //
                    .withMessage("There seems to be no relevant information in the user manual.") //
                    .build();
        }
        else {
            message = MTextMessage.builder() //
                    .withActor("User guide") //
                    .withRole(SYSTEM).internal() //
                    .withMessage(
                            """
                            Use the context information from following user manual entries to respond.
                            """ + messageBody
                                    .toString()) //
                    .build();
        }

        return asList(message);
    }
}
