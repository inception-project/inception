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
package de.tudarmstadt.ukp.clarin.webanno.automation.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.event.BeforeProjectRemovedEvent;
import de.tudarmstadt.ukp.clarin.webanno.automation.model.MiraTemplate;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.TrainingDocument;

@ConditionalOnBean(AutomationService.class)
@Component
public class AutomationServiceEventAdapter
{
    private @Autowired AutomationService service;
    
    @EventListener
    public void onBeforeProjectRemove(BeforeProjectRemovedEvent aEvent)
        throws Exception
    {
        Project project = aEvent.getProject();
        
        for (TrainingDocument document : service.listTrainingDocuments(project)) {
            service.removeTrainingDocument(document);
        }
        for (MiraTemplate template : service.listMiraTemplates(project)) {
           // remove associated TRAIN and OTHER features from the Mira Template
            template.setTrainFeature(null);
            template.setOtherFeatures(null);
            
            service.removeMiraTemplate(template);
        }
    }
}
