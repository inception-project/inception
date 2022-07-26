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
package de.tudarmstadt.ukp.inception.ui.core.docanno.layer;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import org.apache.uima.cas.CAS;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.NopRenderer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.rendering.Renderer;
import de.tudarmstadt.ukp.inception.schema.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.schema.layer.LayerSupport_ImplBase;
import de.tudarmstadt.ukp.inception.schema.layer.LayerType;
import de.tudarmstadt.ukp.inception.ui.core.docanno.config.DocumentMetadataLayerSupportAutoConfiguration;
import de.tudarmstadt.ukp.inception.ui.core.docanno.config.DocumentMetadataLayerSupportProperties;

/**
 * Support for document-level annotations.
 * <p>
 * This class is exposed as a Spring Component via
 * {@link DocumentMetadataLayerSupportAutoConfiguration#documentMetadataLayerSupport}.
 * </p>
 */
public class DocumentMetadataLayerSupport
    extends LayerSupport_ImplBase<DocumentMetadataLayerAdapter, DocumentMetadataLayerTraits>
    implements InitializingBean
{
    public static final String TYPE = "document-metadata";

    private final ApplicationEventPublisher eventPublisher;
    private final DocumentMetadataLayerSupportProperties properties;

    private String layerSupportId;
    private List<LayerType> types;

    @Autowired
    public DocumentMetadataLayerSupport(FeatureSupportRegistry aFeatureSupportRegistry,
            ApplicationEventPublisher aEventPublisher,
            DocumentMetadataLayerSupportProperties aProperties)
    {
        super(aFeatureSupportRegistry);
        eventPublisher = aEventPublisher;
        properties = aProperties;
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
        DocumentMetadataLayerAdapter adapter = new DocumentMetadataLayerAdapter(
                getLayerSupportRegistry(), featureSupportRegistry, eventPublisher, aLayer,
                aFeatures);

        return adapter;
    }

    @Override
    public void generateTypes(TypeSystemDescription aTsd, AnnotationLayer aLayer,
            List<AnnotationFeature> aAllFeaturesInProject)
    {
        TypeDescription td = aTsd.addType(aLayer.getName(), "", CAS.TYPE_NAME_ANNOTATION_BASE);

        List<AnnotationFeature> featureForLayer = aAllFeaturesInProject.stream()
                .filter(feature -> aLayer.equals(feature.getLayer())).collect(toList());
        generateFeatures(aTsd, td, featureForLayer);
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
        AnnotationLayer layer = aLayerModel.getObject();

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
}
