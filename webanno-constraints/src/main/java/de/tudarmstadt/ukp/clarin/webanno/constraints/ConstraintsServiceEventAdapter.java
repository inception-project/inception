/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
package de.tudarmstadt.ukp.clarin.webanno.constraints;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.event.BeforeProjectRemovedEvent;
import de.tudarmstadt.ukp.clarin.webanno.model.ConstraintSet;

@Component
public class ConstraintsServiceEventAdapter
{
    private @Autowired ConstraintsService service;
    
    @EventListener
    public void beforeProjectRemove(BeforeProjectRemovedEvent aEvent)
        throws Exception
    {
        //Remove Constraints
        for (ConstraintSet set : service.listConstraintSets(aEvent.getProject())) {
            service.removeConstraintSet(set);
        }
    }
}
