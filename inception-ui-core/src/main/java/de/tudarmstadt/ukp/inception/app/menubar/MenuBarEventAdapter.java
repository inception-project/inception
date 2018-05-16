/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.app.menubar;

import java.util.Objects;

import org.apache.wicket.Session;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.event.BeforeProjectRemovedEvent;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.app.session.SessionMetaData;

@Component
public class MenuBarEventAdapter
{
    @EventListener
    public void onProjectDeleted(BeforeProjectRemovedEvent aEvent)
    {
        Session session = Session.get();
        if (session != null) {
            // If the currently selected project is deleted, clear it from the session.
            Project project = session.getMetaData(SessionMetaData.CURRENT_PROJECT);
            if (project != null && Objects.equals(aEvent.getProject().getId(), project.getId())) {
                session.setMetaData(SessionMetaData.CURRENT_PROJECT, null);
            }
        }
    }
}
