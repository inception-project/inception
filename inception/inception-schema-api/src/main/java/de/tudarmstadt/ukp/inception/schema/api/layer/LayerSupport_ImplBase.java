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
package de.tudarmstadt.ukp.inception.schema.api.layer;

import static de.tudarmstadt.ukp.inception.support.json.JSONUtil.fromJsonString;
import static de.tudarmstadt.ukp.inception.support.json.JSONUtil.toJsonString;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;

import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;

public abstract class LayerSupport_ImplBase<A extends TypeAdapter, T>
    implements LayerSupport<A, T>
{
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private LayerSupportRegistry layerSupportRegistry;

    protected final FeatureSupportRegistry featureSupportRegistry;

    public LayerSupport_ImplBase(FeatureSupportRegistry aFeatureSupportRegistry)
    {
        featureSupportRegistry = aFeatureSupportRegistry;
    }

    public final void generateFeatures(TypeSystemDescription aTSD, TypeDescription aTD,
            List<AnnotationFeature> aFeatures)
    {
        for (var feature : aFeatures) {
            featureSupportRegistry.findExtension(feature)
                    .ifPresent(fs -> fs.generateFeature(aTSD, aTD, feature));
        }
    }

    @Override
    public void setLayerSupportRegistry(LayerSupportRegistry aLayerSupportRegistry)
    {
        if (layerSupportRegistry != null) {
            throw new IllegalStateException("LayerSupportRegistry can only be set once!");
        }

        layerSupportRegistry = aLayerSupportRegistry;
    }

    @Override
    public LayerSupportRegistry getLayerSupportRegistry()
    {
        if (layerSupportRegistry == null) {
            throw new IllegalStateException("LayerSupportRegistry not set!");
        }

        return layerSupportRegistry;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T readTraits(AnnotationLayer aLayer)
    {
        T traits = null;
        try {
            traits = fromJsonString((Class<T>) createTraits().getClass(), aLayer.getTraits());
        }
        catch (IOException e) {
            LOG.error("Unable to read traits", e);
        }

        if (traits == null) {
            traits = createTraits();
        }

        return traits;
    }

    @Override
    public void writeTraits(AnnotationLayer aLayer, T aTraits)
    {
        try {
            aLayer.setTraits(toJsonString(aTraits));
        }
        catch (IOException e) {
            LOG.error("Unable to write traits", e);
        }
    }
}
