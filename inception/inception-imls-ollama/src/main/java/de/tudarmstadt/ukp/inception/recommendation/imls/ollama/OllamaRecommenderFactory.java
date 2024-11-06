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
package de.tudarmstadt.ukp.inception.recommendation.imls.ollama;

import static org.apache.uima.cas.CAS.TYPE_NAME_STRING;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.ListModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.annotation.layer.span.SpanLayerSupport;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngine;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactoryImplBase;
import de.tudarmstadt.ukp.inception.recommendation.imls.ollama.client.OllamaClient;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.io.WatchedResourceFile;
import de.tudarmstadt.ukp.inception.support.yaml.YamlUtil;
import de.tudarmstadt.ukp.inception.ui.core.docanno.layer.DocumentMetadataLayerSupport;

public class OllamaRecommenderFactory
    extends RecommendationEngineFactoryImplBase<OllamaRecommenderTraits>
{
    // This is a string literal so we can rename/refactor the class without it changing its ID
    // and without the database starting to refer to non-existing recommendation tools.
    public static final String ID = "de.tudarmstadt.ukp.inception.recommendation.imls.ollama.OllamaRecommenderFactory";

    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final AnnotationSchemaService schemaService;
    private final OllamaClient client;

    private WatchedResourceFile<ArrayList<Preset>> presets;

    public OllamaRecommenderFactory(OllamaClient aClient, AnnotationSchemaService aSchemaService)
    {
        client = aClient;
        schemaService = aSchemaService;

        var presetsResource = getClass().getResource("presets.yaml");
        presets = new WatchedResourceFile<>(presetsResource, is -> YamlUtil.getObjectMapper()
                .readValue(is, new TypeReference<ArrayList<Preset>>()
                {
                }));
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return "Ollama Recommender";
    }

    @Override
    public RecommendationEngine build(Recommender aRecommender)
    {
        OllamaRecommenderTraits traits = readTraits(aRecommender);
        return new OllamaRecommender(aRecommender, traits, client, schemaService);
    }

    @Override
    public boolean accepts(AnnotationLayer aLayer, AnnotationFeature aFeature)
    {
        return (SpanLayerSupport.TYPE.equals(aFeature.getLayer().getType())
                || DocumentMetadataLayerSupport.TYPE.equals(aFeature.getLayer().getType()))
                && TYPE_NAME_STRING.equals(aFeature.getType());
    }

    @Override
    public OllamaRecommenderTraitsEditor createTraitsEditor(String aId, IModel<Recommender> aModel)
    {
        return new OllamaRecommenderTraitsEditor(aId, aModel, new ListModel<>(getPresets()));
    }

    @Override
    public OllamaRecommenderTraits createTraits()
    {
        return new OllamaRecommenderTraits();
    }

    @Override
    public boolean isEvaluable()
    {
        return false;
    }

    @Override
    public boolean isMultipleRecommendationProvider()
    {
        return false;
    }

    @Override
    public boolean isInteractive(Recommender aRecommender)
    {
        return readTraits(aRecommender).isInteractive();
    }

    @Override
    public Panel createInteractionPanel(String aId, IModel<Recommender> aModel)
    {
        return new OllamaInteractionPanel(aId, aModel, new ListModel<>(getPresets()));
    }

    private List<Preset> getPresets()
    {
        try {
            return presets.get().get();
        }
        catch (Exception e) {
            LOG.error("Unable to load presets", e);
            return Collections.emptyList();
        }
    }
}
