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
package de.tudarmstadt.ukp.inception.annotation.layer.span.api;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.layer.TypeAdapter_ImplBase.EventCollector;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;

/**
 * Manage interactions with annotations on a span layer.
 */
public interface SpanAdapter
    extends TypeAdapter
{
    public enum SpanOption
    {
        TRIM;
    }

    AnnotationFS handle(CreateSpanAnnotationRequest aRequest) throws AnnotationException;

    AnnotationFS handle(MoveSpanAnnotationRequest aRequest) throws AnnotationException;

    void handle(DeleteSpanAnnotationRequest aRequest) throws AnnotationException;

    EventCollector batchEvents();

    /**
     * Create a new span annotation.
     *
     * @param aDocument
     *            the document to which the CAS belongs
     * @param aDataOwner
     *            the user to which the CAS belongs
     * @param aCas
     *            the CAS.
     * @param aBegin
     *            the begin offset.
     * @param aEnd
     *            the end offset.
     * @return the new annotation.
     * @throws AnnotationException
     *             if the annotation cannot be created/updated.
     */
    AnnotationFS add(SourceDocument aDocument, String aDataOwner, CAS aCas, int aBegin, int aEnd)
        throws AnnotationException;

    AnnotationFS restore(SourceDocument aDocument, String aDocumentOwner, CAS aCas, VID aVid)
        throws AnnotationException;
}
