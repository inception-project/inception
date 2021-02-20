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
package de.tudarmstadt.ukp.inception.externalsearch;

import static java.util.Collections.emptyList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;

import de.tudarmstadt.ukp.inception.support.annotation.OffsetSpan;

/**
 * This class represents a highlight from external search result. A highlight is a section of text
 * containing occurrence(s) of keywords. The offsets tells where those keywords are within the
 * original text document.
 */
public class ExternalSearchHighlight
    implements Serializable
{
    private static final long serialVersionUID = 4044755133816131842L;

    private final String highlight;

    private final List<OffsetSpan> offsets;

    public ExternalSearchHighlight(String aHighlight) throws IllegalArgumentException
    {
        Validate.notNull(aHighlight, "Highlight text must be present");

        offsets = null;
        highlight = aHighlight;
    }

    public ExternalSearchHighlight(String aHighlight, List<OffsetSpan> aOffsets)
        throws IllegalArgumentException
    {
        Validate.notNull(aOffsets, "Offsets must not be null");
        Validate.notNull(aHighlight, "Highlight text must be present");

        offsets = new ArrayList<>(aOffsets);
        highlight = aHighlight;
    }

    public String getHighlight()
    {
        return highlight;
    }

    public List<OffsetSpan> getOffsets()
    {
        return offsets != null ? offsets : emptyList();
    }
}
