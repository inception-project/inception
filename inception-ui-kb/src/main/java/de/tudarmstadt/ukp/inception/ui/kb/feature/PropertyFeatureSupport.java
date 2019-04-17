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

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.wicket.MarkupContainer;
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
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;

@Component
public class PropertyFeatureSupport
    implements FeatureSupport<Void>
{
    public static final String PREDICATE_KEY = "KB: Property";
    public static final String PREFIX = "kb-property:";
    
    private static final Logger LOG = LoggerFactory.getLogger(PropertyFeatureSupport.class);

    private final KnowledgeBaseService kbService;
    
    private LoadingCache<Key, String> labelCache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .refreshAfterWrite(1, TimeUnit.MINUTES)
        .build(key -> loadLabelValue(key));
    
    private String featureSupportId;

    @Autowired
    public PropertyFeatureSupport(KnowledgeBaseService aKbService)
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
    public List<FeatureType> getSupportedFeatureTypes(AnnotationLayer aAnnotationLayer)
    {
        List<FeatureType> types = new ArrayList<>();
        types.add(new FeatureType(PREFIX, PREDICATE_KEY, featureSupportId, true));
        return types;
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
            renderValue = labelCache.get(new Key(aFeature, aLabel));
        }
        return renderValue;
    }

    private String loadLabelValue(Key aKey)
    {
        try {
            return kbService.getKnowledgeBases(aKey.getAnnotationFeature().getProject()).stream()
                    .map(k -> kbService.readProperty(k, aKey.getLabel()))
                    .flatMap(o -> o.map(Stream::of).orElseGet(Stream::empty))
                    .map(KBProperty::getUiLabel).findAny().orElseThrow(NoSuchElementException::new);
        }
        catch (Exception e) {
            LOG.error("Unable to render feature value [{}]", aKey.getLabel(), e);
            return "ERROR (" + aKey.getLabel() + ")";
        }
    }

    @Override
    public String unwrapFeatureValue(AnnotationFeature aFeature, CAS aCAS, Object aValue)
    {
        // Normally, we get KBProperty back from the feature editors
        if (aValue instanceof KBProperty) {
            return ((KBProperty) aValue).getIdentifier();
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
    public KBProperty wrapFeatureValue(AnnotationFeature aFeature, CAS aCAS, Object aValue)
    {
        if (aValue instanceof String) {
            String identifier = (String) aValue;
            return new KBProperty(renderFeatureValue(aFeature, identifier), identifier);
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
    public FeatureEditor createEditor(String aId, MarkupContainer aOwner,
            AnnotationActionHandler aHandler, IModel<AnnotatorState> aStateModel,
            IModel<FeatureState> aFeatureStateModel)
    {
        FeatureState featureState = aFeatureStateModel.getObject();
        final FeatureEditor editor;

        switch (featureState.feature.getMultiValueMode()) {
        case NONE:
            if (featureState.feature.getType().startsWith(PREFIX)) {
                editor = new PropertyFeatureEditor(aId, aOwner, aHandler, aStateModel,
                    aFeatureStateModel);
            }
            else {
                throw unsupportedMultiValueModeException(featureState.feature);
            }
            break;
        case ARRAY: // fall-through
        default:
            throw unsupportedMultiValueModeException(featureState.feature);
        }

        return editor;
    }

    @Override
    public void generateFeature(TypeSystemDescription aTSD, TypeDescription aTD,
        AnnotationFeature aFeature)
    {
        aTD.addFeature(aFeature.getName(), "", CAS.TYPE_NAME_STRING);
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

