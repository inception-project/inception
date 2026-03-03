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
package de.tudarmstadt.ukp.inception.annotation.layer.document;

import static java.util.Arrays.asList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.apache.uima.cas.CAS;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.ValidationError;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.NopRenderer;
import de.tudarmstadt.ukp.clarin.webanno.constraints.ConstraintsService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.annotation.layer.behaviors.LayerBehaviorRegistry;
import de.tudarmstadt.ukp.inception.annotation.layer.document.api.DocumentMetadataLayerAdapter;
import de.tudarmstadt.ukp.inception.annotation.layer.document.api.DocumentMetadataLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.document.api.DocumentMetadataLayerTraits;
import de.tudarmstadt.ukp.inception.annotation.layer.document.config.DocumentMetadataLayerSupportAutoConfiguration;
import de.tudarmstadt.ukp.inception.annotation.layer.document.config.DocumentMetadataLayerSupportProperties;
import de.tudarmstadt.ukp.inception.rendering.Renderer;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupport_ImplBase;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerType;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerTypes;

/**
 * Support for document-level annotations.
 * <p>
 * This class is exposed as a Spring Component via
 * {@link DocumentMetadataLayerSupportAutoConfiguration#documentMetadataLayerSupport}.
 * </p>
 */
public class DocumentMetadataLayerSupportImpl
    extends LayerSupport_ImplBase<DocumentMetadataLayerAdapter, DocumentMetadataLayerTraits>
    implements InitializingBean, DocumentMetadataLayerSupport
{
    public static final String FEATURE_NAME_ORDER = "i7n_uiOrder";

    public static final String TYPE = LayerTypes.DOCUMENT_LAYER_TYPE;

    private final ApplicationEventPublisher eventPublisher;
    private final DocumentMetadataLayerSupportProperties properties;
    private final ConstraintsService constraintsService;
    private final LayerBehaviorRegistry layerBehaviorsRegistry;

    private String layerSupportId;
    private List<LayerType> types;

    @Autowired
    public DocumentMetadataLayerSupportImpl(FeatureSupportRegistry aFeatureSupportRegistry,
            ApplicationEventPublisher aEventPublisher,
            DocumentMetadataLayerSupportProperties aProperties,
            LayerBehaviorRegistry aLayerBehaviorsRegistry, ConstraintsService aConstraintsService)
    {
        super(aFeatureSupportRegistry);
        eventPublisher = aEventPublisher;
        properties = aProperties;
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
        types = asList(
                new LayerType(TYPE, "Document metadata", layerSupportId, !properties.isEnabled()));
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
    public DocumentMetadataLayerAdapter createAdapter(AnnotationLayer aLayer,
            Supplier<Collection<AnnotationFeature>> aFeatures)
    {
        return new DocumentMetadataLayerAdapterImpl(getLayerSupportRegistry(),
                featureSupportRegistry, eventPublisher, aLayer, aFeatures, constraintsService,
                layerBehaviorsRegistry.getLayerBehaviors(this,
                        DocumentMetadataLayerBehavior.class));
    }

    @Override
    public void generateTypes(TypeSystemDescription aTsd, AnnotationLayer aLayer,
            List<AnnotationFeature> aAllFeaturesInProject)
    {
        var td = aTsd.addType(aLayer.getName(), "", CAS.TYPE_NAME_ANNOTATION_BASE);

        td.addFeature(FEATURE_NAME_ORDER, "", CAS.TYPE_NAME_INTEGER);

        var featuresForLayer = aAllFeaturesInProject.stream() //
                .filter(feature -> aLayer.equals(feature.getLayer())) //
                .toList();

        generateFeatures(aTsd, td, featuresForLayer);
    }

    @Override
    public Renderer createRenderer(AnnotationLayer aLayer,
            Supplier<Collection<AnnotationFeature>> aFeatures)
    {
        return new NopRenderer(createAdapter(aLayer, aFeatures), getLayerSupportRegistry(),
                featureSupportRegistry);
    }

    @Override
    public Panel createTraitsEditor(String aId, IModel<AnnotationLayer> aLayerModel)
    {
        var layer = aLayerModel.getObject();

        if (!accepts(layer)) {
            throw unsupportedLayerTypeException(layer);
        }

        return new DocumentMetadataLayerTraitsEditor(aId, this, aLayerModel);
    }

    @Override
    public DocumentMetadataLayerTraits createTraits()
    {
        return new DocumentMetadataLayerTraits();
    }

    @Override
    public List<ValidationError> validateFeatureName(AnnotationFeature aFeature)
    {
        var name = aFeature.getName();
        if (name.equals(FEATURE_NAME_ORDER)) {
            return asList(new ValidationError("[" + name + "] is a reserved feature name on "
                    + "document metadata layers. Please use a different name for the feature."));
        }

        return Collections.emptyList();
    }
}
