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

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.setFeature;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureType;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.FeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBInstance;

/**
 * Extension providing knowledge-base-related features for annotations.
 */
@Component
public class ConceptFeatureSupport
    implements FeatureSupport<ConceptFeatureTraits>
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final String PREFIX = "kb:";
    public static final String ANY_CONCEPT = "<ANY>";
    public static final String TYPE_ANY_CONCEPT = PREFIX + ANY_CONCEPT;

    private @Autowired KnowledgeBaseService kbService;
    //private @PersistenceContext EntityManager entityManager;
    
    private String featureSupportId;
    
    @Override
    public String getId()
    {
        return featureSupportId;
    }
    
    @Override
    public void setBeanName(String aBeanName)
    {
        featureSupportId = aBeanName;
    }
    
    @Override
    public Optional<FeatureType> getFeatureType(AnnotationFeature aFeature)
    {
        if (aFeature.getType().startsWith(PREFIX)) {
            return Optional.of(new FeatureType(aFeature.getType(),
                    aFeature.getType().substring(PREFIX.length()), featureSupportId));
        }
        else {
            return Optional.empty();
        }
    }

    @Override
    public List<FeatureType> getSupportedFeatureTypes(AnnotationLayer aAnnotationLayer)
    {
        // We just start with no specific scope at all (ANY) and let the user refine this via
        // the traits editor
        return asList(new FeatureType(TYPE_ANY_CONCEPT, "KB: Concept", featureSupportId));
    }

    @Override
    public boolean accepts(AnnotationFeature aFeature)
    {
        switch (aFeature.getMultiValueMode()) {
        case NONE:
            return aFeature.getType().startsWith(PREFIX);
        case ARRAY: // fall-through
        default:
            return false;
        }
    }

    @Override
    public String renderFeatureValue(AnnotationFeature aFeature, String aLabel)
    {
        try {
            String renderValue = null;

            // FIXME Since this might be called very often during rendering, it *might* be
            // worth to set up an LRU cache instead of relying on the performance of the
            // underlying KB store.
            if (aLabel != null) {
                ConceptFeatureTraits t = readTraits(aFeature);

                // Use the concept from a particular knowledge base
                Optional<KBInstance> instance;
                if (t.getRepositoryId() != null) {
                    instance = kbService
                            .getKnowledgeBaseById(aFeature.getProject(), t.getRepositoryId())
                            .flatMap(kb -> kbService.readInstance(kb, aLabel));
                }
                // Use the concept from any knowledge base (leave KB unselected)
                else {
                    instance = kbService.readInstance(aFeature.getProject(), aLabel);
                }

                renderValue = instance.map(KBInstance::getUiLabel)
                        .orElseThrow(NoSuchElementException::new);
            }
            
            return renderValue;
        }
        catch (Exception e) {
            log.error("Unable to render feature value", e);
            return "ERROR";
        }
    }

    @Override
    public void setFeatureValue(JCas aJcas, AnnotationFeature aFeature, int aAddress, Object aValue)
    {
        KBHandle kbInst = (KBHandle) aValue;
        FeatureStructure fs = selectByAddr(aJcas, FeatureStructure.class, aAddress);
        setFeature(fs, aFeature, kbInst != null ? kbInst.getIdentifier() : null);
    }

    @Override
    public KBHandle getFeatureValue(AnnotationFeature aFeature, FeatureStructure aFS)
    {
        Feature feature = aFS.getType().getFeatureByBaseName(aFeature.getName());
        final String effectiveType = CAS.TYPE_NAME_STRING;
        
        // Sanity check
        if (!Objects.equals(effectiveType, feature.getRange().getName())) {
            throw new IllegalArgumentException("Actual feature type ["
                    + feature.getRange().getName() + "] does not match expected feature type ["
                    + effectiveType + "].");
        }

        String value = (String) WebAnnoCasUtil.getFeature(aFS, aFeature.getName());
        
        if (value != null) {
            ConceptFeatureTraits t = readTraits(aFeature);

            // Use the concept from a particular knowledge base
            Optional<KBInstance> instance;
            if (t.getRepositoryId() != null) {
                instance = kbService
                        .getKnowledgeBaseById(aFeature.getProject(), t.getRepositoryId())
                        .flatMap(kb -> kbService.readInstance(kb, value));
            }
            // Use the concept from any knowledge base (leave KB unselected)
            else {
                instance = kbService.readInstance(aFeature.getProject(), value);
            }

            return instance.map(i -> KBHandle.of(i)).orElseThrow(NoSuchElementException::new);
        }
        else {
            return null;
        }
    }

    @Override
    public Panel createTraitsEditor(String aId, IModel<AnnotationFeature> aFeatureModel)
    {
        return new ConceptFeatureTraitsEditor(aId, this, aFeatureModel);
    }
    
    @Override
    public FeatureEditor createEditor(String aId, MarkupContainer aOwner,
            AnnotationActionHandler aHandler, IModel<AnnotatorState> aStateModel,
            IModel<FeatureState> aFeatureStateModel)
    {
        AnnotationFeature feature = aFeatureStateModel.getObject().feature;
        FeatureEditor editor;

        switch (feature.getMultiValueMode()) {
        case NONE:
            if (feature.getType().startsWith("kb:")) {
                editor = new ConceptFeatureEditor(aId, aOwner, aFeatureStateModel, aStateModel,
                    aHandler);
            }
            else {
                throw unsupportedMultiValueModeException(feature);
            }
            break;
        case ARRAY: // fall-through
        default:
            throw unsupportedMultiValueModeException(feature);
        }

        return editor;
    }

    @Override
    public ConceptFeatureTraits readTraits(AnnotationFeature aFeature)
    {
        ConceptFeatureTraits traits = null;
        try {
            traits = JSONUtil.fromJsonString(ConceptFeatureTraits.class,
                    aFeature.getTraits());
        }
        catch (IOException e) {
            log.error("Unable to read traits", e);
        }
        
        if (traits == null) {
            traits = new ConceptFeatureTraits();
        }
        
        // If there is no scope set in the trait, see if once can be extracted from the legacy
        // location which is the feature type.
        if (traits.getScope() == null && !TYPE_ANY_CONCEPT.equals(aFeature.getType())) {
            traits.setScope(aFeature.getType().substring(PREFIX.length()));
        }
        
        return traits;
    }
    
    @Override
    public void writeTraits(AnnotationFeature aFeature, ConceptFeatureTraits aTraits)
    {
        // Update the feature type with the scope
        if (aTraits.getScope() != null) {
            aFeature.setType(PREFIX + aTraits.getScope());
        }
        else {
            aFeature.setType(TYPE_ANY_CONCEPT);
        }
        
        try {
            aFeature.setTraits(JSONUtil.toJsonString(aTraits));
        }
        catch (IOException e) {
            log.error("Unable to write traits", e);
        }
    }
    
    @Override
    public void generateFeature(TypeSystemDescription aTSD, TypeDescription aTD,
            AnnotationFeature aFeature)
    {
        aTD.addFeature(aFeature.getName(), "", CAS.TYPE_NAME_STRING);
    }
}
