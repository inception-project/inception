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
package de.tudarmstadt.ukp.inception.log.adapter;

import java.io.IOException;
import java.util.Objects;

import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.inception.log.model.StateChangeDetails;
import de.tudarmstadt.ukp.inception.project.api.event.ProjectStateChangedEvent;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

@Component
public class ProjectStateChangedEventAdapter
    implements EventLoggingAdapter<ProjectStateChangedEvent>
{
    @Override
    public boolean accepts(Class<?> aEvent)
    {
        return ProjectStateChangedEvent.class.isAssignableFrom(aEvent);
    }

    @Override
    public long getProject(ProjectStateChangedEvent aEvent)
    {
        return aEvent.getProject().getId();
    }

    @Override
    public String getDetails(ProjectStateChangedEvent aEvent) throws IOException
    {
        var details = new StateChangeDetails();
        details.setState(Objects.toString(aEvent.getNewState(), null));
        details.setPreviousState(Objects.toString(aEvent.getPreviousState(), null));
        return JSONUtil.toJsonString(details);
    }
}
