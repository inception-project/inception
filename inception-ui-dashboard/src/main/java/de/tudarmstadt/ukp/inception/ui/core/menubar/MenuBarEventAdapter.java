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
package de.tudarmstadt.ukp.inception.ui.core.menubar;

import java.util.Objects;

import org.apache.wicket.Session;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.event.BeforeProjectRemovedEvent;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.ui.core.session.SessionMetaData;

@Component
public class MenuBarEventAdapter
{
    @EventListener
    public void onProjectDeleted(BeforeProjectRemovedEvent aEvent)
    {
        if (Session.exists()) {
            Session session = Session.get();
            // If the currently selected project is deleted, clear it from the session.
            Project project = session.getMetaData(SessionMetaData.CURRENT_PROJECT);
            if (project != null && Objects.equals(aEvent.getProject().getId(), project.getId())) {
                session.setMetaData(SessionMetaData.CURRENT_PROJECT, null);
            }
        }
    }
}
