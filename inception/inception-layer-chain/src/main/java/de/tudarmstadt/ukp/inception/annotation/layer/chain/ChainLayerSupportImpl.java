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
package de.tudarmstadt.ukp.inception.annotation.layer.chain;

import static java.util.Arrays.asList;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.uima.cas.CAS;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.ValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.annotation.layer.behaviors.LayerBehaviorRegistry;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.api.ChainAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.api.ChainLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.api.ChainLayerTraits;
import de.tudarmstadt.ukp.inception.annotation.layer.span.api.SpanLayerBehavior;
import de.tudarmstadt.ukp.inception.rendering.Renderer;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupport_ImplBase;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerType;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@code AnnotationServiceAutoConfiguration#chainLayerSupport}.
 * </p>
 */
public class ChainLayerSupportImpl
    extends LayerSupport_ImplBase<ChainAdapter, ChainLayerTraits>
    implements InitializingBean, ChainLayerSupport
{
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ApplicationEventPublisher eventPublisher;
    private final LayerBehaviorRegistry layerBehaviorsRegistry;
    private final ConstraintsService constraintsService;

    private String layerSupportId;
    private List<LayerType> types;

    @Autowired
    public ChainLayerSupportImpl(FeatureSupportRegistry aFeatureSupportRegistry,
            ApplicationEventPublisher aEventPublisher,
            LayerBehaviorRegistry aLayerBehaviorsRegistry, ConstraintsService aConstraintsService)
    {
        super(aFeatureSupportRegistry);
        eventPublisher = aEventPublisher;
        layerBehaviorsRegistry = aLayerBehaviorsRegistry;
        constraintsService = aConstraintsService;
    }

    @Override
    public String getId()
    {
        return layerSupportId;
    }

    @Override
    public void setBeanName(String aBeanName)
    {
        layerSupportId = aBeanName;
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        types = asList(new LayerType(TYPE, TYPE_SUFFIX_CHAIN, layerSupportId));
    }

    @Override
    public List<LayerType> getSupportedLayerTypes()
    {
        return types;
    }

    @Override
    public boolean accepts(AnnotationLayer aLayer)
    {
        return TYPE.equals(aLayer.getType());
    }

    @Override
    public ChainAdapter createAdapter(AnnotationLayer aLayer,
            Supplier<Collection<AnnotationFeature>> aFeatures)
    {
        return new ChainAdapterImpl(getLayerSupportRegistry(), featureSupportRegistry,
                eventPublisher, aLayer, aFeatures,
                layerBehaviorsRegistry.getLayerBehaviors(this, SpanLayerBehavior.class),
                constraintsService);
    }

    @Override
    public void generateTypes(TypeSystemDescription aTsd, AnnotationLayer aLayer,
            List<AnnotationFeature> aAllFeaturesInProject)
    {
        var tdChain = aTsd.addType(aLayer.getName() + TYPE_SUFFIX_CHAIN, aLayer.getDescription(),
                CAS.TYPE_NAME_ANNOTATION_BASE);
        tdChain.addFeature(FEATURE_NAME_FIRST, "", aLayer.getName() + TYPE_SUFFIX_LINK);

        // Custom features on chain layers are currently not supported
        // generateFeatures(aTsd, tdChains, type);

        var tdLink = aTsd.addType(aLayer.getName() + TYPE_SUFFIX_LINK, "",
                CAS.TYPE_NAME_ANNOTATION);
        tdLink.addFeature(FEATURE_NAME_NEXT, "", aLayer.getName() + TYPE_SUFFIX_LINK);
        tdLink.addFeature(FEATURE_NAME_REFERENCE, "", CAS.TYPE_NAME_STRING);
        tdLink.addFeature(FEATURE_NAME_REFERENCE_RELATION, "", CAS.TYPE_NAME_STRING);
    }

    @Override
    public List<String> getGeneratedTypeNames(AnnotationLayer aLayer)
    {
        return asList(aLayer.getName() + TYPE_SUFFIX_CHAIN, aLayer.getName() + TYPE_SUFFIX_LINK);
    }

    @Override
    public Renderer createRenderer(AnnotationLayer aLayer,
            Supplier<Collection<AnnotationFeature>> aFeatures)
    {
        return new ChainRenderer(createAdapter(aLayer, aFeatures), getLayerSupportRegistry(),
                featureSupportRegistry,
                layerBehaviorsRegistry.getLayerBehaviors(this, SpanLayerBehavior.class));
    }

    @Override
    public Panel createTraitsEditor(String aId, IModel<AnnotationLayer> aLayerModel)
    {
        var layer = aLayerModel.getObject();

        if (!accepts(layer)) {
            throw unsupportedLayerTypeException(layer);
        }

        return new ChainLayerTraitsEditor(aId, this, aLayerModel);
    }

    @Override
    public ChainLayerTraits createTraits()
    {
        return new ChainLayerTraits();
    }

    @Override
    public List<ValidationError> validateFeatureName(AnnotationFeature aFeature)
    {
        var name = aFeature.getName();

        if (Set.of(FEATURE_NAME_FIRST, FEATURE_NAME_NEXT, FEATURE_NAME_REFERENCE,
                FEATURE_NAME_REFERENCE_RELATION).contains(name)) {
            return asList(new ValidationError("[" + name + "] is a reserved feature name on "
                    + "chain layers. Please use a different name for the feature."));
        }

        return Collections.emptyList();
    }

    @Override
    public boolean isDeletable(AnnotationFeature aFeature)
    {
        if (Set.of(FEATURE_NAME_FIRST, FEATURE_NAME_NEXT, FEATURE_NAME_REFERENCE,
                FEATURE_NAME_REFERENCE_RELATION).contains(aFeature.getName())) {
            return false;
        }

        return true;
    }
}
