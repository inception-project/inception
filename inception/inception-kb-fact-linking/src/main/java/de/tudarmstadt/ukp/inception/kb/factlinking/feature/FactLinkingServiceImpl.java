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

package de.tudarmstadt.ukp.inception.kb.factlinking.feature;

import static de.tudarmstadt.ukp.inception.kb.factlinking.feature.FactLinkingConstants.LINKED_LAYER_FEATURE;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.uima.ICasUtil;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureTraits;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.factlinking.config.FactLinkingAutoConfiguration;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBInstance;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.feature.FeatureSupport;
import de.tudarmstadt.ukp.inception.schema.feature.FeatureSupportRegistry;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link FactLinkingAutoConfiguration#factLinkingService}.
 * </p>
 */
@Deprecated
public class FactLinkingServiceImpl
    implements FactLinkingService
{
    @Autowired
    private KnowledgeBaseService kbService;
    @Autowired
    private AnnotationSchemaService annotationService;
    @Autowired
    private FeatureSupportRegistry featureSupportRegistry;

    private final Logger LOG = LoggerFactory.getLogger(getClass());

    @Override
    public List<KBProperty> listProperties(Project aProject, ConceptFeatureTraits traits)
    {
        List<KBProperty> handles = new ArrayList<>();
        if (traits.getRepositoryId() != null) {
            // If a specific KB is selected, get its properties
            Optional<KnowledgeBase> kb = kbService.getKnowledgeBaseById(aProject,
                    traits.getRepositoryId());
            if (kb.isPresent() && kb.get().isEnabled()) {
                handles.addAll(kbService.listProperties(kb.get(), false));
            }
        }
        else {
            for (KnowledgeBase kb : kbService.getEnabledKnowledgeBases(aProject)) {
                handles.addAll(kbService.listProperties(kb, false));
            }
        }
        return handles;
    }

    @Override
    public KBHandle getKBHandleFromCasByAddr(CAS aCas, int targetAddr, Project aProject,
            ConceptFeatureTraits traits)
    {
        FeatureStructure selectedFS = ICasUtil.selectFsByAddr(aCas, targetAddr);
        String kbHandleIdentifier = WebAnnoCasUtil.getFeature(selectedFS, LINKED_LAYER_FEATURE);
        KBHandle kbHandle = null;
        try {
            kbHandle = getKBInstancesByIdentifierAndTraits(kbHandleIdentifier, aProject, traits);
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
                        .filter(KnowledgeBase::isEnabled)
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

        for (KnowledgeBase kb : kbService.getEnabledKnowledgeBases(aProject)) {
            if (kbService.listProperties(kb, false).contains(aProperty)) {
                return kb;
            }
        }

        return null;
    }

    @Override
    public ConceptFeatureTraits getFeatureTraits(Project aProject)
    {
        AnnotationLayer linkedLayer = annotationService.findLayer(aProject,
                NamedEntity.class.getName());
        AnnotationFeature linkedFeature = annotationService.getFeature(LINKED_LAYER_FEATURE,
                linkedLayer);
        FeatureSupport<?> fs = featureSupportRegistry.findExtension(linkedFeature).orElseThrow();
        return (ConceptFeatureTraits) fs.readTraits(linkedFeature);
    }
}
