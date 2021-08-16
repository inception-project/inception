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

public class DocumentResponse
{
    private Viewport viewport;
    private List<Span> spans;
    private List<Arc> arcs;

    public DocumentResponse(Viewport aViewport, List<Span> aSpans,
                            List<Arc> aRelations)
    {
        viewport = aViewport;
        spans = aSpans;
        arcs = aRelations;
    }

    public Viewport getViewport()
    {
        return viewport;
    }

    public void setViewport(Viewport aViewport)
    {
        viewport = aViewport;
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
