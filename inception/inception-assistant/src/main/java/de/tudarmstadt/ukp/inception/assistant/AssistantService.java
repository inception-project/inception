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
package de.tudarmstadt.ukp.inception.assistant;

import java.io.IOException;
import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.assistant.model.MCallResponse;
import de.tudarmstadt.ukp.inception.assistant.model.MChatMessage;
import de.tudarmstadt.ukp.inception.assistant.model.MMessage;
import de.tudarmstadt.ukp.inception.assistant.model.MTextMessage;

public interface AssistantService
{
    List<MChatMessage> getUserChatHistory(String aSessionOwner, Project aProject);

    List<MChatMessage> getInternalChatHistory(String aSessionOwner, Project aProject);

    void processUserMessage(String aSessionOwner, Project aProject, SourceDocument aDocument,
            String aDataOwner, MTextMessage aMessage, MTextMessage... aTransientMessage);

    void processAgentMessage(String aSessionOwner, Project aProject, MTextMessage aMessage,
            MTextMessage... aContextMessages);

    MTextMessage processInternalMessageSync(String aSessionOwner, Project aProject,
            MTextMessage aMessage)
        throws IOException;

    <T> MCallResponse<T> processInternalCallSync(String aSessionOwner, Project aProject,
            Class<T> aType, MTextMessage aMessage)
        throws IOException;

    void clearConversation(String aSessionOwner, Project aProject);

    void setDebugMode(String aSessionOwner, Project aProject, boolean aObject);

    boolean isDebugMode(String aSessionOwner, Project aProject);

    void dispatchMessage(String aSessionOwner, Project aProject, MMessage aMessage);
}
