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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.StringFeatureTraits.EditorType.AUTOCOMPLETE;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.StringFeatureTraits.EditorType.COMBOBOX;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.StringFeatureTraits.EditorType.RADIOGROUP;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.config.PrimitiveUimaFeatureSupportProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.DynamicTextAreaFeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.FeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.InputFieldTextFeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.KendoAutoCompleteTextFeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.KendoComboboxTextFeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.RadioGroupStringFeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.StringFeatureTraits;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.StringFeatureTraits.EditorType;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.StringFeatureTraitsEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.TextAreaFeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;

@Component
public class StringFeatureSupport
    extends UimaPrimitiveFeatureSupport_ImplBase<StringFeatureTraits>
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private List<FeatureType> primitiveTypes;

    private final PrimitiveUimaFeatureSupportProperties properties;

    private final AnnotationSchemaService schemaService;

    /*
     * Constructor for use in unit tests to avoid having to always instantiate the properties.
     */
    public StringFeatureSupport()
    {
        this(new PrimitiveUimaFeatureSupportProperties(), null);
    }

    @Autowired
    public StringFeatureSupport(PrimitiveUimaFeatureSupportProperties aProperties,
            @Autowired(required = false) AnnotationSchemaService aSchemaService)
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
        return Collections.unmodifiableList(primitiveTypes);
    }

    @Override
    public boolean accepts(AnnotationFeature aFeature)
    {
        return MultiValueMode.NONE.equals(aFeature.getMultiValueMode())
                && CAS.TYPE_NAME_STRING.equals(aFeature.getType());
    }

    @Override
    public void setFeatureValue(CAS aCas, AnnotationFeature aFeature, int aAddress, Object aValue)
    {
        if (aValue != null && schemaService != null && aFeature.getTagset() != null
                && CAS.TYPE_NAME_STRING.equals(aFeature.getType())
                && !schemaService.existsTag((String) aValue, aFeature.getTagset())) {
            if (!aFeature.getTagset().isCreateTag()) {
                throw new IllegalArgumentException("[" + aValue
                        + "] is not in the tag list. Please choose from the existing tags");
            }
            else {
                Tag selectedTag = new Tag();
                selectedTag.setName((String) aValue);
                selectedTag.setTagSet(aFeature.getTagset());
                schemaService.createTag(selectedTag);
            }
        }

        super.setFeatureValue(aCas, aFeature, aAddress, aValue);
    }

    @Override
    public Panel createTraitsEditor(String aId, IModel<AnnotationFeature> aFeatureModel)
    {
        AnnotationFeature feature = aFeatureModel.getObject();

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
        AnnotationFeature feature = aFeatureStateModel.getObject().feature;

        if (!accepts(feature)) {
            throw unsupportedFeatureTypeException(feature);
        }

        StringFeatureTraits traits = readTraits(feature);

        if (feature.getTagset() == null) {
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
                return new InputFieldTextFeatureEditor(aId, aOwner, aFeatureStateModel, aHandler);
            }
        }

        EditorType editorType = traits.getEditorType();
        if (editorType == EditorType.AUTO) {
            editorType = autoChooseFeatureEditor(aFeatureStateModel);
        }

        switch (editorType) {
        case RADIOGROUP:
            return new RadioGroupStringFeatureEditor(aId, aOwner, aFeatureStateModel, aHandler);
        case COMBOBOX:
            return new KendoComboboxTextFeatureEditor(aId, aOwner, aFeatureStateModel, aHandler);
        case AUTOCOMPLETE:
            return new KendoAutoCompleteTextFeatureEditor(aId, aOwner, aFeatureStateModel,
                    properties.getAutoCompleteMaxResults(), aHandler);
        default:
            throw new IllegalStateException(
                    "Unknown editor type: [" + traits.getEditorType() + "]");
        }
    }

    private EditorType autoChooseFeatureEditor(final IModel<FeatureState> aFeatureStateModel)
    {
        // For really small tagsets, use a radio group
        if (aFeatureStateModel.getObject().tagset.size() <= 3) {
            return RADIOGROUP;
        }

        // For mid-sized tagsets, use a combobox
        if (aFeatureStateModel.getObject().tagset.size() < properties.getAutoCompleteThreshold()) {
            return COMBOBOX;
        }

        // For larger ones, use an auto-complete field
        return AUTOCOMPLETE;
    }

    @Override
    public StringFeatureTraits readTraits(AnnotationFeature aFeature)
    {
        StringFeatureTraits traits = null;
        try {
            traits = JSONUtil.fromJsonString(StringFeatureTraits.class, aFeature.getTraits());
        }
        catch (IOException e) {
            log.error("Unable to read traits", e);
        }

        if (traits == null) {
            traits = new StringFeatureTraits();
        }

        return traits;
    }

    @Override
    public void writeTraits(AnnotationFeature aFeature, StringFeatureTraits aTraits)
    {
        try {
            aFeature.setTraits(JSONUtil.toJsonString(aTraits));
        }
        catch (IOException e) {
            log.error("Unable to write traits", e);
        }
    }

    @Override
    public boolean suppressAutoFocus(AnnotationFeature aFeature)
    {
        StringFeatureTraits traits = readTraits(aFeature);
        return !traits.getKeyBindings().isEmpty();
    }
}
