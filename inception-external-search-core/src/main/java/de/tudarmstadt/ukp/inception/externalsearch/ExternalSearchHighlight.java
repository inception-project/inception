/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.externalsearch;

import java.io.IOException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import de.tudarmstadt.ukp.inception.support.annotation.OffsetSpan;

/**
 * This class represents a highlight from external search result.
 * A highlight is a section of text containing occurrence(s) of keywords.
 * The offsets tells where those keywords are within the original text document.
 */
public class ExternalSearchHighlight implements Serializable
{

    private static final long serialVersionUID = 4044755133816131842L;

    private String highlight = null;

    private List<OffsetSpan> offsets;

    public ExternalSearchHighlight(List<OffsetSpan> aOffsets) throws IllegalArgumentException
    {
        if (aOffsets == null || aOffsets.isEmpty()) {
            throw new IllegalArgumentException("Offsets must not be empty");
        }
        offsets = new ArrayList<>(aOffsets);

    }

    public ExternalSearchHighlight(String aHighlight, List<OffsetSpan> aOffsets)
        throws IllegalArgumentException
    {
        this(aOffsets);
        highlight = aHighlight;
    }

    public String getHighlight()
    {
        return highlight;
    }

    public List<OffsetSpan> getOffsets() {
        return offsets;
    }
}
