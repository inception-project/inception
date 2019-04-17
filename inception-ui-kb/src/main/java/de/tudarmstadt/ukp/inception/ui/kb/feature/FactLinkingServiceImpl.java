/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.tudarmstadt.ukp.inception.ui.kb.feature;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectFsByAddr;
import static de.tudarmstadt.ukp.inception.ui.kb.feature.FactLinkingConstants.LINKED_LAYER_FEATURE;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureTraits;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBInstance;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

@Component(FactLinkingService.SERVICE_NAME)
public class FactLinkingServiceImpl implements FactLinkingService
{
    @Autowired private KnowledgeBaseService kbService;
    @Autowired private AnnotationSchemaService annotationService;
    @Autowired private FeatureSupportRegistry featureSupportRegistry;

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    @Override
    public List<KBProperty> listProperties(Project aProject, ConceptFeatureTraits traits)
    {
        List<KBProperty> handles = new ArrayList<>();
        if (traits.getRepositoryId() != null) {
            // If a specific KB is selected, get its properties
            Optional<KnowledgeBase> kb = kbService
                .getKnowledgeBaseById(aProject, traits.getRepositoryId());
            if (kb.isPresent()) {
                handles.addAll(kbService.listProperties(kb.get(), false));
            }
        }
        else {
            for (KnowledgeBase kb : kbService.getKnowledgeBases(aProject)) {
                handles.addAll(kbService.listProperties(kb, false));
            }
        }
        return handles;
    }

    @Override
    public KBHandle getKBHandleFromCasByAddr(CAS aCas, int targetAddr, Project aProject,
        ConceptFeatureTraits traits)
    {
        FeatureStructure selectedFS = selectFsByAddr(aCas, targetAddr);
        String kbHandleIdentifier = WebAnnoCasUtil.getFeature(selectedFS, LINKED_LAYER_FEATURE);
        KBHandle kbHandle = null;
        try {
            kbHandle = getKBInstancesByIdentifierAndTraits(kbHandleIdentifier, aProject,
                traits);
        }
        catch (Exception e) {
            LOG.error("Error: " + e.getMessage(), e);
        }
        return kbHandle;
    }

    @Override
    public KBHandle getKBInstancesByIdentifierAndTraits(String kbHandleIdentifier, Project aProject,
        ConceptFeatureTraits traits)
    {
        KBHandle kbHandle = null;
        if (kbHandleIdentifier != null) {
            Optional<KBInstance> instance;
            // Use the concept from a particular knowledge base
            if (traits.getRepositoryId() != null) {
                instance = kbService.getKnowledgeBaseById(aProject, traits.getRepositoryId())
                    .flatMap(kb -> kbService.readInstance(kb, kbHandleIdentifier));
            }
            // Use the concept from any knowledge base (leave KB unselected)
            else {
                instance = kbService.readInstance(aProject, kbHandleIdentifier);
            }
            return instance.map(i -> KBHandle.of(i)).orElse(null);
        }
        return kbHandle;
    }

    @Override
    public KnowledgeBase findKnowledgeBaseContainingProperty(KBProperty aProperty, Project aProject,
        ConceptFeatureTraits traits)
    {
        if (traits.getRepositoryId() != null) {
            return kbService.getKnowledgeBaseById(aProject, traits.getRepositoryId()).get();
        }
        else {
            for (KnowledgeBase kb : kbService.getKnowledgeBases(aProject)) {
                if (kbService.listProperties(kb, false).contains(aProperty)) {
                    return kb;
                }
            }
        }
        return null;
    }

    @Override
    public ConceptFeatureTraits getFeatureTraits(Project aProject)
    {
        AnnotationLayer linkedLayer = annotationService
            .getLayer(NamedEntity.class.getName(), aProject);
        AnnotationFeature linkedFeature = annotationService
            .getFeature(FactLinkingConstants.LINKED_LAYER_FEATURE, linkedLayer);
        FeatureSupport<ConceptFeatureTraits> fs = featureSupportRegistry
            .getFeatureSupport(linkedFeature);
        ConceptFeatureTraits traits = fs.readTraits(linkedFeature);
        return traits;
    }

}
