/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.ui.kb.project;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import com.googlecode.wicket.kendo.ui.form.combobox.ComboBox;

import de.agilecoders.wicket.core.markup.html.bootstrap.button.Buttons;
import de.agilecoders.wicket.core.markup.html.bootstrap.form.radio.BootstrapRadioGroup;
import de.agilecoders.wicket.core.markup.html.bootstrap.form.radio.BootstrapRadioGroup.ISelectionChangeHandler;
import de.agilecoders.wicket.core.markup.html.bootstrap.form.radio.EnumRadioChoiceRenderer;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModelAdapter;
import de.tudarmstadt.ukp.inception.kb.IriConstants;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.ui.kb.project.wizard.SchemaProfile;

public class KnowledgeBaseIriPanel
    extends Panel
{

    private static final long serialVersionUID = -7189344732710228206L;
    private final IModel<SchemaProfile> selectedSchemaProfile;
    private final CompoundPropertyModel<KnowledgeBaseWrapper> kbModel;

    public KnowledgeBaseIriPanel(String id, CompoundPropertyModel<KnowledgeBaseWrapper> aModel)
    {
        super(id);

        selectedSchemaProfile = Model.of(SchemaProfile.RDFSCHEMA);

        kbModel = aModel;
        
        // Add textfield and label for basePrefix
        ComboBox<String> basePrefix = new ComboBox<String>("basePrefix",
                kbModel.bind("kb.basePrefix"), Arrays.asList(IriConstants.INCEPTION_NAMESPACE));
        basePrefix.add(new LambdaAjaxFormComponentUpdatingBehavior("change", t -> {
            // Do nothing just update the model values
        }));
        basePrefix.setConvertEmptyInputStringToNull(false);
        basePrefix.setOutputMarkupId(true);
        add(basePrefix);

        // RadioGroup to select the IriSchemaType
        BootstrapRadioGroup<SchemaProfile> iriSchemaChoice = new BootstrapRadioGroup<SchemaProfile>(
                "iriSchema", selectedSchemaProfile, Arrays.asList(SchemaProfile.values()),
                new EnumRadioChoiceRenderer<>(Buttons.Type.Default, this))
        {
            private static final long serialVersionUID = 3863260896285332033L;

            @Override
            protected void onInitialize()
            {
                super.onInitialize();
                // Initialize according to current model values
                SchemaProfile modelProfile = checkSchemaProfile(kbModel.getObject().getKb());
                setModelObject(modelProfile);

            }

        };
        iriSchemaChoice.setOutputMarkupId(true);

        // The Kendo comboboxes do not redraw properly when added directly to an
        // AjaxRequestTarget (for each combobox, a text field and a dropdown will be shown).
        // Instead, wrap all of them in a WMC and redraw that.
        WebMarkupContainer comboBoxWrapper = new WebMarkupContainer("comboBoxWrapper");
        comboBoxWrapper.setOutputMarkupId(true);
        add(comboBoxWrapper);

        // Add comboboxes for classIri, subclassIri, typeIri and descriptionIri
        ComboBox<String> classField = buildComboBox("classIri", kbModel.bind("kb.classIri"),
                IriConstants.CLASS_IRIS);
        ComboBox<String> subclassField = buildComboBox("subclassIri",
                kbModel.bind("kb.subclassIri"), IriConstants.SUBCLASS_IRIS);
        ComboBox<String> typeField = buildComboBox("typeIri", kbModel.bind("kb.typeIri"),
                IriConstants.TYPE_IRIS);
        ComboBox<String> descriptionField = buildComboBox("descriptionIri",
                kbModel.bind("kb.descriptionIri"), IriConstants.DESCRIPTION_IRIS);
        ComboBox<String> labelField = buildComboBox("labelIri",
                kbModel.bind("kb.labelIri"), IriConstants.LABEL_IRIS);
        ComboBox<String> propertyTypeField = buildComboBox("propertyTypeIri",
                kbModel.bind("kb.propertyTypeIri"), IriConstants.PROPERTY_TYPE_IRIS);
        comboBoxWrapper.add(classField, subclassField, typeField, descriptionField, labelField,
                propertyTypeField);

        // OnChange update the model with corresponding iris
        iriSchemaChoice.setChangeHandler(new ISelectionChangeHandler<SchemaProfile>()
        {
            private static final long serialVersionUID = 1653808650286121732L;

            @Override
            public void onSelectionChanged(AjaxRequestTarget target, SchemaProfile bean)
            {
                classField.setModelObject(bean.getClassIri().stringValue());
                subclassField.setModelObject(bean.getSubclassIri().stringValue());
                typeField.setModelObject(bean.getTypeIri().stringValue());
                descriptionField.setModelObject(bean.getDescriptionIri().stringValue());
                labelField.setModelObject(bean.getLabelIri().stringValue());
                propertyTypeField.setModelObject(bean.getPropertyTypeIri().stringValue());

                target.add(comboBoxWrapper, iriSchemaChoice);
            }
        });

        add(iriSchemaChoice);

    }

    private ComboBox<String> buildComboBox(String id, IModel<IRI> model, List<IRI> iris)
    {
        // Only set model object if it has not been initialized yet
        if (model.getObject() == null) {
            model.setObject(iris.get(0));
        }
 
        List<String> choices = iris.stream().map(IRI::stringValue).collect(Collectors.toList());

        IModel<String> adapter = new LambdaModelAdapter<String>(
            () -> model.getObject().stringValue(),
            str -> model.setObject(SimpleValueFactory.getInstance().createIRI(str)));

        ComboBox<String> comboBox = new ComboBox<String>(id, adapter, choices);
        comboBox.add(LambdaBehavior.onConfigure(cb -> cb
                .setEnabled(SchemaProfile.CUSTOMSCHEMA.equals(selectedSchemaProfile.getObject()))));
        comboBox.setOutputMarkupId(true);
        comboBox.setRequired(true);
        comboBox.add(Validators.IRI_VALIDATOR);
        comboBox.add(new LambdaAjaxFormComponentUpdatingBehavior("change", t -> {
            // Do nothing just update the model values
        }));
        return comboBox;
    }

    private SchemaProfile checkSchemaProfile(KnowledgeBase kb)
    {
        SchemaProfile[] profiles = SchemaProfile.values();
        for (int i = 0; i < profiles.length; i++) {
            // Check if kb has a known schema profile
            if (equalsSchemaProfile(profiles[i], kb.getClassIri(), kb.getSubclassIri(),
                    kb.getTypeIri(), kb.getDescriptionIri(), kb.getLabelIri(),
                    kb.getPropertyTypeIri())) {
                return profiles[i];
            }
        }
        // If the iris don't represent a known schema profile , return CUSTOM
        return SchemaProfile.CUSTOMSCHEMA;
    }

    /**
     * Compares a schema profile to given IRIs. Returns true if the IRIs are the same as in the
     * profile
     */
    private boolean equalsSchemaProfile(SchemaProfile profile, IRI classIri, IRI subclassIri,
            IRI typeIri, IRI descriptionIri, IRI labelIri, IRI propertyTypeIri)
    {
        return profile.getClassIri().equals(classIri)
                && profile.getSubclassIri().equals(subclassIri)
                && profile.getTypeIri().equals(typeIri)
                && profile.getDescriptionIri().equals(descriptionIri)
                && profile.getLabelIri().equals(labelIri)
                && profile.getPropertyTypeIri().equals(propertyTypeIri);
    }
    
    /**
     * Label and TextField for basePrefix only show up in CUSTOM mode or if the user has changed
     * the default value
     */
    private boolean isBasePrefixVisible()
    {
        return SchemaProfile.CUSTOMSCHEMA.equals(selectedSchemaProfile.getObject()) || !kbModel
                .getObject().getKb().getBasePrefix().equals(IriConstants.INCEPTION_NAMESPACE);
    }

}
