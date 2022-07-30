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

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import org.apache.uima.cas.CAS;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst;
import de.tudarmstadt.ukp.inception.annotation.layer.behaviors.LayerBehaviorRegistry;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanLayerBehavior;
import de.tudarmstadt.ukp.inception.rendering.Renderer;
import de.tudarmstadt.ukp.inception.schema.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.layer.LayerSupport_ImplBase;
import de.tudarmstadt.ukp.inception.schema.layer.LayerType;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@code AnnotationServiceAutoConfiguration#chainLayerSupport}.
 * </p>
 */
public class ChainLayerSupport
    extends LayerSupport_ImplBase<ChainAdapter, ChainLayerTraits>
    implements InitializingBean
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ApplicationEventPublisher eventPublisher;
    private final LayerBehaviorRegistry layerBehaviorsRegistry;

    private String layerSupportId;
    private List<LayerType> types;

    @Autowired
    public ChainLayerSupport(FeatureSupportRegistry aFeatureSupportRegistry,
            ApplicationEventPublisher aEventPublisher,
            LayerBehaviorRegistry aLayerBehaviorsRegistry)
    {
        super(aFeatureSupportRegistry);
        eventPublisher = aEventPublisher;
        layerBehaviorsRegistry = aLayerBehaviorsRegistry;
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
        types = asList(new LayerType(WebAnnoConst.CHAIN_TYPE, "Chain", layerSupportId));
    }

    @Override
    public List<LayerType> getSupportedLayerTypes()
    {
        return types;
    }

    @Override
    public boolean accepts(AnnotationLayer aLayer)
    {
        return WebAnnoConst.CHAIN_TYPE.equals(aLayer.getType());
    }

    @Override
    public ChainAdapter createAdapter(AnnotationLayer aLayer,
            Supplier<Collection<AnnotationFeature>> aFeatures)
    {
        ChainAdapter adapter = new ChainAdapter(getLayerSupportRegistry(), featureSupportRegistry,
                eventPublisher, aLayer, aFeatures,
                layerBehaviorsRegistry.getLayerBehaviors(this, SpanLayerBehavior.class));

        return adapter;
    }

    @Override
    public void generateTypes(TypeSystemDescription aTsd, AnnotationLayer aLayer,
            List<AnnotationFeature> aAllFeaturesInProject)
    {
        TypeDescription tdChains = aTsd.addType(aLayer.getName() + "Chain", aLayer.getDescription(),
                CAS.TYPE_NAME_ANNOTATION_BASE);
        tdChains.addFeature("first", "", aLayer.getName() + "Link");

        // Custom features on chain layers are currently not supported
        // generateFeatures(aTsd, tdChains, type);

        TypeDescription tdLink = aTsd.addType(aLayer.getName() + "Link", "",
                CAS.TYPE_NAME_ANNOTATION);
        tdLink.addFeature("next", "", aLayer.getName() + "Link");
        tdLink.addFeature("referenceType", "", CAS.TYPE_NAME_STRING);
        tdLink.addFeature("referenceRelation", "", CAS.TYPE_NAME_STRING);
    }

    @Override
    public List<String> getGeneratedTypeNames(AnnotationLayer aLayer)
    {
        return asList(aLayer.getName() + "Chain", aLayer.getName() + "Link");
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
        AnnotationLayer layer = aLayerModel.getObject();

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
}
