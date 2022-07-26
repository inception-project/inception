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
package de.tudarmstadt.ukp.inception.diam.messages;

import static java.util.stream.Collectors.toList;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.tudarmstadt.ukp.inception.rendering.vmodel.VDocument;

public class MViewportInit
{
    private final String text;
    private final List<MArc> arcs;
    private final List<MSpan> spans;

    public MViewportInit(VDocument aVDocument)
    {
        text = aVDocument.getText();
        arcs = aVDocument.getArcs().values().stream().map(MArc::new).collect(toList());
        spans = aVDocument.getSpans().values().stream().map(MSpan::new).collect(toList());
    }

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public MViewportInit(@JsonProperty("text") String aText, @JsonProperty("arcs") List<MArc> aArcs,
            @JsonProperty("spans") List<MSpan> aSpans)
    {
        text = aText;
        arcs = aArcs;
        spans = aSpans;
    }

    public String getText()
    {
        return text;
    }

    public List<MArc> getArcs()
    {
        return arcs;
    }

    public List<MSpan> getSpans()
    {
        return spans;
    }
}
