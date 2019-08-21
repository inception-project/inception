/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.event;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

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

    public BulkAnnotationEvent(Object aSource, SourceDocument aDocument, String aUser,
            AnnotationLayer aLayer)
    {
        super(aSource, aDocument, aUser, aLayer);
    }
}
