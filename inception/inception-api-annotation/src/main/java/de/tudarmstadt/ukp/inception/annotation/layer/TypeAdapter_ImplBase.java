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
package de.tudarmstadt.ukp.inception.annotation.layer;

import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.selectFsByAddr;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Supplier;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.ConstraintsEvaluator;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.events.FeatureValueUpdatedEvent;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupport;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistry;

public abstract class TypeAdapter_ImplBase
    implements TypeAdapter
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final LayerSupportRegistry layerSupportRegistry;
    private final FeatureSupportRegistry featureSupportRegistry;
    private final ConstraintsService constraintsService;

    private final AnnotationLayer layer;
    private final Supplier<Collection<AnnotationFeature>> featureSupplier;

    private Map<String, AnnotationFeature> features;
    private ApplicationEventPublisher applicationEventPublisher;
    private Map<AnnotationLayer, Object> layerTraitsCache;
    private Map<AnnotationFeature, Object> featureTraitsCache;

    /**
     * Constructor.
     * 
     * @param aLayerSupportRegistry
     *            the layer support registry to allow e.g. convenient decoding of layer traits.
     * @param aFeatureSupportRegistry
     *            the feature support registry to allow e.g. convenient generation of features or
     *            getting/setting feature values.
     * @param aEventPublisher
     *            an optional publisher for Spring events.
     * @param aLayer
     *            the layer for which the adapter is created.
     * @param aFeatures
     *            supplier for the features, typically
     *            {@link AnnotationSchemaService#listSupportedFeatures(AnnotationLayer)}. Since the
     *            features are not always needed, we use a supplied here so they can be loaded
     *            lazily. To facilitate testing, we do not pass the entire
     *            {@link AnnotationSchemaService}.
     */
    public TypeAdapter_ImplBase(LayerSupportRegistry aLayerSupportRegistry,
            FeatureSupportRegistry aFeatureSupportRegistry, ConstraintsService aConstraintsService,
            ApplicationEventPublisher aEventPublisher, AnnotationLayer aLayer,
            Supplier<Collection<AnnotationFeature>> aFeatures)
    {
        layerSupportRegistry = aLayerSupportRegistry;
        featureSupportRegistry = aFeatureSupportRegistry;
        constraintsService = aConstraintsService;
        applicationEventPublisher = aEventPublisher;
        layer = aLayer;
        featureSupplier = aFeatures;
    }

    @Override
    public AnnotationLayer getLayer()
    {
        return layer;
    }

    @Override
    public Collection<AnnotationFeature> listFeatures()
    {
        initFeaturesCacheIfNecessary();
        return features.values();
    }

    @Override
    public Optional<AnnotationFeature> getFeature(String aName)
    {
        initFeaturesCacheIfNecessary();
        return Optional.ofNullable(features.get(aName));
    }

    @Override
    public String renderFeatureValue(FeatureStructure aFS, String aFeature)
    {
        var feature = getFeature(aFeature);
        if (!feature.isPresent()) {
            return null;
        }
        return featureSupportRegistry.findExtension(feature.get()) //
                .map(fs -> fs.renderFeatureValue(feature.get(), aFS)) //
                .orElse(null);
    }

    @Override
    public <T> Optional<FeatureSupport<T>> getFeatureSupport(String aName)
    {
        return getFeature(aName).flatMap(featureSupportRegistry::findExtension);
    }

    @Override
    public <T> Optional<FeatureSupport<T>> getFeatureSupport(AnnotationFeature aFeature)
    {
        return getFeature(aFeature.getName()).flatMap(featureSupportRegistry::findExtension);
    }

    private void initFeaturesCacheIfNecessary()
    {
        if (features == null) {
            // Using a sorted map here so we have reliable positions in the map when iterating. We
            // use these positions to remember the armed slots!
            features = new TreeMap<>();
            for (var feature : featureSupplier.get()) {
                features.put(feature.getName(), feature);
            }
        }
    }

    @Override
    public final boolean isFeatureValueValid(AnnotationFeature aFeature, FeatureStructure aFS)
    {
        var featureSupport = featureSupportRegistry.findExtension(aFeature).orElseThrow();

        return featureSupport.isFeatureValueValid(aFeature, aFS);
    }

    @Override
    public final boolean isFeatureValueEqual(AnnotationFeature aFeature, FeatureStructure aFS1,
            FeatureStructure aFS2)
    {
        var featureSupport = featureSupportRegistry.findExtension(aFeature)
                .orElseThrow(() -> new NoSuchElementException("Support for feature " + aFeature
                        + " of type [" + aFeature.getType() + "] not found"));

        return featureSupport.isFeatureValueEqual(aFeature, aFS1, aFS2);
    }

    @Override
    public final void setFeatureValue(SourceDocument aDocument, String aUsername, CAS aCas,
            int aAddress, AnnotationFeature aFeature, Object aValue)
        throws AnnotationException
    {
        var featureSupport = featureSupportRegistry.findExtension(aFeature).orElseThrow();

        var fs = selectFsByAddr(aCas, aAddress);

        var oldValue = featureSupport.getFeatureValue(aFeature, fs);

        featureSupport.setFeatureValue(aCas, aFeature, aAddress, aValue);

        var newValue = featureSupport.getFeatureValue(aFeature, fs);

        if (!Objects.equals(oldValue, newValue)) {
            publishEvent(() -> new FeatureValueUpdatedEvent(this, aDocument, aUsername, getLayer(),
                    fs, aFeature, newValue, oldValue));
        }

        clearHiddenFeatures(aDocument, aUsername, fs);
    }

    private void clearHiddenFeatures(SourceDocument aDocument, String aUsername,
            FeatureStructure aFS)
    {
        LOG.trace("begin clear hidden");

        var constraints = constraintsService.getMergedConstraints(aDocument.getProject());
        if (constraints == null) {
            return;
        }

        var evaluator = new ConstraintsEvaluator();
        for (var feature : listFeatures()) {
            if (evaluator.isHiddenConditionalFeature(constraints, aFS, feature)) {
                var featureSupport = featureSupportRegistry.findExtension(feature).orElseThrow();

                var oldValue = featureSupport.getFeatureValue(feature, aFS);

                featureSupport.clearFeatureValue(feature, aFS);

                var newValue = featureSupport.getFeatureValue(feature, aFS);

                if (!Objects.equals(oldValue, newValue)) {
                    publishEvent(() -> new FeatureValueUpdatedEvent(this, aDocument, aUsername,
                            getLayer(), aFS, feature, newValue, oldValue));
                }
            }
        }

        LOG.trace("end clear hidden");
    }

    @Override
    public final void pushFeatureValue(SourceDocument aDocument, String aUsername, CAS aCas,
            int aAddress, AnnotationFeature aFeature, Object aValue)
        throws AnnotationException
    {
        var featureSupport = featureSupportRegistry.findExtension(aFeature).orElseThrow();

        var fs = selectFsByAddr(aCas, aAddress);

        var oldValue = featureSupport.getFeatureValue(aFeature, fs);

        featureSupport.pushFeatureValue(aCas, aFeature, aAddress, aValue);

        var newValue = featureSupport.getFeatureValue(aFeature, fs);

        if (!Objects.equals(oldValue, newValue)) {
            publishEvent(() -> new FeatureValueUpdatedEvent(this, aDocument, aUsername, getLayer(),
                    fs, aFeature, newValue, oldValue));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getFeatureValue(AnnotationFeature aFeature, FeatureStructure aFs)
    {
        return (T) featureSupportRegistry.findExtension(aFeature) //
                .orElseThrow(() -> new NoSuchElementException(
                        "Unsupported feature type [" + aFeature.getType() + "]"))
                .getFeatureValue(aFeature, aFs);
    }

    @Override
    public FeatureState getFeatureState(AnnotationFeature aFeature, FeatureStructure aFS)
    {
        Serializable value = getFeatureValue(aFeature, aFS);
        var vid = VID.of(aFS);
        var suggestionStates = getFeatureSupport(aFeature).get().getSuggestions(aFS, aFeature);
        var state = new FeatureState(vid, aFeature, value);
        state.setSuggestions(suggestionStates);
        return state;
    }

    @Deprecated
    @Override
    public void publishEvent(ApplicationEvent aEvent)
    {
        if (applicationEventPublisher != null) {
            applicationEventPublisher.publishEvent(aEvent);
        }
    }

    @Override
    public void publishEvent(Supplier<ApplicationEvent> aEventSupplier)
    {
        if (applicationEventPublisher != null) {
            applicationEventPublisher.publishEvent(aEventSupplier.get());
        }
    }

    @Override
    public void initializeLayerConfiguration(AnnotationSchemaService aSchemaService)
    {
        // Nothing to do
    }

    @Override
    public String getAttachFeatureName()
    {
        return getLayer().getAttachFeature() == null ? null
                : getLayer().getAttachFeature().getName();
    }

    /**
     * A field that takes the name of the annotation to attach to, e.g.
     * "de.tudarmstadt...type.Token" (Token.class.getName())
     */
    @Override
    public String getAttachTypeName()
    {
        return getLayer().getAttachType() == null //
                ? CAS.TYPE_NAME_ANNOTATION //
                : getLayer().getAttachType().getName();
    }

    @Override
    public void silenceEvents()
    {
        applicationEventPublisher = null;
    }

    @Override
    public boolean isSilenced()
    {
        return applicationEventPublisher == null;
    }

    public EventCollector batchEvents()
    {
        return new EventCollector();
    }

    /**
     * Decodes the traits for the current layer and returns them if they implement the requested
     * interface. This method internally caches the decoded traits, so it can be called often.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getTraits(Class<T> aInterface)
    {
        if (layerTraitsCache == null) {
            layerTraitsCache = new HashMap<>();
        }

        Object trait = layerTraitsCache.computeIfAbsent(getLayer(),
                feature -> layerSupportRegistry.getLayerSupport(feature).readTraits(feature));

        if (trait != null && aInterface.isAssignableFrom(trait.getClass())) {
            return Optional.of((T) trait);
        }

        return Optional.empty();
    }

    /**
     * Decodes the traits for the given feature and returns them if they implement the requested
     * interface. This method internally caches the decoded traits, so it can be called often.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getFeatureTraits(AnnotationFeature aFeature, Class<T> aInterface)
    {
        if (featureTraitsCache == null) {
            featureTraitsCache = new HashMap<>();
        }

        Object trait = featureTraitsCache.computeIfAbsent(aFeature,
                feature -> featureSupportRegistry.findExtension(feature).get().readTraits(feature));

        if (trait != null && aInterface.isAssignableFrom(trait.getClass())) {
            return Optional.of((T) trait);
        }

        return Optional.empty();
    }

    public class EventCollector
        implements ApplicationEventPublisher, AutoCloseable
    {
        private final ApplicationEventPublisher delegate;
        private List<Object> events = new ArrayList<>();
        private boolean committed = false;

        public EventCollector()
        {
            delegate = TypeAdapter_ImplBase.this.applicationEventPublisher;
            TypeAdapter_ImplBase.this.applicationEventPublisher = this;
        }

        @Override
        public void publishEvent(Object aEvent)
        {
            events.add(aEvent);
        }

        public void commit()
        {
            committed = true;
        }

        @Override
        public void close()
        {
            try {
                if (committed && delegate != null) {
                    events.forEach(delegate::publishEvent);
                }
            }
            finally {
                TypeAdapter_ImplBase.this.applicationEventPublisher = delegate;
            }
        }
    }
}
