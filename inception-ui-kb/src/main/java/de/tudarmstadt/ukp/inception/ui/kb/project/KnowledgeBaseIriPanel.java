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
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.QueryEvaluationException;

import com.googlecode.wicket.kendo.ui.form.combobox.ComboBox;

import de.agilecoders.wicket.core.markup.html.bootstrap.button.Buttons;
import de.agilecoders.wicket.core.markup.html.bootstrap.form.radio.BootstrapRadioGroup;
import de.agilecoders.wicket.core.markup.html.bootstrap.form.radio.BootstrapRadioGroup.ISelectionChangeHandler;
import de.agilecoders.wicket.core.markup.html.bootstrap.form.radio.EnumRadioChoiceRenderer;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModelAdapter;
import de.tudarmstadt.ukp.inception.kb.IriConstants;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.SchemaProfile;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;


public class KnowledgeBaseIriPanel
    extends Panel
{

    private static final long serialVersionUID = -7189344732710228206L;
    private final IModel<SchemaProfile> selectedSchemaProfile;
    private final CompoundPropertyModel<KnowledgeBaseWrapper> kbModel;
    private final AdvancedIriSettingsPanel advancedSettingsPanel;
    private @SpringBean KnowledgeBaseService kbService;

    public KnowledgeBaseIriPanel(String id, CompoundPropertyModel<KnowledgeBaseWrapper> aModel,
            KnowledgeBaseIriPanelMode mode)
    {
        super(id);
        setOutputMarkupId(true);
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
                SchemaProfile modelProfile = kbService
                    .checkSchemaProfile(kbModel.getObject().getKb());
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

        // The FTS IRI is actually more of a mode switch and not part of the schema as it relates
        // to the storage mechanism and not to the knowledge resource schema
        DropDownChoice<IRI> ftsField = new DropDownChoice<>("fullTextSearchIri",
                kbModel.bind("kb.fullTextSearchIri"), IriConstants.FTS_IRIS);
        ftsField.setOutputMarkupId(true);
        ftsField.setNullValid(true);
        add(ftsField);

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
        ComboBox<String> propertyLabelField = buildComboBox("propertyLabelIri",
                kbModel.bind("kb.propertyLabelIri"), IriConstants.PROPERTY_LABEL_IRIS);
        ComboBox<String> propertyDescriptionField = buildComboBox("propertyDescriptionIri",
                kbModel.bind("kb.propertyDescriptionIri"), IriConstants.PROPERTY_DESCRIPTION_IRIS);
        comboBoxWrapper.add(classField, subclassField, typeField, descriptionField, labelField,
                propertyTypeField, propertyLabelField, propertyDescriptionField);
       
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
                propertyLabelField.setModelObject(bean.getPropertyLabelIri().stringValue());
                propertyDescriptionField
                    .setModelObject(bean.getPropertyDescriptionIri().stringValue());
                target.add(comboBoxWrapper, iriSchemaChoice);
            }
        });
        comboBoxWrapper.add(iriSchemaChoice);
           
        // Add advanced settings panel
        advancedSettingsPanel = new AdvancedIriSettingsPanel("advancedSettings", kbModel);
        advancedSettingsPanel.setVisible(false);
        advancedSettingsPanel.setOutputMarkupPlaceholderTag(true);
        add(advancedSettingsPanel);  
        
        // Only make the advanced settings panel visible in the project settings because the kb has
        // not been registered in the wizard at the point where we want to read specified concepts 
        // to check whether they exist (see isConceptValid method).     
        LambdaAjaxLink toggleAdvancedSettings = new LambdaAjaxLink("toggleAdvancedSettings",
                KnowledgeBaseIriPanel.this::actionToggleAdvancedSettings);
        toggleAdvancedSettings.setVisible(false);
        add(toggleAdvancedSettings);
        
        if (KnowledgeBaseIriPanelMode.PROJECTSETTINGS.equals(mode)) {
            toggleAdvancedSettings.setVisible(true);
            Label toggleAdvancedSettingsLabel = new Label("toggleAdvSettingsLabel")
            {
                private static final long serialVersionUID = -1593621355344848909L;
                
                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    
                    IModel<String> labelModel;
                    if (advancedSettingsPanel.isVisible()) {
                        labelModel = new ResourceModel("toogleAdvSettingsHide");
                    }
                    else {
                        labelModel = new ResourceModel("toggleAdvSettingsShow");
                    }
                    setDefaultModel(labelModel);
                }
            };
            toggleAdvancedSettings.setOutputMarkupId(true);
            toggleAdvancedSettings.add(toggleAdvancedSettingsLabel);
        }
    }
    
    private void actionToggleAdvancedSettings(AjaxRequestTarget aTarget) {
        advancedSettingsPanel.setVisible(!advancedSettingsPanel.isVisible());
        aTarget.add(advancedSettingsPanel);
        aTarget.add(get("toggleAdvancedSettings"));
    }

    private ComboBox<String> buildComboBox(String id, IModel<IRI> model, List<IRI> iris)
    {
        // Only set model object if it has not been initialized yet
        if (model.getObject() == null) {
            model.setObject(iris.get(0));
        }
 
        List<String> choices = iris.stream().map(IRI::stringValue).collect(Collectors.toList());

        IModel<String> adapter = new LambdaModelAdapter<String>(
            () -> { return model.getObject() != null ? model.getObject().stringValue() : null; },
            str -> { 
                model.setObject(str != null ? SimpleValueFactory.getInstance().createIRI(str) : 
                    null); });

        ComboBox<String> comboBox = new ComboBox<>(id, adapter, choices);
        comboBox.add(LambdaBehavior.enabledWhen(() -> 
                SchemaProfile.CUSTOMSCHEMA.equals(selectedSchemaProfile.getObject())));
        comboBox.setOutputMarkupId(true);
        comboBox.setRequired(true);
        comboBox.add(Validators.IRI_VALIDATOR);
        // Do nothing just update the model values
        comboBox.add(new LambdaAjaxFormComponentUpdatingBehavior("change"));
        return comboBox;
    }
    
    private TextField<String> buildTextField(String id, IModel<IRI> model) {
        IModel<String> adapter = new LambdaModelAdapter<String>(
            () -> model.getObject().stringValue(),
            str -> model.setObject(SimpleValueFactory.getInstance().createIRI(str)));
        
        TextField<String> iriTextfield = new TextField<>(id, adapter);
        iriTextfield.setOutputMarkupId(true);
        iriTextfield.add(new LambdaAjaxFormComponentUpdatingBehavior("change", t -> {
            // Do nothing just update the model values
        }));
        iriTextfield.setEnabled(false);       
        return iriTextfield;
    }
    
    private class AdvancedIriSettingsPanel
        extends Panel
    {
        private static final long serialVersionUID = 1161350402387498209L;
        private final CompoundPropertyModel<KnowledgeBaseWrapper> kbModel;
        private final List<IRI> concepts;
        private final IModel<String> newConceptIRIString = Model.of();
        
        public AdvancedIriSettingsPanel(String id,
                CompoundPropertyModel<KnowledgeBaseWrapper> aModel)
        {
            super(id);

            kbModel = aModel;
            concepts = kbModel.getObject().getKb().getExplicitlyDefinedRootConcepts();
            setOutputMarkupId(true);

            ListView<IRI> conceptsListView = new ListView<IRI>("explicitlyDefinedRootConcepts",
                    kbModel.bind("kb.explicitlyDefinedRootConcepts"))
            {
                private static final long serialVersionUID = 1L;
                
                @Override
                protected void populateItem(ListItem<IRI> item)
                {   
                    Form<Void> conceptForm = new Form<Void>("conceptForm");
                    conceptForm.add(buildTextField("textField", item.getModel()));
                    conceptForm.add(new LambdaAjaxLink("removeConcept", t -> {
                        AdvancedIriSettingsPanel.this.actionRemoveConcept(t, item.getModelObject());
                    }));
                    item.add(conceptForm);
                }

            };
            conceptsListView.setOutputMarkupId(true);
            add(conceptsListView);
            
            TextField<String> newRootConcept = new TextField<String>("newConceptField",
                    newConceptIRIString);
            newRootConcept.add(new LambdaAjaxFormComponentUpdatingBehavior("change", t -> {
                // Do nothing just update the model values
            }));
            add(newRootConcept);
            LambdaAjaxLink specifyConcept = new LambdaAjaxLink("newExplicitConcept",
                    AdvancedIriSettingsPanel.this::actionNewRootConcept);
            add(specifyConcept);
            specifyConcept.add(new Label("add", new ResourceModel("specifyRootConcept")));
        }
        
        private void actionNewRootConcept(AjaxRequestTarget aTarget) {
            ValueFactory vf = SimpleValueFactory.getInstance();
            IRI concept = vf.createIRI(newConceptIRIString.getObject());
            if (isConceptValid(kbModel.getObject().getKb(), concept, true)) {
                concepts.add(concept);
                newConceptIRIString.setObject(null);
            }
            else {
                error("Concept does not exist or has already been specified");
                aTarget.addChildren(getPage(), IFeedback.class);
            }
            aTarget.add(advancedSettingsPanel);
        }
        
        private void actionRemoveConcept(AjaxRequestTarget aTarget, IRI iri) {
            concepts.remove(iri);
            aTarget.add(advancedSettingsPanel);
        }
        
        public boolean isConceptValid(KnowledgeBase kb, IRI conceptIRI, boolean aAll)
            throws QueryEvaluationException
        {
            return kbService.readConcept(kbModel.getObject().getKb(), conceptIRI.stringValue())
                    .isPresent()
                    && !concepts.contains(conceptIRI);  
        }
    }
}
