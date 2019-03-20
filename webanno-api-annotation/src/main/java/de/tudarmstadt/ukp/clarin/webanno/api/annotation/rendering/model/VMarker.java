/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model;

public abstract class VMarker
{
    public static final String FOCUS = "focus";
    public static final String MATCH_FOCUS = "matchfocus";
    public static final String MATCH = "match";
    
    private final Object source;
    
    /**
     * @param aSource
     *            markers can have a source. This source can be used to retrieve markers from a
     *            document being rendered, e.g. to clear them. This can at time be useful when a
     *            marker has already been added but later in the rendering process it needs to be
     *            replaced by another marker. It may not always be feasible to defer the creation
     *            of the marker until its final position is clear.
     */
    public VMarker(Object aSource)
    {
        source = aSource;
    }
    
    public abstract String getType();
    
    public final Object getSource()
    {
        return source;
    }
}
