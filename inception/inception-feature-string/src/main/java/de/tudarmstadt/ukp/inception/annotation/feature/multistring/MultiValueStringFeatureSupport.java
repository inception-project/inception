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
package de.tudarmstadt.ukp.inception.annotation.feature.multistring;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.inception.annotation.type.StringSuggestionUtil.appendStringSuggestions;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.abbreviate;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.uima.cas.CAS.TYPE_NAME_FS_ARRAY;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING_ARRAY;

import java.io.Serializable;
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
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.feature.misc.UimaPrimitiveFeatureSupport_ImplBase;
import de.tudarmstadt.ukp.inception.annotation.feature.string.StringFeatureSupportProperties;
import de.tudarmstadt.ukp.inception.annotation.feature.string.StringFeatureSupportPropertiesImpl;
import de.tudarmstadt.ukp.inception.annotation.type.StringSuggestion;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.SuggestionState;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VLazyDetail;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VLazyDetailGroup;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.adapter.IllegalFeatureValueException;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureEditor;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureType;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@code AnnotationSchemaServiceAutoConfiguration#multiValueStringFeatureSupport}.
 * </p>
 */
public class MultiValueStringFeatureSupport
    extends UimaPrimitiveFeatureSupport_ImplBase<MultiValueStringFeatureTraits>
{
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private List<FeatureType> primitiveTypes;

    private final StringFeatureSupportProperties properties;

    private final AnnotationSchemaService schemaService;

    /*
     * Constructor for use in unit tests to avoid having to always instantiate the properties.
     */
    public MultiValueStringFeatureSupport()
    {
        this(new StringFeatureSupportPropertiesImpl(), null);
    }

    public MultiValueStringFeatureSupport(StringFeatureSupportProperties aProperties,
            AnnotationSchemaService aSchemaService)
    {
        properties = aProperties;
        schemaService = aSchemaService;
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        primitiveTypes = asList(new FeatureType(TYPE_NAME_STRING_ARRAY,
                "Primitive: String (multi-valued)", getId()));
    }

    @Override
    public List<FeatureType> getSupportedFeatureTypes(AnnotationLayer aAnnotationLayer)
    {
        return unmodifiableList(primitiveTypes);
    }

    @Override
    public boolean accepts(AnnotationFeature aFeature)
    {
        return MultiValueMode.ARRAY.equals(aFeature.getMultiValueMode())
                && CAS.TYPE_NAME_STRING_ARRAY.equals(aFeature.getType());
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

        if (aValue instanceof List) {
            return (List<String>) aValue;
        }

        throw new IllegalArgumentException(
                "Unable to handle value [" + aValue + "] of type [" + aValue.getClass() + "]");
    }

    @Override
    public Serializable wrapFeatureValue(AnnotationFeature aFeature, CAS aCAS, Object aValue)
    {
        if (aValue == null) {
            return null;
        }

        if (aValue instanceof StringArray value) {
            return new ArrayList<>(asList(value.toArray()));
        }

        if (aValue instanceof String value) {
            return new ArrayList<>(asList(value));
        }

        if (aValue instanceof Collection) {
            return (Serializable) aValue;
        }

        throw new IllegalArgumentException(
                "Unable to handle value [" + aValue + "] of type [" + aValue.getClass() + "]");
    }

    @Override
    public void setFeatureValue(CAS aCas, AnnotationFeature aFeature, int aAddress, Object aValue)
        throws IllegalFeatureValueException
    {
        if (!accepts(aFeature)) {
            throw unsupportedFeatureTypeException(aFeature);
        }

        var fs = getFS(aCas, aFeature, aAddress);
        var values = unwrapFeatureValue(aFeature, aValue);
        if (values == null || values.isEmpty()) {
            FSUtil.setFeature(fs, aFeature.getName(), (Collection<String>) null);
            return;
        }

        for (String value : values) {
            schemaService.createMissingTag(aFeature, value);
        }

        // Create a new array if size differs otherwise re-use existing one
        var array = FSUtil.getFeature(fs, aFeature.getName(), StringArrayFS.class);
        if (array == null || (array.size() != values.size())) {
            array = fs.getCAS().createStringArrayFS(values.size());
        }

        // Fill in links
        array.copyFromArray(values.toArray(new String[values.size()]), 0, 0, values.size());

        fs.setFeatureValue(fs.getType().getFeatureByBaseName(aFeature.getName()), array);
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

        for (String value : newValues) {
            schemaService.createMissingTag(aFeature, value);
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
    public Panel createTraitsEditor(String aId, IModel<AnnotationFeature> aFeatureModel)
    {
        return new MultiValueStringFeatureTraitsEditor(aId, this, aFeatureModel);
    }

    @Override
    public void configureFeature(AnnotationFeature aFeature)
    {
        aFeature.setMode(MultiValueMode.ARRAY);
    }

    @Override
    public FeatureEditor createEditor(String aId, MarkupContainer aOwner,
            AnnotationActionHandler aHandler, final IModel<AnnotatorState> aStateModel,
            final IModel<FeatureState> aFeatureStateModel)
    {
        AnnotationFeature feature = aFeatureStateModel.getObject().feature;

        if (!accepts(feature)) {
            throw unsupportedFeatureTypeException(feature);
        }

        return new MultiSelectTextFeatureEditor(aId, aOwner, aFeatureStateModel, aHandler);
    }

    @Override
    public MultiValueStringFeatureTraits createDefaultTraits()
    {
        var traits = new MultiValueStringFeatureTraits();
        traits.setRolesSeeingSuggestionInfo(asList(CURATOR));
        return traits;
    }

    @Override
    public List<String> renderFeatureValues(AnnotationFeature aFeature, FeatureStructure aFs)
    {
        var labelFeature = aFs.getType().getFeatureByBaseName(aFeature.getName());

        if (labelFeature == null) {
            return emptyList();
        }

        List<String> value = getFeatureValue(aFeature, aFs);
        if (value == null) {
            return emptyList();
        }

        return value;
    }

    @Override
    public List<VLazyDetailGroup> lookupLazyDetails(AnnotationFeature aFeature, Object aValue)
    {
        var results = new VLazyDetailGroup();
        if (aValue instanceof Iterable) {
            var values = (Iterable<?>) aValue;
            for (var v : values) {
                if (v instanceof String value) {
                    var tag = schemaService.getTag(value, aFeature.getTagset());

                    if (isNotBlank(value) && aFeature.getTagset() != null && tag.isEmpty()) {
                        results.addDetail(new VLazyDetail(value, "Tag not in tagset"));
                    }

                    if (tag.map(t -> isNotBlank(t.getDescription())).orElse(false)) {
                        results.addDetail(new VLazyDetail(value,
                                abbreviate(tag.get().getDescription(), "…", 128)));
                    }
                }
            }
        }
        return asList(results);
    }

    @Override
    public <V> V getFeatureValue(AnnotationFeature aFeature, FeatureStructure aFS)
    {
        Object value;

        var f = aFS.getType().getFeatureByBaseName(aFeature.getName());

        if (f == null) {
            value = null;
        }
        else if (f.getRange().isPrimitive()) {
            value = FSUtil.getFeature(aFS, aFeature.getName(), Object.class);
        }
        else if (FSUtil.isMultiValuedFeature(aFS, f)) {
            value = FSUtil.getFeature(aFS, aFeature.getName(), List.class);
        }
        else {
            value = FSUtil.getFeature(aFS, aFeature.getName(), FeatureStructure.class);
        }

        return (V) wrapFeatureValue(aFeature, aFS.getCAS(), value);
    }

    @Override
    public void generateFeature(TypeSystemDescription aTSD, TypeDescription aTD,
            AnnotationFeature aFeature)
    {
        super.generateFeature(aTSD, aTD, aFeature);

        var traits = readTraits(aFeature);
        if (traits.isRetainSuggestionInfo()) {
            aTD.addFeature(aFeature.getName() + SUFFIX_SUGGESTION_INFO, "", TYPE_NAME_FS_ARRAY,
                    StringSuggestion._TypeName, false);
        }
    }
}
