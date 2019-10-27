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

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureType;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.FeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VLazyDetailQuery;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VLazyDetailResult;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.inception.kb.ConceptFeatureTraits;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBErrorHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBObject;

/**
 * Extension providing knowledge-base-related features for annotations.
 */
@Component
public class ConceptFeatureSupport
    implements FeatureSupport<ConceptFeatureTraits>
{
    public static final String PREFIX = "kb:";

    public static final String ANY_OBJECT = "<ANY>";
    public static final String TYPE_ANY_OBJECT = PREFIX + ANY_OBJECT;

    
    private static final Logger LOG = LoggerFactory.getLogger(ConceptFeatureSupport.class);

    private final KnowledgeBaseService kbService;
    
    private LoadingCache<Key, KBHandle> labelCache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .refreshAfterWrite(1, TimeUnit.MINUTES)
        .build(key -> loadLabelValue(key));
    
    private String featureSupportId;

    @Autowired
    public ConceptFeatureSupport(KnowledgeBaseService aKbService)
    {
        kbService = aKbService;
    }
    
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
        return asList(new FeatureType(TYPE_ANY_OBJECT, "KB: Concept/Instance", featureSupportId));
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
        String renderValue = null;
        if (aLabel != null) {
            return labelCache.get(new Key(aFeature, aLabel)).getUiLabel();
        }
        return renderValue;
    }
    
    private KBHandle loadLabelValue(Key aKey)
    {
        try {
            ConceptFeatureTraits t = readTraits(aKey.getAnnotationFeature());
    
            // Use the concept from a particular knowledge base
            Optional<KBObject> kbObject;
            if (t.getRepositoryId() != null) {
                kbObject = kbService
                        .getKnowledgeBaseById(aKey.getAnnotationFeature().getProject(),
                                t.getRepositoryId())
                        .flatMap(kb -> kbService.readItem(kb, aKey.getLabel()));
            }
            
            // Use the concept from any knowledge base (leave KB unselected)
            else {
                kbObject = kbService.readItem(aKey.getAnnotationFeature().getProject(),
                        aKey.getLabel());

            }
            return kbObject.map(KBObject::toKBHandle).orElseThrow(NoSuchElementException::new);
        }
        catch (NoSuchElementException e) {
            LOG.error("No label for feature value [{}]", aKey.getLabel());
            return new KBErrorHandle("NO LABEL (" + aKey.getLabel() + ")", e);
        }
        catch (Exception e) {
            LOG.error("Unable to obtain label value for feature value [{}]", aKey.getLabel(), e);
            return new KBErrorHandle("ERROR (" + aKey.getLabel() + ")", e);
        }
    }

    @Override
    public String unwrapFeatureValue(AnnotationFeature aFeature, CAS aCAS, Object aValue)
    {
        // Normally, we get KBHandles back from the feature editors
        if (aValue instanceof KBHandle) {
            return ((KBHandle) aValue).getIdentifier();
        }
        // When used in a recommendation context, we might get the concept identifier as a string
        // value.
        else if (aValue instanceof String || aValue == null) {
            return (String) aValue;
        }
        else {
            throw new IllegalArgumentException(
                    "Unable to handle value [" + aValue + "] of type [" + aValue.getClass() + "]");
        }
    }

    @Override
    public KBHandle wrapFeatureValue(AnnotationFeature aFeature, CAS aCAS, Object aValue)
    {
        if (aValue instanceof String) {
            String identifier = (String) aValue;
            return new KBHandle(identifier, renderFeatureValue(aFeature, identifier));
        }
        else if (aValue instanceof KBHandle) {
            return (KBHandle) aValue;
        }
        else if (aValue == null ) {
            return null;
        }
        else {
            throw new IllegalArgumentException(
                    "Unable to handle value [" + aValue + "] of type [" + aValue.getClass() + "]");
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
            LOG.error("Unable to read traits", e);
        }
        
        if (traits == null) {
            traits = new ConceptFeatureTraits();
        }
        
        // If there is no scope set in the trait, see if once can be extracted from the legacy
        // location which is the feature type.
        if (traits.getScope() == null && !TYPE_ANY_OBJECT.equals(aFeature.getType())) {
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
            aFeature.setType(TYPE_ANY_OBJECT);
        }
        
        try {
            aFeature.setTraits(JSONUtil.toJsonString(aTraits));
        }
        catch (IOException e) {
            LOG.error("Unable to write traits", e);
        }
    }
    
    @Override
    public void generateFeature(TypeSystemDescription aTSD, TypeDescription aTD,
            AnnotationFeature aFeature)
    {
        aTD.addFeature(aFeature.getName(), "", CAS.TYPE_NAME_STRING);
    }
    
    @Override
    public List<VLazyDetailQuery> getLazyDetails(AnnotationFeature aFeature, String aLabel)
    {
        if (StringUtils.isEmpty(aLabel)) {
            return Collections.emptyList();
        }
        
        return asList(new VLazyDetailQuery(aFeature.getName(), aLabel));
    }
    
    @Override
    public List<VLazyDetailResult> renderLazyDetails(AnnotationFeature aFeature, String aQuery)
    {
        List<VLazyDetailResult> result = new ArrayList<>();
        
        KBHandle handle = labelCache.get(new Key(aFeature, aQuery));

        result.add(new VLazyDetailResult("Label", handle.getUiLabel()));

        if (isNotBlank(handle.getDescription())) {
            result.add(new VLazyDetailResult("Description", handle.getDescription()));
        }
        
        return result;
    }

    private class Key
    {
        private final AnnotationFeature feature;
        private final String label;
        
        public Key(AnnotationFeature aFeature, String aLabel)
        {
            super();
            feature = aFeature;
            label = aLabel;
        }
        
        public String getLabel()
        {
            return label;
        }
        
        public AnnotationFeature getAnnotationFeature()
        {
            return feature;
        }
        
        @Override
        public boolean equals(final Object other)
        {
            if (!(other instanceof Key)) {
                return false;
            }
            Key castOther = (Key) other;
            return new EqualsBuilder().append(feature, castOther.feature)
                    .append(label, castOther.label).isEquals();
        }

        @Override
        public int hashCode()
        {
            return new HashCodeBuilder().append(feature).append(label).toHashCode();
        }
    }
}
