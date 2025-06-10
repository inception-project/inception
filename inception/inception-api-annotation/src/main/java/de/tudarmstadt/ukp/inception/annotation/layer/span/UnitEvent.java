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
package de.tudarmstadt.ukp.inception.annotation.layer.span;

import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.events.AnnotationEvent;

public abstract class UnitEvent
    extends AnnotationEvent
{
    private static final long serialVersionUID = 6884859369952961123L;

    private final AnnotationFS resizedAnnotation;
    private final int oldBegin;
    private final int oldEnd;

    public UnitEvent(Object aSource, SourceDocument aDocument, String aDocumentOwner,
            AnnotationLayer aLayer, AnnotationFS aResizedUnit, int aOldBegin, int aOldEnd)
    {
        super(aSource, aDocument, aDocumentOwner, aLayer);
        resizedAnnotation = aResizedUnit;
        oldBegin = aOldBegin;
        oldEnd = aOldEnd;
    }

    public AnnotationFS getResizedUnit()
    {
        return resizedAnnotation;
    }

    public int getOldBegin()
    {
        return oldBegin;
    }

    public int getOldEnd()
    {
        return oldEnd;
    }
}
