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
import static java.lang.String.join;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import java.util.List;

import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.assistant.config.AssistantProperties;
import de.tudarmstadt.ukp.inception.assistant.model.MTextMessage;
import de.tudarmstadt.ukp.inception.assistant.retriever.Retriever;

@Order(1000)
public class UserGuideRetriever
    implements Retriever
{
    private final UserGuideQueryService documentationIndexingService;
    private final AssistantProperties properties;

    public UserGuideRetriever(UserGuideQueryService aDocumentationIndexingService,
            AssistantProperties aProperties)
    {
        documentationIndexingService = aDocumentationIndexingService;
        properties = aProperties;
    }

    @Override
    public String getId()
    {
        return UserGuideRetriever.class.getSimpleName();
    }

    @Override
    public boolean accepts(Project aContext)
    {
        return true;
    }

    @Override
    public List<MTextMessage> retrieve(Project aProject, MTextMessage aMessage)
    {
        var messageBody = new StringBuilder();
        var passages = documentationIndexingService.query(aMessage.message(),
                properties.getUserGuide().getMaxChunks(), properties.getUserGuide().getMinScore());
        for (var passage : passages) {
            messageBody.append("\n```user-manual\n") //
                    .append(passage) //
                    .append("\n```\n\n");
        }

        if (messageBody.isEmpty()) {
            // var message = MTextMessage.builder() //
            // .withActor("User guide") //
            // .withRole(SYSTEM).internal().ephemeral() //
            // .withMessage("There seems to be no relevant information in the user manual.") //
            // .build();
            return emptyList();
        }

        return asList(MTextMessage.builder() //
                .withActor("User guide") //
                .withRole(SYSTEM).internal().ephemeral() //
                .withMessage(join("\n", asList(
                        "The user guide retriever automatically provides you with relevant sources from the user guide.",
                        "Use the following sources from the user guide to respond.", "",
                        messageBody.toString())))
                .build());
    }
}
