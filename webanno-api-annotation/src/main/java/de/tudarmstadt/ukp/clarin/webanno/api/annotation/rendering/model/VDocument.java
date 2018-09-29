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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

public class VDocument
{
    private Map<VID, VArc> arcs = new LinkedHashMap<>();
    private Map<VID, VSpan> spans = new LinkedHashMap<>();
    private ListValuedMap<VID, VComment> comments = new ArrayListValuedHashMap<>();
    private ListValuedMap<Long, VArc> arcsByLayer = new ArrayListValuedHashMap<>();
    private ListValuedMap<Long, VSpan> spansByLayer = new ArrayListValuedHashMap<>();
    private Map<Long, AnnotationLayer> annotationLayers = new LinkedHashMap<>();
    private List<VMarker> markers = new ArrayList<>();

    public void add(VArc aArc)
    {
        if (arcs.containsKey(aArc.getVid()) || spans.containsKey(aArc.getVid())) {
            throw new IllegalStateException("Annotation [" + aArc.getVid() + "] already exists.");
        }
        
        arcs.put(aArc.getVid(), aArc);
        annotationLayers.put(aArc.getLayer().getId(), aArc.getLayer());
        arcsByLayer.put(aArc.getLayer().getId(), aArc);
    }
    
    public void add(VSpan aSpan)
    {
        if (arcs.containsKey(aSpan.getVid()) || spans.containsKey(aSpan.getVid())) {
            throw new IllegalStateException("Annotation [" + aSpan.getVid() + "] already exists.");
        }
        
        spans.put(aSpan.getVid(), aSpan);
        annotationLayers.put(aSpan.getLayer().getId(), aSpan.getLayer());
        spansByLayer.put(aSpan.getLayer().getId(), aSpan);
    }
    
    public void add(VComment aComment)
    {
        comments.put(aComment.getVid(), aComment);
    }
    
    public void add(VMarker aMarker)
    {
        markers.add(aMarker);
    }
    
    public VSpan getSpan(VID aVid) {
        return spans.get(aVid);
    }

    public VArc getArc(VID aVid) {
        return arcs.get(aVid);
    }

    public Collection<VSpan> spans()
    {
        return Collections.unmodifiableCollection(spans.values());
    }
    
    public List<VMarker> getMarkers()
    {
        return markers;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Collection<VSpan> spans(long aLayerId)
    {
        if (spansByLayer.containsKey(aLayerId)) {
            return Collections.unmodifiableList((List) spansByLayer.get(aLayerId));
        }
        else {
            return Collections.emptyList();
        }
    }

    public Collection<VArc> arcs()
    {
        return Collections.unmodifiableCollection(arcs.values());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Collection<VArc> arcs(long aLayerId)
    {
        if (arcsByLayer.containsKey(aLayerId)) {
            return Collections.unmodifiableList((List) arcsByLayer.get(aLayerId));
        }
        else {
            return Collections.emptyList();
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Collection<VComment> comments()
    {
        return (Collection) comments.values();
    }
    
    public VObject get(VID aVid)
    {
        VArc arc = arcs.get(aVid);
        if (arc != null) {
            return arc;
        }
        
        return spans.get(aVid);
    }

    public Collection<AnnotationLayer> getAnnotationLayers()
    {
        return Collections.unmodifiableCollection(annotationLayers.values());
    }
}
