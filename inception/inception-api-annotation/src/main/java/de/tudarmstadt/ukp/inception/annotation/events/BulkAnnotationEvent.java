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
package de.tudarmstadt.ukp.inception.annotation.events;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.layer.TypeAdapter_ImplBase;
import de.tudarmstadt.ukp.inception.rendering.model.Range;

/**
 * Indicates that many annotations have been affected at once. This is used on bulk operations
 * instead of generating an event for every individual change.
 * 
 * @see TypeAdapter_ImplBase#silenceEvents()
 */
public class BulkAnnotationEvent
    extends AnnotationEvent
{
    private static final long serialVersionUID = -1187536069360130349L;

    public BulkAnnotationEvent(Object aSource, Project aProject, String aDataOwner,
            AnnotationLayer aLayer)
    {
        super(aSource, aProject, aDataOwner, aLayer);
    }

    public BulkAnnotationEvent(Object aSource, SourceDocument aDocument, String aDataOwner,
            AnnotationLayer aLayer)
    {
        super(aSource, aDocument, aDataOwner, aLayer);
    }

    public BulkAnnotationEvent(Object aSource, SourceDocument aDocument, String aDataOwner)
    {
        super(aSource, aDocument, aDataOwner);
    }

    @Override
    public Range getAffectedRange()
    {
        return Range.UNDEFINED;
    }
}
