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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering;

import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VDocument;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VObject;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;

public class NopRenderer
    extends Renderer_ImplBase<TypeAdapter>
{
    public NopRenderer(TypeAdapter aTypeAdapter, LayerSupportRegistry aLayerSupportRegistry,
            FeatureSupportRegistry aFeatureSupportRegistry)
    {
        super(aTypeAdapter, aLayerSupportRegistry, aFeatureSupportRegistry);
    }

    @Override
    protected boolean typeSystemInit(TypeSystem aTypeSystem)
    {
        return false;
    }

    @Override
    public void render(CAS aCas, List<AnnotationFeature> aFeatures, VDocument aResponse,
            int windowBeginOffset, int windowEndOffset)
    {
        // Nothing to do
    }

    @Override
    public VObject render(AnnotationFS aFS, List<AnnotationFeature> aFeatures, int aWindowBegin)
    {
        // Nothing to do
        return null;
    }
}
