/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer;

import static java.util.Arrays.asList;

import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.ChainAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;

@Component
public class ChainLayerSupport
    implements LayerSupport<ChainAdapter>, InitializingBean
{
    private final FeatureSupportRegistry featureSupportRegistry;
    private final ApplicationEventPublisher eventPublisher;
    private final AnnotationSchemaService schemaService;

    private String layerSupportId;
    private List<LayerType> types;

    @Autowired
    public ChainLayerSupport(FeatureSupportRegistry aFeatureSupportRegistry,
            ApplicationEventPublisher aEventPublisher, AnnotationSchemaService aSchemaService)
    {
        featureSupportRegistry = aFeatureSupportRegistry;
        eventPublisher = aEventPublisher;
        schemaService = aSchemaService;
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
    public ChainAdapter createAdapter(AnnotationLayer aLayer)
    {
        ChainAdapter adapter = new ChainAdapter(featureSupportRegistry, eventPublisher,
                aLayer, aLayer.getId(), aLayer.getName() + ChainAdapter.CHAIN, aLayer.getName(),
                "first", "next", schemaService.listAnnotationFeature(aLayer));

        adapter.setLinkedListBehavior(aLayer.isLinkedListBehavior());

        return adapter;
    }
}
