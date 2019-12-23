/*
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
package de.tudarmstadt.ukp.clarin.webanno.ui.curation.component.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

/**
 * A model for curation container comprises of {@link SourceListView}, {@link SourceDocument}, and
 * {@link Project}.
 */
public class CurationContainer
    implements Serializable
{
    private static final long serialVersionUID = -6632707037285383353L;

    private Map<Integer, SourceListView> curationViewByBegin = new HashMap<>();

    private AnnotatorState state;

    public List<SourceListView> getCurationViews()
    {
        List<Integer> viewsBegin = new ArrayList<>(curationViewByBegin.keySet());
        Collections.sort(viewsBegin);
        List<SourceListView> curationViews = new LinkedList<>();
        for (Integer begin : viewsBegin) {
            curationViews.add(curationViewByBegin.get(begin));
        }
        return curationViews;
    }

    public Map<Integer, SourceListView> getCurationViewByBegin()
    {
        return curationViewByBegin;
    }

    public void setCurationSegmentByBegin(Map<Integer, SourceListView> aCurationViewByBegin)
    {
        curationViewByBegin = aCurationViewByBegin;
    }

    public AnnotatorState getState()
    {
        return state;
    }

    public void setState(AnnotatorState aState)
    {
        state = aState;
    }
}
