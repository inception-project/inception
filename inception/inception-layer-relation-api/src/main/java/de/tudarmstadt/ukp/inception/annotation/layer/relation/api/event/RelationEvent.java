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
package de.tudarmstadt.ukp.inception.annotation.layer.relation.api.event;

import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.events.AnnotationEvent;
import de.tudarmstadt.ukp.inception.rendering.model.Range;

public abstract class RelationEvent
    extends AnnotationEvent
{
    private static final long serialVersionUID = -8621413642390759892L;

    private final AnnotationFS relation;
    private final AnnotationFS targetAnno;
    private final AnnotationFS sourceAnno;

    public RelationEvent(Object aSource, SourceDocument aDocument, String aDocumentOwner,
            AnnotationLayer aLayer, AnnotationFS aRelationFS, AnnotationFS aTargetAnnotation,
            AnnotationFS aSourceAnnotation)
    {
        super(aSource, aDocument, aDocumentOwner, aLayer);

        relation = aRelationFS;
        targetAnno = aTargetAnnotation;
        sourceAnno = aSourceAnnotation;
    }

    public AnnotationFS getAnnotation()
    {
        return relation;
    }

    public AnnotationFS getTargetAnnotation()
    {
        return targetAnno;
    }

    public AnnotationFS getSourceAnnotation()
    {
        return sourceAnno;
    }

    @Override
    public Range getAffectedRange()
    {
        return new Range( //
                Math.min(sourceAnno.getBegin(), targetAnno.getBegin()),
                Math.min(sourceAnno.getEnd(), targetAnno.getEnd()));
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getSimpleName());
        builder.append(" [");
        if (getDocument() != null) {
            builder.append("docID=");
            builder.append(getDocument());
            builder.append(", docOwner=");
            builder.append(getDocumentOwner());
            builder.append(", ");
        }
        builder.append("relation=[");
        builder.append(targetAnno.getBegin());
        builder.append("-");
        builder.append(targetAnno.getEnd());
        builder.append("](");
        builder.append(targetAnno.getCoveredText());
        builder.append(") <-[");
        builder.append(sourceAnno.getBegin());
        builder.append("-");
        builder.append(sourceAnno.getEnd());
        builder.append("](");
        builder.append(sourceAnno.getCoveredText());
        builder.append(")]");
        return builder.toString();
    }

}
