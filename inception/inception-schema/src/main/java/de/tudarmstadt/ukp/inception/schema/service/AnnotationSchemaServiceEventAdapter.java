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
package de.tudarmstadt.ukp.inception.schema.service;

import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.inception.project.api.event.BeforeProjectRemovedEvent;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.config.AnnotationSchemaServiceAutoConfiguration;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link AnnotationSchemaServiceAutoConfiguration#annotationSchemaServiceEventAdapter}.
 * </p>
 */
public class AnnotationSchemaServiceEventAdapter
{
    private final AnnotationSchemaService service;

    public AnnotationSchemaServiceEventAdapter(AnnotationSchemaService aService)
    {
        service = aService;
    }

    @EventListener
    @Transactional
    public void beforeProjectRemove(BeforeProjectRemovedEvent aEvent) throws Exception
    {
        Project project = aEvent.getProject();

        // clear the attach feature and attach type fields to clear up circular references
        // on columns on which we have defined foreign key constraints
        for (AnnotationLayer layer : service.listAnnotationLayer(project)) {
            layer.setAttachFeature(null);
            layer.setAttachType(null);
            service.createOrUpdateLayer(layer);
        }

        // remove the layers too
        for (AnnotationLayer layer : service.listAnnotationLayer(project)) {
            service.removeLayer(layer);
        }

        for (TagSet tagSet : service.listTagSets(project)) {
            service.removeTagSet(tagSet);
        }
    }
}
