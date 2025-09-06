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
package de.tudarmstadt.ukp.inception.annotation.layer.span.pivot;

import java.util.Optional;

import org.apache.uima.jcas.tcas.Annotation;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.pivot.api.extractor.AnnotationExtractor;

public class SpanRangeExtractor<T extends Annotation>
    implements AnnotationExtractor<T, String>
{
    private AnnotationLayer layer;

    public SpanRangeExtractor(AnnotationLayer aLayer)
    {
        layer = aLayer;
    }

    @Override
    public String getName()
    {
        // if (layer != null) {
        // return layer.getUiName() + " :: <range>";
        // }

        return "<range>";
    }

    @Override
    public Class<String> getResultType()
    {
        return String.class;
    }

    @Override
    public Optional<String> getTriggerType()
    {
        if (layer == null) {
            return Optional.empty();
        }

        return Optional.of(layer.getName());
    }

    @Override
    public boolean isWeak()
    {
        return true;
    }

    @Override
    public String extract(T a)
    {
        if (layer == null) {
            return a.getBegin() + "-" + a.getEnd();
        }

        if (a.getType().getName().equals(layer.getName())) {
            return a.getBegin() + "-" + a.getEnd();
        }

        return null;
    }
}
