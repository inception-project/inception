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
package de.tudarmstadt.ukp.inception.annotation.feature.string;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.inception.annotation.feature.string.StringFeatureTraits.EditorType.AUTOCOMPLETE;
import static de.tudarmstadt.ukp.inception.annotation.feature.string.StringFeatureTraits.EditorType.COMBOBOX;
import static de.tudarmstadt.ukp.inception.annotation.feature.string.StringFeatureTraits.EditorType.RADIOGROUP;
import static de.tudarmstadt.ukp.inception.annotation.type.StringSuggestionUtil.setStringSuggestions;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static org.apache.commons.lang3.StringUtils.abbreviate;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.uima.cas.CAS.TYPE_NAME_FS_ARRAY;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.AnnotationBaseFS;
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
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.feature.misc.UimaPrimitiveFeatureSupport_ImplBase;
import de.tudarmstadt.ukp.inception.annotation.feature.string.StringFeatureTraits.EditorType;
import de.tudarmstadt.ukp.inception.annotation.type.StringSuggestion;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.SuggestionState;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VLazyDetail;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VLazyDetailGroup;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureEditor;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureType;
import de.tudarmstadt.ukp.inception.support.uima.ICasUtil;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@code AnnotationSchemaServiceAutoConfiguration#stringFeatureSupport}.
 * </p>
 */
public class StringFeatureSupport
    extends UimaPrimitiveFeatureSupport_ImplBase<StringFeatureTraits>
{
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private List<FeatureType> primitiveTypes;

    private final StringFeatureSupportProperties properties;

    private final AnnotationSchemaService schemaService;

    /*
     * Constructor for use in unit tests to avoid having to always instantiate the properties.
     */
    public StringFeatureSupport()
    {
        this(new StringFeatureSupportPropertiesImpl(), null);
    }

    public StringFeatureSupport(StringFeatureSupportProperties aProperties,
            AnnotationSchemaService aSchemaService)
    {
        properties = aProperties;
        schemaService = aSchemaService;
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        primitiveTypes = asList(
                new FeatureType(CAS.TYPE_NAME_STRING, "Primitive: String", getId()));
    }

    @Override
    public List<FeatureType> getSupportedFeatureTypes(AnnotationLayer aAnnotationLayer)
    {
        return unmodifiableList(primitiveTypes);
    }

    @Override
    public boolean accepts(AnnotationFeature aFeature)
    {
        return MultiValueMode.NONE.equals(aFeature.getMultiValueMode())
                && CAS.TYPE_NAME_STRING.equals(aFeature.getType());
    }

    @Override
    public void setFeatureValue(CAS aCas, AnnotationFeature aFeature, int aAddress, Object aValue)
        throws AnnotationException
    {
        if (schemaService != null) {
            schemaService.createMissingTag(aFeature, (String) aValue);
        }

        super.setFeatureValue(aCas, aFeature, aAddress, aValue);
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
        var feature = aFeatureModel.getObject();

        if (!accepts(feature)) {
            throw unsupportedFeatureTypeException(feature);
        }

        return new StringFeatureTraitsEditor(aId, this, aFeatureModel);
    }

    @Override
    public FeatureEditor createEditor(String aId, MarkupContainer aOwner,
            AnnotationActionHandler aHandler, final IModel<AnnotatorState> aStateModel,
            final IModel<FeatureState> aFeatureStateModel)
    {
        var feature = aFeatureStateModel.getObject().feature;

        if (!accepts(feature)) {
            throw unsupportedFeatureTypeException(feature);
        }

        var traits = readTraits(feature);

        if (feature.getTagset() == null || traits.isMultipleRows()) {
            if (traits.isMultipleRows()) {
                // If multiple rows are set use a textarea
                if (traits.isDynamicSize()) {
                    return new DynamicTextAreaFeatureEditor(aId, aOwner, aFeatureStateModel);
                }
                else {
                    return new TextAreaFeatureEditor(aId, aOwner, aFeatureStateModel);
                }
            }
            else {
                // Otherwise use a simple input field
                return new InputFieldStringFeatureEditor(aId, aOwner, aFeatureStateModel, aHandler);
            }
        }

        var editorType = traits.getEditorType();
        if (editorType == EditorType.AUTO) {
            editorType = autoChooseFeatureEditorWithTagset(aFeatureStateModel);
        }

        switch (editorType) {
        case RADIOGROUP:
            return new RadioGroupStringFeatureEditor(aId, aOwner, aFeatureStateModel, aHandler);
        case COMBOBOX:
            return new KendoComboboxTextFeatureEditor(aId, aOwner, aFeatureStateModel, aHandler);
        case AUTOCOMPLETE:
            return new KendoAutoCompleteTextFeatureEditor(aId, aOwner, aFeatureStateModel,
                    aHandler);
        default:
            throw new IllegalStateException(
                    "Unknown editor type: [" + traits.getEditorType() + "]");
        }
    }

    private EditorType autoChooseFeatureEditorWithTagset(
            final IModel<FeatureState> aFeatureStateModel)
    {
        var featureState = aFeatureStateModel.getObject();

        // For really small tagsets where tag creation is not supported, use a radio group
        if (!featureState.feature.getTagset().isCreateTag()
                && featureState.tagset.size() < properties.getComboBoxThreshold()) {
            return RADIOGROUP;
        }

        // For mid-sized tagsets, use a combobox
        if (featureState.tagset.size() < properties.getAutoCompleteThreshold()) {
            return COMBOBOX;
        }

        // For larger ones, use an auto-complete field
        return AUTOCOMPLETE;
    }

    @Override
    public StringFeatureTraits createDefaultTraits()
    {
        var traits = new StringFeatureTraits();
        traits.setRolesSeeingSuggestionInfo(asList(CURATOR));
        return traits;
    }

    @Override
    public boolean suppressAutoFocus(AnnotationFeature aFeature)
    {
        var traits = readTraits(aFeature);
        return !traits.getKeyBindings().isEmpty();
    }

    @Override
    public List<VLazyDetailGroup> lookupLazyDetails(AnnotationFeature aFeature, Object aValue)
    {
        if (aValue instanceof String value) {
            var tag = schemaService.getTag(value, aFeature.getTagset());

            if (isNotBlank(value) && aFeature.getTagset() != null && tag.isEmpty()) {
                return asList(new VLazyDetailGroup(new VLazyDetail(value, "Tag not in tagset")));
            }

            if (tag.map(t -> isNotBlank(t.getDescription())).orElse(false)) {
                return asList(new VLazyDetailGroup(
                        new VLazyDetail(value, abbreviate(tag.get().getDescription(), "…", 128))));
            }
        }

        return emptyList();
    }

    @Override
    public <V> V getNullFeatureValue(AnnotationFeature aFeature, FeatureStructure aFS)
    {
        return null;
    }

    @Override
    public void initializeAnnotation(AnnotationFeature aFeature, FeatureStructure aFS)
        throws AnnotationException
    {
        var traits = readTraits(aFeature);
        if (isNotBlank(traits.getDefaultValue())) {
            setFeatureValue(aFS.getCAS(), aFeature, ICasUtil.getAddr(aFS),
                    traits.getDefaultValue());
        }
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

    @Override
    public void pushSuggestions(SourceDocument aDocument, String aDataOwner,
            AnnotationBaseFS aAnnotation, AnnotationFeature aFeature,
            List<SuggestionState> aSuggestions)
    {
        setStringSuggestions(aAnnotation, aFeature.getName() + SUFFIX_SUGGESTION_INFO,
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
}
