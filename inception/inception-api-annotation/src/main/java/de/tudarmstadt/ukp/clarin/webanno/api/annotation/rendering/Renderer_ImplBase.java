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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.rendering.Renderer;
import de.tudarmstadt.ukp.inception.rendering.request.RenderRequest;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VDocument;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VRange;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VSpan;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistry;

public abstract class Renderer_ImplBase<T extends TypeAdapter>
    implements Renderer
{
    private final T typeAdapter;
    private final FeatureSupportRegistry featureSupportRegistry;
    private final LayerSupportRegistry layerSupportRegistry;

    private Map<AnnotationFeature, Object> featureTraitsCache;
    private Map<AnnotationLayer, Object> layerTraitsCache;

    private TypeSystem typeSystem;
    private boolean allTypesPresent;

    public Renderer_ImplBase(T aTypeAdapter, LayerSupportRegistry aLayerSupportRegistry,
            FeatureSupportRegistry aFeatureSupportRegistry)
    {
        featureSupportRegistry = aFeatureSupportRegistry;
        layerSupportRegistry = aLayerSupportRegistry;
        typeAdapter = aTypeAdapter;
    }

    /**
     * Checks if the type system has changed compared to the last call. If this is the case, then
     * {@link #typeSystemInit} is called to give the renderer the opportunity to obtain new type and
     * feature information from the type system.
     * 
     * @param aCas
     *            a CAS.
     * @return returns {@code true} if all types are present and rendering can commence and
     *         {@code false} if any types are missing and rendering should be skipped.
     */
    protected boolean checkTypeSystem(CAS aCas)
    {
        if (typeSystem != aCas.getTypeSystem()) {
            typeSystem = aCas.getTypeSystem();
            allTypesPresent = typeSystemInit(typeSystem);
        }

        return allTypesPresent;
    }

    protected abstract boolean typeSystemInit(TypeSystem aTypeSystem);

    @Override
    public FeatureSupportRegistry getFeatureSupportRegistry()
    {
        return featureSupportRegistry;
    }

    @Override
    public T getTypeAdapter()
    {
        return typeAdapter;
    }

    /**
     * @param aFeature
     *            the feature
     * @param aInterface
     *            the traits interface
     * @param <T>
     *            the traits type
     * @return the traits for the given feature if they implement the requested interface. This
     *         method internally caches the decoded traits, so it can be called often.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getTraits(AnnotationFeature aFeature, Class<T> aInterface)
    {
        if (featureTraitsCache == null) {
            featureTraitsCache = new HashMap<>();
        }

        Object trait = featureTraitsCache.computeIfAbsent(aFeature,
                feature -> featureSupportRegistry.findExtension(feature).orElseThrow()
                        .readTraits(feature));

        if (trait != null && aInterface.isAssignableFrom(trait.getClass())) {
            return Optional.of((T) trait);
        }

        return Optional.empty();
    }

    /**
     * @param aLayer
     *            the layer
     * @param aInterface
     *            the traits interface
     * @param <T>
     *            the traits type
     * @return the decoded traits for the given layer if they implement the requested interface.
     *         This method internally caches the decoded traits, so it can be called often.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getTraits(AnnotationLayer aLayer, Class<T> aInterface)
    {
        if (layerTraitsCache == null) {
            layerTraitsCache = new HashMap<>();
        }

        Object trait = layerTraitsCache.computeIfAbsent(aLayer,
                feature -> layerSupportRegistry.getLayerSupport(feature).readTraits(feature));

        if (trait != null && aInterface.isAssignableFrom(trait.getClass())) {
            return Optional.of((T) trait);
        }

        return Optional.empty();
    }

    public static VID createEndpoint(RenderRequest aRequest, VDocument aVDocument,
            AnnotationFS aEndpoint, TypeAdapter aTypeAdapter)
    {
        var windowBegin = aVDocument.getWindowBegin();
        var windowEnd = aVDocument.getWindowEnd();

        if (aEndpoint.getEnd() < windowBegin) {
            if (aRequest.isClipArcs()) {
                if (aVDocument.getSpan(VID_BEFORE) == null) {
                    var beforeAnchor = VSpan.builder() //
                            .withVid(VID_BEFORE) //
                            .withLayer(aTypeAdapter.getLayer()) //
                            .withRange(new VRange(0, 0)) //
                            .build();
                    aVDocument.add(beforeAnchor);
                }
                return VID_BEFORE;
            }

            var placeholder = VSpan.builder() //
                    .forAnnotation(aEndpoint) //
                    .placeholder() //
                    .withLayer(aTypeAdapter.getLayer()) //
                    .withRange(new VRange(aEndpoint.getBegin() - windowBegin,
                            aEndpoint.getEnd() - windowBegin)) //
                    .build();
            aVDocument.add(placeholder);
            return placeholder.getVid();
        }

        if (aEndpoint.getBegin() >= windowEnd) {
            if (aRequest.isClipArcs()) {
                if (aVDocument.getSpan(VID_AFTER) == null) {
                    var afterAnchor = VSpan.builder() //
                            .withVid(VID_AFTER) //
                            .withLayer(aTypeAdapter.getLayer()) //
                            .withRange(new VRange(windowEnd - windowBegin, windowEnd - windowBegin)) //
                            .build();
                    aVDocument.add(afterAnchor);
                }
                return VID_AFTER;
            }

            var placeholder = VSpan.builder() //
                    .forAnnotation(aEndpoint) //
                    .placeholder() //
                    .withLayer(aTypeAdapter.getLayer()) //
                    .withRange(new VRange(aEndpoint.getBegin() - windowBegin,
                            aEndpoint.getEnd() - windowBegin)) //
                    .build();
            aVDocument.add(placeholder);
            return placeholder.getVid();
        }

        return VID.of(aEndpoint);
    }
}
