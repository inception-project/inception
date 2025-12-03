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
package de.tudarmstadt.ukp.inception.workload.matrix.management.event;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.wicketstuff.event.annotation.AbstractAjaxAwareEvent;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

/**
 * Fired when a user clicks on the annotator comment symbol.
 */
public class AnnotatorColumnCellShowAnnotatorCommentEvent
    extends AbstractAjaxAwareEvent
{
    private final Component cell;
    private final SourceDocument sourceDocument;
    private final AnnotationDocumentState state;
    private final AnnotationSet annotationSet;

    public AnnotatorColumnCellShowAnnotatorCommentEvent(AjaxRequestTarget aTarget, Component aCell,
            SourceDocument aSourceDocument, AnnotationSet aSet, AnnotationDocumentState aState)
    {
        super(aTarget);

        cell = aCell;
        sourceDocument = aSourceDocument;
        annotationSet = aSet;
        state = aState;
    }

    public SourceDocument getSourceDocument()
    {
        return sourceDocument;
    }

    public AnnotationSet getAnnotationSet()
    {
        return annotationSet;
    }

    public AnnotationDocumentState getState()
    {
        return state;
    }

    public Component getCell()
    {
        return cell;
    }
}
