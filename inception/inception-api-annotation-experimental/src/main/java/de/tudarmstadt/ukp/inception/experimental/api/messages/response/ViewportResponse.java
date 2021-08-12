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

public class ViewportResponse
{
    private List<String> viewportText;
    private List<Span> spans;
    private List<Arc> relations;

    public ViewportResponse(List<String> aViewportText, List<Span> aSpans, List<Arc> aRelations)
    {
        viewportText = aViewportText;
        spans = aSpans;
        relations = aRelations;
    }

    public List<String> getViewportText() {
        return viewportText;
    }

    public void setViewportText(List<String> aViewportText) {
        this.viewportText = aViewportText;
    }

    public List<Span> getSpans()
    {
        return spans;
    }

    public void setSpans(List<Span> aSpans)
    {
        spans = aSpans;
    }

    public List<Arc> getRelations()
    {
        return relations;
    }

    public void setRelations(List<Arc> aRelations)
    {
        relations = aRelations;
    }
}
