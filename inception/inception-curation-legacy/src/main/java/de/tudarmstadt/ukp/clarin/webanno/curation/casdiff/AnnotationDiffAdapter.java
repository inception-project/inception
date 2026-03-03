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
package de.tudarmstadt.ukp.clarin.webanno.curation.casdiff;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.jcas.cas.AnnotationBase;
import org.apache.uima.jcas.tcas.Annotation;

import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanPosition;
import de.tudarmstadt.ukp.inception.curation.api.DiffAdapter_ImplBase;
import de.tudarmstadt.ukp.inception.curation.api.Position;

class AnnotationDiffAdapter
    extends DiffAdapter_ImplBase
{
    public AnnotationDiffAdapter(String aType, String... aFeatures)
    {
        this(aType, new HashSet<>(asList(aFeatures)));
    }

    public AnnotationDiffAdapter(String aType, Set<String> aFeatures)
    {
        super(aType, aFeatures);
    }

    @Override
    public List<Annotation> selectAnnotationsInWindow(CAS aCas, int aWindowBegin, int aWindowEnd)
    {
        return aCas.select(getType()) //
                .coveredBy(0, aWindowEnd) //
                .includeAnnotationsWithEndBeyondBounds() //
                .map(fs -> (Annotation) fs) //
                .filter(ann -> ann.overlapping(aWindowBegin, aWindowEnd)) //
                .collect(toList());
    }

    @Override
    public Position getPosition(AnnotationBase aFS)
    {
        return SpanPosition.builder() //
                .forAnnotation((Annotation) aFS) //
                .build();
    }

    @Override
    public List<? extends Position> generateSubPositions(AnnotationBase aFs)
    {
        throw new IllegalStateException("Sub-positions not supported on base adapter");
    }
}
