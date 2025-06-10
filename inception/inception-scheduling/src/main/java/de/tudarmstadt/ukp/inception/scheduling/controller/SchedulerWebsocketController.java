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
package de.tudarmstadt.ukp.inception.scheduling.controller;

import static de.tudarmstadt.ukp.inception.security.config.InceptionSecurityWebUIApiAutoConfiguration.BASE_API_URL;
import static de.tudarmstadt.ukp.inception.websocket.config.WebSocketConstants.PARAM_PROJECT;
import static de.tudarmstadt.ukp.inception.websocket.config.WebSocketConstants.TOPIC_ELEMENT_PROJECT;

import java.util.Properties;

import org.springframework.util.PropertyPlaceholderHelper;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.scheduling.controller.model.MTaskStateUpdate;

public interface SchedulerWebsocketController
{
    String BASE_URL = BASE_API_URL + "/scheduler";

    String BASE_TOPIC = "/scheduler";
    String USER_TASKS_TOPIC = BASE_TOPIC + "/user";
    String PROJECT_TASKS_TOPIC_TEMPLATE = BASE_TOPIC + TOPIC_ELEMENT_PROJECT + "{" + PARAM_PROJECT
            + "}";

    void dispatch(MTaskStateUpdate update);

    static String getUserTaskUpdatesTopic()
    {
        return USER_TASKS_TOPIC;
    }

    static String getProjectTaskUpdatesTopic(Project aProject)
    {
        return getProjectTaskUpdatesTopic(aProject.getId());
    }

    static String getProjectTaskUpdatesTopic(long aProjectId)
    {
        var properties = new Properties();
        properties.setProperty(PARAM_PROJECT, String.valueOf(aProjectId));
        var replacer = new PropertyPlaceholderHelper("{", "}", null, null, false);
        var topic = replacer.replacePlaceholders(PROJECT_TASKS_TOPIC_TEMPLATE, properties);
        return topic;
    }
}
