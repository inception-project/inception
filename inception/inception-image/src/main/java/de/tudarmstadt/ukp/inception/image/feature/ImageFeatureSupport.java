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
package de.tudarmstadt.ukp.inception.image.feature;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.util.FSUtil;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.image.config.ImageSupportAutoConfiguration;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VLazyDetail;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VLazyDetailGroup;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureEditor;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupport;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureType;

/**
 * Extension providing image-related features for annotations.
 * <p>
 * This class is exposed as a Spring Component via
 * {@link ImageSupportAutoConfiguration#imageSidebarFactory}.
 * </p>
 */
public class ImageFeatureSupport
    implements FeatureSupport<ImageFeatureTraits>
{
    public static final String PREFIX = "img:";

    public static final String URL = "url";
    public static final String TYPE_IMAGE_URL = PREFIX + URL;

    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
    public boolean isFeatureValueValid(AnnotationFeature aFeature, FeatureStructure aFS)
    {
        if (aFeature.isRequired()) {
            return isNotBlank(FSUtil.getFeature(aFS, aFeature.getName(), String.class));
        }

        return true;
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
    public ImageFeatureTraits createDefaultTraits()
    {
        return new ImageFeatureTraits();
    }

    @Override
    public void generateFeature(TypeSystemDescription aTSD, TypeDescription aTD,
            AnnotationFeature aFeature)
    {
        aTD.addFeature(aFeature.getName(), "", CAS.TYPE_NAME_STRING);
    }

    @Override
    public <V> V getNullFeatureValue(AnnotationFeature aFeature, FeatureStructure aFS)
    {
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> V unwrapFeatureValue(AnnotationFeature aFeature, Object aValue)
    {
        return (V) aValue;
    }

    @Override
    public Serializable wrapFeatureValue(AnnotationFeature aFeature, CAS aCAS, Object aValue)
    {
        return (Serializable) aValue;
    }

    @Override
    public List<VLazyDetailGroup> lookupLazyDetails(AnnotationFeature aFeature, Object aValue)
    {
        if (aValue instanceof String) {
            var url = (String) aValue;

            if (isBlank(url)) {
                return emptyList();
            }

            return asList(new VLazyDetailGroup(new VLazyDetail("<img>", url)));
        }

        return Collections.emptyList();
    }
}
