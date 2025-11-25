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
package de.tudarmstadt.ukp.inception.annotation.layer.relation;

import static java.util.Arrays.asList;
import static org.apache.uima.cas.CAS.TYPE_NAME_ANNOTATION;

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
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.annotation.layer.behaviors.LayerBehaviorRegistry;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerTraits;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.behavior.RelationLayerBehavior;
import de.tudarmstadt.ukp.inception.rendering.Renderer;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupport_ImplBase;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerType;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerTypes;
import de.tudarmstadt.ukp.inception.support.WebAnnoConst;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@code AnnotationServiceAutoConfiguration#relationLayerSupport}.
 * </p>
 */
public class RelationLayerSupportImpl
    extends LayerSupport_ImplBase<RelationAdapter, RelationLayerTraits>
    implements InitializingBean, RelationLayerSupport
{
    @SuppressWarnings("deprecation")
    public static final String FEAT_REL_TARGET = WebAnnoConst.FEAT_REL_TARGET;

    @SuppressWarnings("deprecation")
    public static final String FEAT_REL_SOURCE = WebAnnoConst.FEAT_REL_SOURCE;

    @SuppressWarnings("deprecation")
    public static final String TYPE = LayerTypes.RELATION_LAYER_TYPE;

    private final ApplicationEventPublisher eventPublisher;
    private final LayerBehaviorRegistry layerBehaviorsRegistry;
    private final ConstraintsService constraintsService;

    private String layerSupportId;
    private List<LayerType> types;

    @Autowired
    public RelationLayerSupportImpl(FeatureSupportRegistry aFeatureSupportRegistry,
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
        types = asList(new LayerType(TYPE, "Relation", layerSupportId));
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
    public RelationAdapter createAdapter(AnnotationLayer aLayer,
            Supplier<Collection<AnnotationFeature>> aFeatures)
    {
        return new RelationAdapterImpl(getLayerSupportRegistry(), featureSupportRegistry,
                eventPublisher, aLayer, FEAT_REL_TARGET, FEAT_REL_SOURCE, aFeatures,
                layerBehaviorsRegistry.getLayerBehaviors(this, RelationLayerBehavior.class),
                constraintsService);
    }

    @Override
    public void generateTypes(TypeSystemDescription aTsd, AnnotationLayer aLayer,
            List<AnnotationFeature> aAllFeaturesInProject)
    {
        var td = aTsd.addType(aLayer.getName(), aLayer.getDescription(), TYPE_NAME_ANNOTATION);
        var attachType = aLayer.getAttachType();

        if (attachType != null) {
            td.addFeature(FEAT_REL_TARGET, "", attachType.getName());
            td.addFeature(FEAT_REL_SOURCE, "", attachType.getName());
        }
        else {
            td.addFeature(FEAT_REL_TARGET, "", CAS.TYPE_NAME_ANNOTATION);
            td.addFeature(FEAT_REL_SOURCE, "", CAS.TYPE_NAME_ANNOTATION);
        }

        var featureForLayer = aAllFeaturesInProject.stream() //
                .filter(feature -> aLayer.equals(feature.getLayer())) //
                .toList();
        generateFeatures(aTsd, td, featureForLayer);
    }

    @Override
    public Renderer createRenderer(AnnotationLayer aLayer,
            Supplier<Collection<AnnotationFeature>> aFeatures)
    {
        return new RelationRenderer(createAdapter(aLayer, aFeatures), getLayerSupportRegistry(),
                featureSupportRegistry,
                layerBehaviorsRegistry.getLayerBehaviors(this, RelationLayerBehavior.class));
    }

    @Override
    public Panel createTraitsEditor(String aId, IModel<AnnotationLayer> aLayerModel)
    {
        var layer = aLayerModel.getObject();

        if (!accepts(layer)) {
            throw unsupportedLayerTypeException(layer);
        }

        return new RelationLayerTraitsEditor(aId, this, aLayerModel);
    }

    @Override
    public RelationLayerTraits createTraits()
    {
        return new RelationLayerTraits();
    }

    @Override
    public List<ValidationError> validateFeatureName(AnnotationFeature aFeature)
    {
        var name = aFeature.getName();
        if (name.equals(FEAT_REL_SOURCE) || name.equals(FEAT_REL_TARGET)) {
            return asList(new ValidationError("[" + name + "] is a reserved feature name on "
                    + "relation layers. Please use a different name for the feature."));
        }

        return Collections.emptyList();
    }

    @Override
    public boolean isDeletable(AnnotationFeature aFeature)
    {
        if (Set.of(FEAT_REL_SOURCE, FEAT_REL_TARGET).contains(aFeature.getName())) {
            return false;
        }

        return true;
    }
}
