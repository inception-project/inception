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
package de.tudarmstadt.ukp.inception.annotation.layer.relation.event;

import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.events.AnnotationCreatedEvent;

public class RelationCreatedEvent
    extends RelationEvent
    implements AnnotationCreatedEvent
{
    private static final long serialVersionUID = 1729547025986959164L;

    public RelationCreatedEvent(Object aSource, SourceDocument aDocument, String aDocumentOwner,
            AnnotationLayer aLayer, AnnotationFS aRelationFS, AnnotationFS aTargetAnnotation,
            AnnotationFS aSourceAnnotation)
    {
        super(aSource, aDocument, aDocumentOwner, aLayer, aRelationFS, aTargetAnnotation,
                aSourceAnnotation);
    }
}
