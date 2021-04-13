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
package de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;

public class TsvChain
{
    private final int id;
    private final Type headType;
    private final Type elementType;
    private final NavigableMap<Integer, AnnotationFS> elements = new TreeMap<>();
    private final Map<AnnotationFS, TsvChain> fs2ChainIndex;
    private List<AnnotationFS> cachedElementsList = null;

    public TsvChain(int aId, Type aHeadType, Type aElementType,
            Map<AnnotationFS, TsvChain> aSharedIndex)
    {
        id = aId;
        headType = aHeadType;
        elementType = aElementType;
        fs2ChainIndex = aSharedIndex;
    }

    public TsvChain(int aId, Type aHeadType, Type aElementType, List<AnnotationFS> aElements,
            Map<AnnotationFS, TsvChain> aSharedIndex)
    {
        id = aId;
        headType = aHeadType;
        elementType = aElementType;
        fs2ChainIndex = aSharedIndex;

        for (int i = 0; i < aElements.size(); i++) {
            putElement(i, aElements.get(i));
        }
    }

    public int getId()
    {
        return id;
    }

    public Type getHeadType()
    {
        return headType;
    }

    public Type getElementType()
    {
        return elementType;
    }

    public void addElement(AnnotationFS aElement)
    {
        elements.put(elements.size(), aElement);
        cachedElementsList = null;
        fs2ChainIndex.put(aElement, this);
    }

    public void putElement(int aIndex, AnnotationFS aElement)
    {
        elements.put(aIndex, aElement);
        cachedElementsList = null;
        fs2ChainIndex.put(aElement, this);
    }

    public AnnotationFS getElement(int aIndex)
    {
        return elements.get(aIndex);
    }

    public Collection<AnnotationFS> getElements()
    {
        return elements.values();
    }

    public int indexOf(AnnotationFS aElement)
    {
        // This may be called often so we internally cache the list of elements.
        if (cachedElementsList == null) {
            cachedElementsList = new ArrayList<>(elements.values());
        }
        return cachedElementsList.indexOf(aElement);
    }
}
