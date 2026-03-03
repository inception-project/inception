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

import java.util.Collections;
import java.util.List;

import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.tcas.Annotation;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.inception.rendering.request.RenderRequest;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VDocument;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VObject;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistry;

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
    public void render(RenderRequest aRequest, List<AnnotationFeature> aFeatures,
            VDocument aResponse)
    {
        // Nothing to do
    }

    @Override
    public List<VObject> render(RenderRequest aRequest, List<AnnotationFeature> aFeatures,
            VDocument aResponse, AnnotationFS aFS)

    {
        return Collections.emptyList();
    }

    @Override
    public List<Annotation> selectAnnotationsInWindow(RenderRequest aRequest)
    {
        return Collections.emptyList();
    }
}
