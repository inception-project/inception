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
import de.tudarmstadt.ukp.inception.rendering.model.Range;

public class UnitMergedEvent
    extends UnitEvent
{
    private static final long serialVersionUID = 4144074959212391974L;

    private final AnnotationFS deletedUnit;

    public UnitMergedEvent(Object aSource, SourceDocument aDocument, String aUser,
            AnnotationLayer aLayer, AnnotationFS aResizedAnnotation, int aOldBegin, int aOldEnd,
            AnnotationFS aDeletedUnit)
    {
        super(aSource, aDocument, aUser, aLayer, aResizedAnnotation, aOldBegin, aOldEnd);
        deletedUnit = aDeletedUnit;
    }

    public AnnotationFS getDeletedUnit()
    {
        return deletedUnit;
    }

    @Override
    public Range getAffectedRange()
    {
        var begin = Math.min(getResizedUnit().getBegin(), deletedUnit.getBegin());
        var end = Math.min(getResizedUnit().getEnd(), deletedUnit.getEnd());
        return new Range(begin, end);
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getSimpleName());
        builder.append(" [");
        if (getDocument() != null) {
            builder.append("docID=");
            builder.append(getDocument().getId());
            builder.append(", docOwner=");
            builder.append(getDocumentOwner());
            builder.append(", ");
        }
        builder.append("resizedUnit=[");
        builder.append(getResizedUnit().getBegin());
        builder.append("-");
        builder.append(getResizedUnit().getEnd());
        builder.append("](");
        builder.append(getResizedUnit().getCoveredText());
        builder.append("), ");
        builder.append("deletedUnit=[");
        builder.append(deletedUnit.getBegin());
        builder.append("-");
        builder.append(deletedUnit.getEnd());
        builder.append("](");
        builder.append(deletedUnit.getCoveredText());
        builder.append(")]");
        return builder.toString();
    }
}
