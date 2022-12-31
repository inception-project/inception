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
package de.tudarmstadt.ukp.inception.feature.lookup;

import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.feature.lookup.config.LookupServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.feature.lookup.config.LookupServiceProperties;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VLazyDetailQuery;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VLazyDetailResult;
import de.tudarmstadt.ukp.inception.schema.feature.FeatureEditor;
import de.tudarmstadt.ukp.inception.schema.feature.FeatureSupport;
import de.tudarmstadt.ukp.inception.schema.feature.FeatureType;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link LookupServiceAutoConfiguration#lookupFeatureSupport}.
 * </p>
 */
public class LookupFeatureSupport
    implements FeatureSupport<LookupFeatureTraits>
{
    public static final String PREFIX = "lookup:";
    public static final String STRING = "string";
    public static final String TYPE_STRING_LOOKUP = PREFIX + STRING;

    private static final Logger LOG = LoggerFactory.getLogger(LookupFeatureSupport.class);

    private final LookupCache labelCache;
    private final LookupServiceProperties properties;

    private String featureSupportId;

    public LookupFeatureSupport(LookupCache aLabelCache, LookupServiceProperties aProperties)
    {
        labelCache = aLabelCache;
        properties = aProperties;
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
        if (!aFeature.getType().startsWith(PREFIX)) {
            return Optional.empty();
        }

        return Optional.of(new FeatureType(aFeature.getType(),
                aFeature.getType().substring(PREFIX.length()), featureSupportId));
    }

    @Override
    public List<FeatureType> getSupportedFeatureTypes(AnnotationLayer aAnnotationLayer)
    {
        return asList(new FeatureType(TYPE_STRING_LOOKUP, "Lookup", featureSupportId));
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
    public String renderFeatureValue(AnnotationFeature aFeature, String aId)
    {
        if (aId == null) {
            return null;
        }

        LookupFeatureTraits traits = readTraits(aFeature);
        return labelCache.get(aFeature, traits, aId).getUiLabel();
    }

    @SuppressWarnings("unchecked")
    @Override
    public String unwrapFeatureValue(AnnotationFeature aFeature, CAS aCAS, Object aValue)
    {
        // When used in a recommendation context, we might get the concept identifier as a string
        // value.
        if (aValue == null || aValue instanceof String) {
            return (String) aValue;
        }

        // Normally, we get LookupEntrys back from the feature editors
        if (aValue instanceof LookupEntry) {
            return ((LookupEntry) aValue).getIdentifier();
        }

        throw new IllegalArgumentException(
                "Unable to handle value [" + aValue + "] of type [" + aValue.getClass() + "]");
    }

    @Override
    public LookupEntry wrapFeatureValue(AnnotationFeature aFeature, CAS aCAS, Object aValue)
    {
        if (aValue == null || aValue instanceof LookupEntry) {
            return (LookupEntry) aValue;
        }

        if (aValue instanceof String) {
            String identifier = (String) aValue;
            String label = renderFeatureValue(aFeature, identifier);
            LookupFeatureTraits traits = readTraits(aFeature);
            String description = labelCache.get(aFeature, traits, identifier).getDescription();

            return new LookupEntry(identifier, label, description);
        }

        throw new IllegalArgumentException(
                "Unable to handle value [" + aValue + "] of type [" + aValue.getClass() + "]");
    }

    @Override
    public Panel createTraitsEditor(String aId, IModel<AnnotationFeature> aFeatureModel)
    {
        return new LookupFeatureTraitsEditor(aId, this, aFeatureModel);
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
            if (feature.getType().startsWith(PREFIX)) {
                editor = new LookupFeatureEditor(aId, aOwner, aFeatureStateModel, aStateModel,
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
    public LookupFeatureTraits readTraits(AnnotationFeature aFeature)
    {
        LookupFeatureTraits traits = null;
        try {
            traits = JSONUtil.fromJsonString(LookupFeatureTraits.class, aFeature.getTraits());
        }
        catch (IOException e) {
            LOG.error("Unable to read traits", e);
        }

        if (traits == null) {
            traits = new LookupFeatureTraits();
            traits.setLimit(properties.getDefaultMaxResults());
        }

        return traits;
    }

    @Override
    public void writeTraits(AnnotationFeature aFeature, LookupFeatureTraits aTraits)
    {
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
    public List<VLazyDetailResult> renderLazyDetails(CAS aCas, AnnotationFeature aFeature,
            VID aParamId, String aQuery)
    {
        List<VLazyDetailResult> result = new ArrayList<>();

        LookupFeatureTraits traits = readTraits(aFeature);
        LookupEntry handle = labelCache.get(aFeature, traits, aQuery);

        result.add(new VLazyDetailResult("Label", handle.getUiLabel()));

        if (isNotBlank(handle.getDescription())) {
            result.add(new VLazyDetailResult("Description", handle.getDescription()));
        }

        return result;
    }
}
