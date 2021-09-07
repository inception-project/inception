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
package de.tudarmstadt.ukp.inception.experimental.api.messages.response;

import java.util.List;

import de.tudarmstadt.ukp.inception.experimental.api.model.Arc;
import de.tudarmstadt.ukp.inception.experimental.api.model.Span;
import de.tudarmstadt.ukp.inception.experimental.api.model.Viewport;

/**
 * Class required for Messaging between Server and Client.
 * Basis for JSON
 * DocumentMessage: Message published to a specific client containing the data for the requested document
 *
 * Attributes:
 * viewport: List of Viewports and their contents requested by the client
 * sourceDocumentId: The ID of the requested sourcedocument
 * spans: List of Spans contained in the requested viewport for a certain document
 * arcs: List of Arcs contained in the requested viewport for a certain document
 **/
public class DocumentMessage
{
    private List<Viewport> viewport;
    private long sourceDocumentId;
    private List<Span> spans;
    private List<Arc> arcs;

    public DocumentMessage(List<Viewport> aViewport, long aSourceDocumentId, List<Span> aSpans,
                           List<Arc> aRelations)
    {
        viewport = aViewport;
        sourceDocumentId = aSourceDocumentId;
        spans = aSpans;
        arcs = aRelations;
    }

    public List<Viewport> getViewport()
    {
        return viewport;
    }

    public void setViewport(List<Viewport> aViewport)
    {
        viewport = aViewport;
    }

    public long getSourceDocumentId()
    {
        return sourceDocumentId;
    }

    public void setSourceDocumentId(long aSourceDocumentId)
    {
        sourceDocumentId = aSourceDocumentId;
    }

    public List<Span> getSpans()
    {
        return spans;
    }

    public void setSpans(List<Span> aSpans)
    {
        spans = aSpans;
    }

    public List<Arc> getArcs()
    {
        return arcs;
    }

    public void setArcs(List<Arc> aArcs)
    {
        arcs = aArcs;
    }
}
