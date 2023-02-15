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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.actions;

import org.springframework.context.ApplicationEvent;

import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationCreatedEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationDeletedEvent;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.RelationEvent;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;

public class RelationAnnotationActionUndoSupport
    implements UndoableAnnotationActionSupport
{
    private final AnnotationSchemaService schemaService;

    public RelationAnnotationActionUndoSupport(AnnotationSchemaService aSchemaService)
    {
        schemaService = aSchemaService;
    }

    @Override
    public boolean accepts(ApplicationEvent aContext)
    {
        return aContext instanceof RelationEvent;
    }

    @Override
    public UndoableAnnotationAction actionForEvent(long aRequestId, ApplicationEvent aEvent)
    {
        if (aEvent instanceof RelationCreatedEvent) {
            return new CreateRelationAnnotationAction(aRequestId, schemaService,
                    (RelationCreatedEvent) aEvent);
        }

        if (aEvent instanceof RelationDeletedEvent) {
            return new DeleteRelationAnnotationAction(aRequestId, schemaService,
                    (RelationDeletedEvent) aEvent);
        }

        throw new IllegalArgumentException("Not an undoable action: [" + aEvent.getClass() + "]");
    }
}
