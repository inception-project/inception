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
package de.tudarmstadt.ukp.inception.ui.kb.feature;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.inception.annotation.type.StringSuggestionUtil.appendStringSuggestions;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.uima.cas.CAS.TYPE_NAME_FS_ARRAY;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.uima.cas.AnnotationBaseFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.StringArrayFS;
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
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.type.StringSuggestion;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.kb.MultiValueConceptFeatureTraits;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.SuggestionState;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VLazyDetail;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VLazyDetailGroup;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.adapter.IllegalFeatureValueException;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureEditor;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupport;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureType;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;
import de.tudarmstadt.ukp.inception.ui.kb.config.KnowledgeBaseServiceUIAutoConfiguration;

/**
 * Extension providing knowledge-base-related features for annotations.
 * <p>
 * This class is exposed as a Spring Component via
 * {@link KnowledgeBaseServiceUIAutoConfiguration#multiValueConceptFeatureSupport}.
 * </p>
 */
public class MultiValueConceptFeatureSupport
    implements FeatureSupport<MultiValueConceptFeatureTraits>
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String PREFIX = "kb-multi:";

    public static final String ANY_OBJECT = "<ANY>";
    public static final String TYPE_ANY_OBJECT = PREFIX + ANY_OBJECT;

    private final ConceptLabelCache labelCache;

    private String featureSupportId;

    @Autowired
    public MultiValueConceptFeatureSupport(ConceptLabelCache aLabelCache)
    {
        labelCache = aLabelCache;
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
    public boolean accepts(AnnotationFeature aFeature)
    {
        switch (aFeature.getMultiValueMode()) {
        case NONE:
            return false;
        case ARRAY:
            return aFeature.getType().startsWith(PREFIX);
        default:
            return false;
        }
    }

    @Override
    public boolean isFeatureValueValid(AnnotationFeature aFeature, FeatureStructure aFS)
    {
        if (aFeature.isRequired()) {
            var value = FSUtil.getFeature(aFS, aFeature.getName(), List.class);
            return isNotEmpty(value);
        }

        return true;
    }

    @Override
    public List<FeatureType> getSupportedFeatureTypes(AnnotationLayer aAnnotationLayer)
    {
        // We just start with no specific scope at all (ANY) and let the user refine this via
        // the traits editor
        return asList(new FeatureType(TYPE_ANY_OBJECT,
                "KB: Concept/Instance/Property (multi-valued)", featureSupportId));
    }

    @Override
    public void pushFeatureValue(CAS aCas, AnnotationFeature aFeature, int aAddress, Object aValue)
        throws AnnotationException
    {
        if (!accepts(aFeature)) {
            throw unsupportedFeatureTypeException(aFeature);
        }

        var fs = getFS(aCas, aFeature, aAddress);
        var newValues = unwrapFeatureValue(aFeature, aValue);
        if (newValues == null || newValues.isEmpty()) {
            return;
        }

        var feature = fs.getType().getFeatureByBaseName(aFeature.getName());
        var oldValues = FSUtil.getFeature(fs, aFeature.getName(), StringArrayFS.class);

        var mergedValues = new LinkedHashSet<String>();
        if (oldValues != null) {
            for (var i = 0; i < oldValues.size(); i++) {
                mergedValues.add(oldValues.get(i));
            }
        }

        mergedValues.addAll(newValues);

        // Create a new array if size differs otherwise re-use existing one
        var array = FSUtil.getFeature(fs, aFeature.getName(), StringArrayFS.class);
        if (array == null || (array.size() != mergedValues.size())) {
            array = fs.getCAS().createStringArrayFS(mergedValues.size());
        }

        array.copyFromArray(mergedValues.toArray(new String[mergedValues.size()]), 0, 0,
                mergedValues.size());

        fs.setFeatureValue(feature, array);
    }

    @Override
    public void pushSuggestions(SourceDocument aDocument, String aDataOwner,
            AnnotationBaseFS aAnnotation, AnnotationFeature aFeature,
            List<SuggestionState> aSuggestions)
    {
        appendStringSuggestions(aAnnotation, aFeature.getName() + SUFFIX_SUGGESTION_INFO,
                aSuggestions);
    }

    @Override
    public void generateFeature(TypeSystemDescription aTSD, TypeDescription aTD,
            AnnotationFeature aFeature)
    {
        aTD.addFeature(aFeature.getName(), aFeature.getDescription(), CAS.TYPE_NAME_STRING_ARRAY);

        var traits = readTraits(aFeature);
        if (traits.isRetainSuggestionInfo()) {
            aTD.addFeature(aFeature.getName() + SUFFIX_SUGGESTION_INFO, "", TYPE_NAME_FS_ARRAY,
                    StringSuggestion._TypeName, false);
        }
    }

    @Override
    public void configureFeature(AnnotationFeature aFeature)
    {
        aFeature.setMode(MultiValueMode.ARRAY);
    }

    @Override
    public Panel createTraitsEditor(String aId, IModel<AnnotationFeature> aFeatureModel)
    {
        return new MultiValueConceptFeatureTraitsEditor(aId, this, aFeatureModel);
    }

    @Override
    public MultiValueConceptFeatureTraits createDefaultTraits()
    {
        var traits = new MultiValueConceptFeatureTraits();
        traits.setRolesSeeingSuggestionInfo(asList(CURATOR));
        return traits;
    }

    @Override
    public MultiValueConceptFeatureTraits readTraits(AnnotationFeature aFeature)
    {
        var traits = FeatureSupport.super.readTraits(aFeature);

        // If there is no scope set in the trait, see if once can be extracted from the legacy
        // location which is the feature type.
        if (traits.getScope() == null && !TYPE_ANY_OBJECT.equals(aFeature.getType())) {
            traits.setScope(aFeature.getType().substring(PREFIX.length()));
        }

        return traits;
    }

    @Override
    public void writeTraits(AnnotationFeature aFeature, MultiValueConceptFeatureTraits aTraits)
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
    public FeatureEditor createEditor(String aId, MarkupContainer aOwner,
            AnnotationActionHandler aHandler, IModel<AnnotatorState> aStateModel,
            IModel<FeatureState> aFeatureStateModel)
    {
        AnnotationFeature feature = aFeatureStateModel.getObject().feature;
        FeatureEditor editor;

        switch (feature.getMultiValueMode()) {
        case NONE:
            throw unsupportedMultiValueModeException(feature);
        case ARRAY:
            if (feature.getType().startsWith(PREFIX)) {
                editor = new MultiValueConceptFeatureEditor(aId, aOwner, aFeatureStateModel,
                        aStateModel, aHandler);
            }
            else {
                throw unsupportedMultiValueModeException(feature);
            }
            break;
        default:
            throw unsupportedMultiValueModeException(feature);
        }

        return editor;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V> V getNullFeatureValue(AnnotationFeature aFeature, FeatureStructure aFS)
    {
        return (V) emptyList();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> unwrapFeatureValue(AnnotationFeature aFeature, Object aValue)
    {
        if (aValue == null) {
            return null;
        }

        if (aValue instanceof String) {
            return asList((String) aValue);
        }

        // Normally, we get KBHandles back from the feature editors
        if (aValue instanceof List) {
            return ((List<KBHandle>) aValue).stream() //
                    .map(item -> item.getIdentifier()) //
                    .collect(toList());
        }

        throw new IllegalArgumentException(
                "Unable to handle value [" + aValue + "] of type [" + aValue.getClass() + "]");
    }

    @Override
    public ArrayList<KBHandle> wrapFeatureValue(AnnotationFeature aFeature, CAS aCAS, Object aValue)
    {
        if (aValue == null) {
            return null;
        }

        var traits = readTraits(aFeature);
        var wrapped = new ArrayList<KBHandle>();

        if (aValue instanceof String identifier) {
            wrapped.add(getConcept(aFeature, traits, identifier));
            return wrapped;
        }

        if (aValue instanceof List) {
            for (var item : (List<?>) aValue) {
                if (item instanceof KBHandle) {
                    wrapped.add((KBHandle) item);
                    continue;
                }

                if (item instanceof String identifier) {
                    wrapped.add(getConcept(aFeature, traits, identifier));
                    continue;
                }

                throw new IllegalArgumentException("Unable to handle list item [" + item
                        + "] of type [" + item.getClass() + "]");
            }

            return wrapped;
        }

        throw new IllegalArgumentException(
                "Unable to handle value [" + aValue + "] of type [" + aValue.getClass() + "]");
    }

    private KBHandle getConcept(AnnotationFeature aFeature, MultiValueConceptFeatureTraits traits,
            String identifier)
    {
        var chbk = labelCache.get(aFeature, traits.getRepositoryId(), identifier);
        var clone = new KBHandle(chbk.getIdentifier(), chbk.getUiLabel(), chbk.getDescription(),
                chbk.getLanguage());
        clone.setKB(chbk.getKB());
        return clone;
    }

    @Override
    public void setFeatureValue(CAS aCas, AnnotationFeature aFeature, int aAddress, Object aValue)
        throws IllegalFeatureValueException
    {
        if (!accepts(aFeature)) {
            throw unsupportedFeatureTypeException(aFeature);
        }

        FeatureStructure fs = getFS(aCas, aFeature, aAddress);
        List<String> values = unwrapFeatureValue(aFeature, aValue);
        if (values == null || values.isEmpty()) {
            FSUtil.setFeature(fs, aFeature.getName(), (Collection<String>) null);
            return;
        }

        // Create a new array if size differs otherwise re-use existing one
        StringArrayFS array = FSUtil.getFeature(fs, aFeature.getName(), StringArrayFS.class);
        if (array == null || (array.size() != values.size())) {
            array = fs.getCAS().createStringArrayFS(values.size());
        }

        // Fill in links
        array.copyFromArray(values.toArray(new String[values.size()]), 0, 0, values.size());

        fs.setFeatureValue(fs.getType().getFeatureByBaseName(aFeature.getName()), array);
    }

    @Override
    public String renderFeatureValue(AnnotationFeature aFeature, String aIdentifier)
    {
        if (aIdentifier == null) {
            return null;
        }

        var traits = readTraits(aFeature);
        return labelCache.get(aFeature, traits.getRepositoryId(), aIdentifier).getUiLabel();
    }

    @Override
    public List<String> renderFeatureValues(AnnotationFeature aFeature, FeatureStructure aFs)
    {
        var labelFeature = aFs.getType().getFeatureByBaseName(aFeature.getName());

        if (labelFeature == null) {
            return emptyList();
        }

        List<KBHandle> handles = getFeatureValue(aFeature, aFs);
        if (handles == null || handles.isEmpty()) {
            return emptyList();
        }

        return handles.stream() //
                .map(KBHandle::getUiLabel) //
                .toList();
    }

    @Override
    public String renderWrappedFeatureValue(Object aValue)
    {
        if (aValue == null) {
            return null;
        }

        if (aValue instanceof List list) {
            var sb = new StringBuilder();
            for (var item : list) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                if (item == null) {
                    sb.append("no label");
                }
                else if (item instanceof KBHandle handle) {
                    sb.append(handle.getUiLabel());
                }
                else {
                    throw new IllegalArgumentException(
                            "Expected List<KBHandle> but encounterd item of type ["
                                    + item.getClass() + "]");

                }
            }
            return sb.toString();
        }

        throw new IllegalArgumentException(
                "Expected List<KBHandle> but was [" + aValue.getClass() + "]");
    }

    @Override
    public List<VLazyDetailGroup> lookupLazyDetails(AnnotationFeature aFeature, Object aValue)
    {
        var result = new VLazyDetailGroup();

        if (aValue instanceof Iterable) {
            var handles = (Iterable<?>) aValue;
            for (var h : handles) {
                if (h instanceof KBHandle) {
                    var handle = (KBHandle) h;
                    result.addDetail(new VLazyDetail("Label", handle.getUiLabel()));

                    if (isNotBlank(handle.getDescription())) {
                        result.addDetail(new VLazyDetail("Description", handle.getDescription()));
                    }
                }
            }
        }

        return asList(result);
    }

    @Override
    public List<SuggestionState> getSuggestions(FeatureStructure aAnnotation,
            AnnotationFeature aFeature)
    {
        var cas = aAnnotation.getCAS();
        var suggestionInfoFeature = aAnnotation.getType()
                .getFeatureByBaseName(aFeature.getName() + SUFFIX_SUGGESTION_INFO);

        if (suggestionInfoFeature == null) {
            // If the feature does not exist, there is no info to return.
            // Checking for the feature is faster than parsing the traits of the feature
            // to check if it has suggestion info enabled.
            return emptyList();
        }

        var suggestions = FSUtil.getFeature(aAnnotation, suggestionInfoFeature,
                StringSuggestion[].class);
        var suggestionStates = new ArrayList<SuggestionState>();
        if (suggestions != null) {
            for (var suggestion : suggestions) {
                var state = new SuggestionState(suggestion.getRecommender().getName(),
                        suggestion.getScore(),
                        wrapFeatureValue(aFeature, cas, suggestion.getLabel()));
                suggestionStates.add(state);
            }
        }
        return suggestionStates;
    }
}
