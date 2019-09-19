/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.inception.image.feature;

import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
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
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VLazyDetailQuery;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.model.VLazyDetailResult;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;

/**
 * Extension providing image-related features for annotations.
 */
@Component
public class ImageFeatureSupport
    implements FeatureSupport<ImageFeatureTraits>
{
    public static final String PREFIX = "img:";

    public static final String URL = "url";
    public static final String TYPE_IMAGE_URL = PREFIX + URL;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private String featureSupportId;

    @Autowired
    public ImageFeatureSupport()
    {
        // Nothing to do
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
        return asList(new FeatureType(TYPE_IMAGE_URL, "Image URL", featureSupportId));
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
    public Panel createTraitsEditor(String aId, IModel<AnnotationFeature> aFeatureModel)
    {
        return new ImageFeatureTraitsEditor(aId, this, aFeatureModel);
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
            if (feature.getType().startsWith("img:")) {
                editor = new ImageFeatureEditor(aId, aOwner, aFeatureStateModel);
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
    public ImageFeatureTraits readTraits(AnnotationFeature aFeature)
    {
        ImageFeatureTraits traits = null;
        try {
            traits = JSONUtil.fromJsonString(ImageFeatureTraits.class,
                    aFeature.getTraits());
        }
        catch (IOException e) {
            log.error("Unable to read traits", e);
        }
        
        if (traits == null) {
            traits = new ImageFeatureTraits();
        }
                
        return traits;
    }
    
    @Override
    public void writeTraits(AnnotationFeature aFeature, ImageFeatureTraits aTraits)
    {
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

    @Override
    public <V> V unwrapFeatureValue(AnnotationFeature aFeature, CAS aCAS, Object aValue)
    {
        return (V) aValue;
    }

    @Override
    public Object wrapFeatureValue(AnnotationFeature aFeature, CAS aCAS, Object aValue)
    {
        return aValue;
    }
    
    @Override
    public List<VLazyDetailQuery> getLazyDetails(AnnotationFeature aFeature, FeatureStructure aFs)
    {
        String label = renderFeatureValue(aFeature, aFs);
                
        if (StringUtils.isEmpty(label)) {
            return Collections.emptyList();
        }
        
        return asList(new VLazyDetailQuery(aFeature.getName(), label));
    }
    
    @Override
    public List<VLazyDetailResult> renderLazyDetails(AnnotationFeature aFeature, String aQuery)
    {
        return asList(new VLazyDetailResult("<img>", aQuery));
    }
}
