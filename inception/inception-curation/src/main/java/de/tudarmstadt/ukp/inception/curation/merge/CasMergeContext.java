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
package de.tudarmstadt.ukp.inception.curation.merge;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.annotation.feature.link.LinkFeatureTraits;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.TypeAdapter;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

public class CasMergeContext
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final AnnotationSchemaService schemaService;

    private final Map<String, AnnotationLayer> layerCache;
    private final LoadingCache<AnnotationLayer, TypeAdapter> adapterCache;
    private final LoadingCache<AnnotationFeature, LinkFeatureTraits> linkTraitsCache;
    private final Map<AnnotationLayer, List<AnnotationFeature>> featureCache;

    private boolean silenceEvents = false;

    public CasMergeContext(AnnotationSchemaService aSchemaService)
    {
        schemaService = aSchemaService;

        layerCache = new HashMap<>();
        featureCache = new HashMap<>();
        adapterCache = Caffeine.newBuilder() //
                .maximumSize(100) //
                .build(schemaService::getAdapter);
        linkTraitsCache = Caffeine.newBuilder() //
                .maximumSize(100) //
                .build(this::_readTraits);
    }

    public void setSilenceEvents(boolean aSilenceEvents)
    {
        silenceEvents = aSilenceEvents;
    }

    public boolean isSilenceEvents()
    {
        return silenceEvents;
    }

    public List<AnnotationFeature> listSupportedFeatures(AnnotationLayer aLayer)
    {
        return featureCache.computeIfAbsent(aLayer,
                key -> schemaService.listSupportedFeatures(key));
    }

    public AnnotationLayer findLayer(Project aProject, String aTypeName)
    {
        return layerCache.computeIfAbsent(aTypeName,
                typeName -> schemaService.findLayer(aProject, typeName));
    }

    public TypeAdapter getAdapter(AnnotationLayer aLayer)
    {
        return adapterCache.get(aLayer);
    }

    public LinkFeatureTraits readLinkTraits(AnnotationFeature aFeature)
    {
        return linkTraitsCache.get(aFeature);
    }

    // Would be better to use this from the LinkFeatureSupport - but I do not want to change the
    // constructor at the moment to inject another dependency.
    private LinkFeatureTraits _readTraits(AnnotationFeature aFeature)
    {
        LinkFeatureTraits traits = null;
        try {
            traits = JSONUtil.fromJsonString(LinkFeatureTraits.class, aFeature.getTraits());
        }
        catch (IOException e) {
            LOG.error("Unable to read traits", e);
        }

        if (traits == null) {
            traits = new LinkFeatureTraits();
        }

        return traits;
    }
}
