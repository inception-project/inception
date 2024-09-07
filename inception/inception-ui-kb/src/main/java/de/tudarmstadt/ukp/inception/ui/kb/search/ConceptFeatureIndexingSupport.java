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
package de.tudarmstadt.ukp.inception.ui.kb.search;

import static de.tudarmstadt.ukp.inception.kb.IriConstants.hasImplicitNamespace;

import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.uima.cas.text.AnnotationFS;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBObject;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupport;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.inception.search.FeatureIndexingSupport;
import de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil;
import de.tudarmstadt.ukp.inception.ui.kb.config.KnowledgeBaseServiceUIAutoConfiguration;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link KnowledgeBaseServiceUIAutoConfiguration#conceptFeatureIndexingSupport}.
 * </p>
 */
public class ConceptFeatureIndexingSupport
    implements FeatureIndexingSupport
{
    public static final String KB_ENTITY = "KB" + SPECIAL_SEP + "Entity";
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
    public MultiValuedMap<String, String> indexFeatureValue(String aFieldPrefix,
            AnnotationFS aAnnotation, String aFeaturePrefix, AnnotationFeature aFeature)
    {
        // Returns KB IRI label after checking if the
        // feature type is associated with KB and feature value is not null
        FeatureSupport<?> featSup = featureSupportRegistry.findExtension(aFeature).orElseThrow();
        KBHandle featureObject = featSup.getFeatureValue(aFeature, aAnnotation);
        MultiValuedMap<String, String> values = new HashSetValuedHashMap<String, String>();

        // Feature value is not set
        if (featureObject == null) {
            return values;
        }

        // Get object from the KB
        Optional<KBHandle> kbObject = kbService.readHandle(aFeature.getProject(),
                WebAnnoCasUtil.getFeature(aAnnotation, aFeature.getName()));

        if (!kbObject.isPresent()) {
            return values;
        }

        String field = aFieldPrefix;

        // Indexing <layer>.<feature>-exact=<UI label>
        values.put(featureIndexName(field, aFeaturePrefix, aFeature), featureObject.getUiLabel());
        // Indexing <layer>.<feature>=<UI label>
        values.put(field + ATTRIBUTE_SEP + aFeature.getUiName(), featureObject.getUiLabel());
        // Indexing: <layer>.<feature>-exact=<URI>
        values.put(featureIndexName(field, aFeaturePrefix, aFeature),
                featureObject.getIdentifier());
        // Indexing: <layer>.<feature>=<URI>
        values.put(field + ATTRIBUTE_SEP + aFeature.getUiName(), featureObject.getIdentifier());

        // The following fields are used by the mentions panes on the KB page in order to find all
        // mentions of a given resource irrespective of which layer/feature they are linked to.
        // Indexing: KB.Entity=<UI label>
        values.put(KB_ENTITY, featureObject.getUiLabel());
        // Indexing: KB.Entity=<URI>
        values.put(KB_ENTITY, featureObject.getIdentifier());

        // Indexing super concepts with type super.concept
        KBObject kbObj = kbObject.get();
        List<KBHandle> listParentConcepts = kbService.getParentConceptList(kbObj.getKB(),
                kbObj.getIdentifier(), false);
        for (KBHandle parentConcept : listParentConcepts) {
            if (hasImplicitNamespace(kbObj.getKB(), parentConcept.getIdentifier())) {
                continue;
            }
            values.put(field + aFeaturePrefix + ATTRIBUTE_SEP + aFeature.getUiName(),
                    parentConcept.getUiLabel());
        }

        return values;
    }

    @Override
    public String featureIndexName(String aFieldPrefix, String aFeaturePrefix,
            AnnotationFeature aFeature)
    {
        return aFieldPrefix + ATTRIBUTE_SEP + aFeature.getUiName() + SPECIAL_SEP + INDEX_KB_EXACT;
    }

}
