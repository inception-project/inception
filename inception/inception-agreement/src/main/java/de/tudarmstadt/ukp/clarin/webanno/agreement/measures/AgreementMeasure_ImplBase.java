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
package de.tudarmstadt.ukp.clarin.webanno.agreement.measures;

import java.io.Serializable;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

public abstract class AgreementMeasure_ImplBase<R extends Serializable, T extends DefaultAgreementTraits>
    implements AgreementMeasure<R>
{
    private final AnnotationLayer layer;
    private final AnnotationFeature feature;
    private final T traits;

    public AgreementMeasure_ImplBase(AnnotationLayer aLayer, AnnotationFeature aFeature, T aTraits)
    {
        layer = aLayer;
        feature = aFeature;
        traits = aTraits;
    }

    @Override
    public AnnotationLayer getLayer()
    {
        return layer;
    }

    @Override
    public AnnotationFeature getFeature()
    {
        return feature;
    }

    @Override
    public T getTraits()
    {
        return traits;
    }
}
