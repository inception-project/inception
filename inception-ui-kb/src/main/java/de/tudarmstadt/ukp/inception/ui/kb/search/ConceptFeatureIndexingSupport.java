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

import java.util.Optional;
import java.util.Set;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
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
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
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
    public static final String INDEX_KB_EXACT = "exact";
    
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
    public MultiValuedMap<String, String> indexFeatureValue(AnnotationFeature aFeature,
            AnnotationFS aAnnotation)
    {
        // Returns KB IRI label after checking if the
        // feature type is associated with KB and feature value is not null
        FeatureSupport<?> featSup = featureSupportRegistry.getFeatureSupport(aFeature);
        KBHandle featureObject = featSup.getFeatureValue(aFeature, aAnnotation);
        MultiValuedMap<String, String> values = new HashSetValuedHashMap<String, String>();
        
        // Feature value is not set
        if (featureObject == null) {
            return values;
        }

        // Get object from the KB
        Optional<KBObject> kbObject = kbService.readKBIdentifier(aFeature.getProject(),
                WebAnnoCasUtil.getFeature(aAnnotation, aFeature.getName()));

        if (!kbObject.isPresent()) {
            return values;
        }

        String field = aFeature.getLayer().getUiName();
        
        // Indexing <layer>.<feature>.exact=<UI label>
        values.put(field + "." + aFeature.getUiName() + "." + INDEX_KB_EXACT,
                featureObject.getUiLabel());
        // Indexing <layer>.<feature>=<UI label>
        values.put(field + "." + aFeature.getUiName(),
                featureObject.getUiLabel());
        // Indexing: <layer>.<feature>.exact=<URI>
        values.put(field + "." + aFeature.getUiName() + "." + INDEX_KB_EXACT,
                kbObject.get().getIdentifier());
        // Indexing: <layer>.<feature>=<URI>
        values.put(field + "." + aFeature.getUiName(),
                kbObject.get().getIdentifier());

        // Indexing UI label without type and layer for generic search
        values.put(KB_ENTITY, featureObject.getUiLabel());
        
        // Indexing super concepts with type super.concept 
        Set<KBHandle> listParentConcepts = kbService.getParentConceptList(kbObject.get().getKB(),
                kbObject.get().getIdentifier(), false);
        for (KBHandle parentConcept : listParentConcepts) {
            if (kbService.hasImplicitNamespace(parentConcept.getIdentifier())) {
                continue;
            }
            values.put(field + "." + aFeature.getUiName(), parentConcept.getUiLabel());
        }
        return values;
    }
    
}
