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

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.feature.lookup.config.LookupServiceAutoConfiguration;
import de.tudarmstadt.ukp.inception.feature.lookup.config.LookupServiceProperties;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VLazyDetail;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VLazyDetailGroup;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureEditor;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupport;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureType;

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

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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
                "Lookup (" + aFeature.getType().substring(PREFIX.length()) + ")",
                featureSupportId));
    }

    @Override
    public List<FeatureType> getSupportedFeatureTypes(AnnotationLayer aAnnotationLayer)
    {
        return asList(
                new FeatureType(TYPE_STRING_LOOKUP, "Lookup (" + STRING + ")", featureSupportId));
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
    public String renderFeatureValue(AnnotationFeature aFeature, String aId)
    {
        if (aId == null) {
            return null;
        }

        LookupFeatureTraits traits = readTraits(aFeature);
        return labelCache.get(aFeature, traits, aId).getUiLabel();
    }

    @Override
    public <V> V getNullFeatureValue(AnnotationFeature aFeature, FeatureStructure aFS)
    {
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public String unwrapFeatureValue(AnnotationFeature aFeature, Object aValue)
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
    public LookupFeatureTraits createDefaultTraits()
    {
        var traits = new LookupFeatureTraits();
        traits.setLimit(properties.getDefaultMaxResults());
        return traits;
    }

    @Override
    public void generateFeature(TypeSystemDescription aTSD, TypeDescription aTD,
            AnnotationFeature aFeature)
    {
        aTD.addFeature(aFeature.getName(), "", CAS.TYPE_NAME_STRING);
    }

    @Override
    public List<VLazyDetailGroup> lookupLazyDetails(AnnotationFeature aFeature, Object aValue)
    {
        if (aValue instanceof LookupEntry) {
            var handle = (LookupEntry) aValue;

            var result = new VLazyDetailGroup();
            result.addDetail(new VLazyDetail("Label", handle.getUiLabel()));

            if (isNotBlank(handle.getDescription())) {
                result.addDetail(new VLazyDetail("Description", handle.getDescription()));
            }

            return asList(result);
        }

        return Collections.emptyList();
    }
}
