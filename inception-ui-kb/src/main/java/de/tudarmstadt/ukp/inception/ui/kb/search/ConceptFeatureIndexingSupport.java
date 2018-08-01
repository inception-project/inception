/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.ui.kb.search;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.uima.cas.text.AnnotationFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBConcept;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBInstance;
import de.tudarmstadt.ukp.inception.kb.graph.KBObject;
import de.tudarmstadt.ukp.inception.search.FeatureIndexingSupport;

@Component
public class ConceptFeatureIndexingSupport
    implements FeatureIndexingSupport
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final String KB_ENTITY = "KB.Entity";
    public static final String INDEX_KB_CONCEPT = "class";
    public static final String INDEX_KB_INSTANCE = "instance";
    public static final String INDEX_KB_SUPER_CONCEPT = "super.concept";
    
    
    private String id;

    private final FeatureSupportRegistry featureSupportRegistry;
    private final KnowledgeBaseService kbService;

    @Autowired
    public ConceptFeatureIndexingSupport(FeatureSupportRegistry aFeatureSupportRegistry,
            KnowledgeBaseService aKbService)
    {
        featureSupportRegistry = aFeatureSupportRegistry;
        kbService = aKbService;
    }
    
    @Override
    public String getId()
    {
        return id;
    }
    
    @Override
    public void setBeanName(String aBeanName)
    {
        id = aBeanName;
    }
    
    @Override
    public boolean accepts(AnnotationFeature aFeature)
    {
        switch (aFeature.getMultiValueMode()) {
        case NONE:
            return aFeature.getType().startsWith("kb:");
        case ARRAY: // fall-through
        default:
            return false;
        }
    }
    
    @Override
    public Map<String, String> indexFeatureValue(AnnotationFeature aFeature,
            AnnotationFS aAnnotation)
    {
        // Returns KB IRI label after checking if the
        // feature type is associated with KB and feature value is not null
        FeatureSupport<?> featSup = featureSupportRegistry.getFeatureSupport(aFeature);
        KBHandle featureObject = featSup.getFeatureValue(aFeature, aAnnotation);
        
        // Feature value is not set
        if (featureObject == null) {
            return Collections.emptyMap();
        }

        // === BEGIN NEEDS REFACTORING =====================================================
        // See comment below.
        Optional<KBObject> kbObject = kbService.readKBIdentifier(aFeature.getProject(),
                WebAnnoCasUtil.getFeature(aAnnotation, aFeature.getName()));
        // === END NEEDS REFACTORING =======================================================

        if (!kbObject.isPresent()) {
            return Collections.emptyMap();
        }
        
        Map<String, String> values = new HashMap<>();
        
        String objectType;
        // === BEGIN NEEDS REFACTORING =====================================================
        // As part of issue #244, this needs to be refactored for a more reliable method of
        // detecting whether an IRI refers to a class or to an instance.
        // 
        if (kbObject.get() instanceof KBConcept) {
            objectType = INDEX_KB_CONCEPT;
        }
        else if (kbObject.get() instanceof KBInstance) {
            objectType = INDEX_KB_INSTANCE;
        }
        else {
            throw new IllegalStateException("Unknown KB object: [" + kbObject.get() + "]");
        }

        String field = replaceSpace(aFeature.getLayer().getUiName());
        
        // Indexing UI label with type i.e Concept/Instance
        values.put(field + "." + aFeature.getUiName() + "." + objectType,
                featureObject.getUiLabel());
        // === END NEEDS REFACTORING =======================================================

        // Indexing <feature>=<UI label>
        values.put(field + "." + aFeature.getUiName(), featureObject.getUiLabel());

        // Indexing <feature>=<URI>
        values.put(field + "." + aFeature.getUiName(), kbObject.get().getIdentifier());

        // Indexing UI label without type and layer for generic search
        values.put(KB_ENTITY, featureObject.getUiLabel());
        
        // Indexing super concepts with type super.concept 
        Set<KBHandle> listParentConcepts = kbService.getParentConceptList(kbObject.get().getKB(),
                kbObject.get().getIdentifier(), false);
        for (KBHandle parentConcept : listParentConcepts) {
            if (kbService.hasImplicitNamespace(parentConcept.getIdentifier())) {
                continue;
            }
            values.put(field + "." + aFeature.getUiName() + "." + INDEX_KB_SUPER_CONCEPT,
                    parentConcept.getUiLabel());
        }
        
        return values;
    }
    
    public static String replaceSpace(String s) {
        return s.replaceAll(" ", "_");  
    }
    
    
}
