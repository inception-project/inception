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

import static de.tudarmstadt.ukp.inception.security.config.InceptionSecurityWebUIApiAutoConfiguration.BASE_API_URL;
import static de.tudarmstadt.ukp.inception.websocket.config.WebSocketConstants.PARAM_PROJECT;
import static de.tudarmstadt.ukp.inception.websocket.config.WebSocketConstants.TOPIC_ELEMENT_PROJECT;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;

public interface AssistantWebsocketController
{
    String BASE_URL = BASE_API_URL + "/assistant";

    String BASE_TOPIC = "/assistant";
    String PROJECT_ASSISTANT_TOPIC_TEMPLATE = BASE_TOPIC + TOPIC_ELEMENT_PROJECT + "{"
            + PARAM_PROJECT + "}";

    public static String getChannel(Project aProject)
    {
        return BASE_TOPIC + TOPIC_ELEMENT_PROJECT + aProject.getId();
    }
}
