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
package de.tudarmstadt.ukp.inception.pivot.api.extractor;

import static java.util.Optional.empty;

import java.io.Serializable;
import java.util.Optional;

import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

public abstract class AnnotationExtractor_ImplBase<T extends AnnotationFS, R extends Serializable>
    implements AnnotationExtractor<T, R>
{
    private final AnnotationLayer layer;

    public AnnotationExtractor_ImplBase(AnnotationLayer aLayer)
    {
        layer = aLayer;
    }

    public AnnotationLayer getLayer()
    {
        return layer;
    }

    @Override
    public Optional<String> getTriggerType()
    {
        if (layer == null) {
            return empty();
        }

        return Optional.of(layer.getName());
    }
    
    @Override
    public boolean isWeak()
    {
        return layer == null;
    }

    @Override
    public boolean accepts(Object aSource)
    {
        if (aSource instanceof AnnotationFS ann) {
            if (layer == null) {
                return true;
            }

            return layer.getName().equals(ann.getType().getName());
        }

        return false;
    }
}
