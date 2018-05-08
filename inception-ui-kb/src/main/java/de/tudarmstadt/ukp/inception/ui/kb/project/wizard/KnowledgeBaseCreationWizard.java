/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.inception.ui.kb.project.wizard;

import static de.tudarmstadt.ukp.inception.kb.KnowledgeBases.KNOWLEDGE_BASES;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.wizard.CancelButton;
import org.apache.wicket.extensions.wizard.FinishButton;
import org.apache.wicket.extensions.wizard.IWizard;
import org.apache.wicket.extensions.wizard.IWizardStep;
import org.apache.wicket.extensions.wizard.dynamic.DynamicWizardModel;
import org.apache.wicket.extensions.wizard.dynamic.DynamicWizardStep;
import org.apache.wicket.extensions.wizard.dynamic.IDynamicWizardStep;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.wicket.kendo.ui.form.combobox.ComboBox;

import de.agilecoders.wicket.core.markup.html.bootstrap.button.Buttons;
import de.agilecoders.wicket.core.markup.html.bootstrap.form.radio.BootstrapRadioGroup;
import de.agilecoders.wicket.core.markup.html.bootstrap.form.radio.BootstrapRadioGroup.ISelectionChangeHandler;
import de.agilecoders.wicket.core.markup.html.bootstrap.form.radio.EnumRadioChoiceRenderer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.kb.IriConstants;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.RepositoryType;
import de.tudarmstadt.ukp.inception.kb.io.FileUploadHelper;
import de.tudarmstadt.ukp.inception.kb.reification.Reification;
import de.tudarmstadt.ukp.inception.ui.kb.project.EnrichedKnowledgeBase;
import de.tudarmstadt.ukp.inception.ui.kb.project.EnrichedKnowledgeBaseUtils;
import de.tudarmstadt.ukp.inception.ui.kb.project.KnowledgeBaseListPanel;

/**
 * Wizard for registering a new knowledge base for a project.
 */
public class KnowledgeBaseCreationWizard extends BootstrapWizard {
    
    /*-
     * Wizard structure as of 2018-02 (use http://asciiflow.com):
     * 
     *             REMOTE                                   
     *            +-------> RemoteRepS. +-+                  
     *            |                       |                  
     * TypeStep +-+                       +-> SchemaConfigS. +-> FINISH
     *            |                       |
     *            +-------> LocalRepS.  +-+
     *             LOCAL
     */

    private static final long serialVersionUID = -3459525951269555510L;
    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseCreationWizard.class);
    private static final int MAXIMUM_REMOTE_REPO_SUGGESTIONS = 10;

    private @SpringBean KnowledgeBaseService kbService;

    private final Map<String, File> uploadedFiles;
    private final IModel<Project> projectModel;
    private final DynamicWizardModel wizardModel;
    private final CompoundPropertyModel<EnrichedKnowledgeBase> wizardDataModel;
    private final IModel<IriSchemaType> selectedIriSchema;

    public KnowledgeBaseCreationWizard(String id, IModel<Project> aProjectModel) {
        super(id);
        
        // needed for the re-rendering in SchemaConfigurenStep
        setOutputMarkupId(true);
        
        selectedIriSchema = new Model<IriSchemaType>();
        
        uploadedFiles = new HashMap<>();

        projectModel = aProjectModel;
        wizardDataModel = new CompoundPropertyModel<>(new EnrichedKnowledgeBase());

        wizardModel = new DynamicWizardModel(new TypeStep(null, wizardDataModel));
        wizardModel.setLastVisible(false);
        init(wizardModel);
    }

    /**
     * Wizard step asking for the KB name and whether it's a local or remote repository.
     */
    private final class TypeStep extends DynamicWizardStep {

        private static final long serialVersionUID = 2632078392967948962L;
        
        private CompoundPropertyModel<EnrichedKnowledgeBase> model;

        public TypeStep(IDynamicWizardStep previousStep,
                CompoundPropertyModel<EnrichedKnowledgeBase> model) {
            super(previousStep, "", "", model);
            this.model = model;

            add(nameField("name", "kb.name"));
            add(repositoryTypeRadioButtons("type", "kb.type"));

            add(selectReificationStrategy("reification", "reification"));
        }

        private DropDownChoice<Reification> selectReificationStrategy(String id, String property)
        {
            final List<Reification> reificationList = Arrays.asList(Reification.values());

            DropDownChoice<Reification> reificationDropDownChoice = new
                DropDownChoice<>(id, model.bind(property), reificationList);
            return reificationDropDownChoice;
        }

        @Override
        public boolean isLastStep() {
            return false;
        }

        @Override
        public IDynamicWizardStep next() {
            switch (model.getObject().getKb().getType()) {
            case LOCAL:
                return new LocalRepositoryStep(this, model);
            case REMOTE:
                return new RemoteRepositoryStep(this, model);
            default:
                throw new IllegalStateException();
            }
        }

        private TextField<String> nameField(String id, String property) {
            TextField<String> nameField = new RequiredTextField<>(id, model.bind(property));
            nameField.add(knowledgeBaseNameValidator());
            return nameField;
        }

        private IValidator<String> knowledgeBaseNameValidator() {
            return (validatable -> {
                String kbName = validatable.getValue();
                if (kbService.knowledgeBaseExists(projectModel.getObject(), kbName)) {
                    String message = String.format(
                        "There already exists a knowledge base in the project with name: [%s]!",
                        kbName
                    );
                    validatable.error(new ValidationError(message));
                }
            });
        }

        private BootstrapRadioGroup<RepositoryType> repositoryTypeRadioButtons(String id,
                                                                               String property) {
            // subclassing is necessary for setting this form input as required
            return new BootstrapRadioGroup<RepositoryType>(id, model.bind(property),
                Arrays.asList(RepositoryType.values()),
                new EnumRadioChoiceRenderer<>(Buttons.Type.Default, this)) {

                private static final long serialVersionUID = -3015289695381851498L;

                @Override
                protected RadioGroup<RepositoryType> newRadioGroup(String id,
                                                                   IModel<RepositoryType> model) {
                    RadioGroup<RepositoryType> group = super.newRadioGroup(id, model);
                    group.setRequired(true);
                    group.add(new AttributeAppender("class", " btn-group-justified"));
                    return group;
                }
            };
        }
    }
    
    /**
     * Wizard step providing a file upload functionality for local (native) knowledge bases.
     */
    private final class LocalRepositoryStep extends DynamicWizardStep {

        private static final long serialVersionUID = 8212277960059805657L;
        
        private CompoundPropertyModel<EnrichedKnowledgeBase> model;
        private FileUploadField fileUpload;
        private boolean completed;

        public LocalRepositoryStep(IDynamicWizardStep previousStep,
                CompoundPropertyModel<EnrichedKnowledgeBase> model) {
            super(previousStep);
            this.model = model;
            completed = true;
            
            fileUpload = new FileUploadField("upload");
            add(fileUpload);
        }
        
        @Override
        public void applyState() {
            // local knowledge bases are editable by default
            model.getObject().getKb().setReadOnly(false);
            try {
                List<File> fileUploads = new ArrayList<>();
                for (FileUpload fu : fileUpload.getFileUploads()) {
                    File tmpFile = uploadFile(fu);
                    fileUploads.add(tmpFile);
                }
                model.getObject().setFiles(fileUploads);
            } catch (Exception e) {
                completed = false;
                log.error("Error while uploading files", e);
                error("Could not upload files");
            }
        }

        private File uploadFile(FileUpload fu) throws Exception {
            String fileName = fu.getClientFileName();
            if (!uploadedFiles.containsKey(fileName)) {
                FileUploadHelper fileUploadHelper = new FileUploadHelper(getApplication());
                File tmpFile = fileUploadHelper.writeToTemporaryFile(fu, model);
                uploadedFiles.put(fileName, tmpFile);
            } else {
                log.debug("File [{}] already downloaded, skipping!", fileName);
            }
            return uploadedFiles.get(fileName);
        }

        @Override
        public boolean isLastStep() {
            return false;
        }

        @Override
        public IDynamicWizardStep next() {
            return new SchemaConfigurationStep(this, model);
        }

        @Override
        public boolean isComplete() {
            return completed;
        }
    }

    /**
     * Wizard step asking for the remote repository URL.
     */
    private final class RemoteRepositoryStep extends DynamicWizardStep {

        private static final long serialVersionUID = -707885872360370015L;

        private CompoundPropertyModel<EnrichedKnowledgeBase> model;

        public RemoteRepositoryStep(IDynamicWizardStep previousStep,
                CompoundPropertyModel<EnrichedKnowledgeBase> model) {
            super(previousStep, "", "", model);
            this.model = model;
            
            RequiredTextField<String> urlField = new RequiredTextField<>("url");
            urlField.add(EnrichedKnowledgeBaseUtils.URL_VALIDATOR);
            add(urlField);
            
            // for up to MAXIMUM_REMOTE_REPO_SUGGESTIONS of knowledge bases, create a link which
            // directly fills in the URL field (convenient for both developers AND users :))
            List<String> suggestions = new ArrayList<>(KNOWLEDGE_BASES.keySet());
            suggestions = suggestions.subList(0,
                    Math.min(suggestions.size(), MAXIMUM_REMOTE_REPO_SUGGESTIONS));
            add(new ListView<String>("suggestions", suggestions) {

                private static final long serialVersionUID = 4179629475064638272L;

                @Override
                protected void populateItem(ListItem<String> item) {
                    // add a link for one knowledge base with proper label
                    LambdaAjaxLink link = new LambdaAjaxLink("suggestionLink", t -> {
                        model.getObject().setUrl(KNOWLEDGE_BASES.get(item.getModelObject()));
                        t.add(urlField);
                    });
                    link.add(new Label("suggestionLabel", item.getModelObject()));
                    item.add(link);
                }
            });
            add(new CheckBox("supportConceptLinking",
                    new PropertyModel<Boolean>(model, "supportConceptLinking")));
        }
        
        @Override
        public void applyState() {
            // MB: as of 2018-02, all remote knowledge bases are read-only, hence the
            // PermissionsStep is currently not shown. Therefore, set the read-only property here
            // manually.
            model.getObject().getKb().setReadOnly(true);
        }

        @Override
        public boolean isLastStep() {
            return false;
        }

        @Override
        public IDynamicWizardStep next() {
            return new SchemaConfigurationStep(this, model);
        }
    }

    /**
     * Wizard step asking for the knowledge base schema
     */
    private final class SchemaConfigurationStep extends DynamicWizardStep {
        private static final long serialVersionUID = -12355235971946712L;

        private final CompoundPropertyModel<EnrichedKnowledgeBase> model;
        private boolean completed;

        public SchemaConfigurationStep(IDynamicWizardStep previousStep,
                                       CompoundPropertyModel<EnrichedKnowledgeBase> aModel)
        {
            super(previousStep, "", "", aModel);
            model = aModel;
            completed = true;

            // RadioGroup to select the IriSchemaType
            BootstrapRadioGroup<IriSchemaType> iriSchemaChoice = new BootstrapRadioGroup<>(
                    "iriSchema", selectedIriSchema, Arrays.asList(IriSchemaType.values()),
                    new EnumRadioChoiceRenderer<>(Buttons.Type.Default, this));
            iriSchemaChoice.setOutputMarkupId(true);

            // Add text fields for classIri, subclassIri and typeIri
            ComboBox<String> classField = buildComboBox("classIri", model, IriConstants.CLASS_IRIS);
            add(classField);

            ComboBox<String> subclassField = buildComboBox("subclassIri", model,
                    IriConstants.SUBCLASS_IRIS);
            add(subclassField);

            ComboBox<String> typeField = buildComboBox("typeIri", model, IriConstants.TYPE_IRIS);
            add(typeField);
            
            // OnChange update the model with corresponding iris
            iriSchemaChoice.setChangeHandler(new ISelectionChangeHandler<IriSchemaType>()
            {
                private static final long serialVersionUID = 1653808650286121732L;

                @Override
                public void onSelectionChanged(AjaxRequestTarget target, IriSchemaType bean)
                {
                    classField.setModelObject(bean.getClassIriString());
                    subclassField.setModelObject(bean.getSubclassIriString());
                    typeField.setModelObject(bean.getTypeIriString());
                    // For some reason it does not work if just the checkboxes
                    // are added to the target
                    target.addChildren(getPage(), KnowledgeBaseCreationWizard.class);
                }
            });

            add(iriSchemaChoice);
            
            // Base Prefix Label and TextField only shown in CUSTOM - "mode"
            Label basePrefixLabel = new Label("basePrefixLabel", "Base Prefix") {

                private static final long serialVersionUID = -1664209681297576256L;

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    setVisible(IriSchemaType.CUSTOMSCHEMA.equals(selectedIriSchema.getObject()));
                }
            }; 
            
            add(basePrefixLabel);
            
            TextField<String> basePrefix = new TextField<String>("basePrefix",
                    model.bind("basePrefix"))
            {
                private static final long serialVersionUID = -8973666497412424655L;

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    setVisible(IriSchemaType.CUSTOMSCHEMA.equals(selectedIriSchema.getObject()));
                }
            };
            basePrefix.setDefaultModelObject(KnowledgeBaseService.INCEPTION_NAMESPACE);
            add(basePrefix);
        }
        
        private ComboBox<String> buildComboBox(String id,
                CompoundPropertyModel<EnrichedKnowledgeBase> model, List<IRI> iris)
        {

            List<String> choices = iris.stream().map(IRI::stringValue).collect(Collectors.toList());
            ComboBox<String> comboBox = new ComboBox<String>(id, model.bind(id), choices)
            {

                private static final long serialVersionUID = 1575770301215073872L;

                @Override
                protected void onConfigure()
                {
                    super.onConfigure();
                    setEnabled(IriSchemaType.CUSTOMSCHEMA.equals(selectedIriSchema.getObject()));
                }
            };
            comboBox.setOutputMarkupId(true);
            comboBox.setRequired(true);
            comboBox.add(EnrichedKnowledgeBaseUtils.URL_VALIDATOR);
            comboBox.add(new LambdaAjaxFormComponentUpdatingBehavior("change", t -> {
                // Do nothing just update the model values
            }));
            return comboBox;
        }
        
        @Override
        public void applyState()
        {   
            EnrichedKnowledgeBase ekb = wizardDataModel.getObject();
            ekb.getKb().setProject(projectModel.getObject());
       
            try {
                EnrichedKnowledgeBaseUtils.registerEkb(ekb, kbService);
            } catch (Exception e) {
                error(e.getMessage());
                
            }
        }

        @Override
        public boolean isComplete()
        {
            return completed;
        }

        @Override
        public boolean isLastStep()
        {
            return true;
        }

        @Override
        public IDynamicWizardStep next()
        {
            return null;
        }
    }

    @Override
    protected Component newButtonBar(String id) {
        // add Bootstrap-compatible button bar which closes the parent dialog via the cancel and
        // finish buttons
        Component buttonBar = new BootstrapWizardButtonBar(id, this) {

            private static final long serialVersionUID = 5657260438232087635L;

            @Override
            protected FinishButton newFinishButton(String id, IWizard wizard) {
                FinishButton button = new FinishButton(id, wizard) {
                    private static final long serialVersionUID = -7070739469409737740L;
                    
                    @Override
                    public void onAfterSubmit() {
                        // update the list panel and close the dialog - this must be done in
                        // onAfterSubmit, otherwise it cancels out the call to onFinish()
                        IWizardStep step = wizardModel.getActiveStep();
                        // do not close the wizard if no schema type has been selected
                        if (selectedIriSchema.getObject() == null) {
                            error("Please select a schema type.");
                        }
                        else if (step.isComplete()) {
                            AjaxRequestTarget target = RequestCycle.get()
                                    .find(AjaxRequestTarget.class);
                            target.add(findParent(KnowledgeBaseListPanel.class));
                            findParent(KnowledgeBaseCreationDialog.class).close(target);
                        }
                    }
                };
                return button;
            }

            @Override
            protected CancelButton newCancelButton(String id, IWizard wizard) {
                CancelButton button = super.newCancelButton(id, wizard);
                button.add(new AjaxEventBehavior("click") {

                    private static final long serialVersionUID = 3425946914411261187L;

                    @Override
                    protected void onEvent(AjaxRequestTarget target) {
                        findParent(KnowledgeBaseCreationDialog.class).close(target);
                    }
                });
                return button;
            }
        };
        return buttonBar;
    }
    
    public enum IriSchemaType
    {
        RDFSCHEMA(RDFS.CLASS.stringValue(), RDFS.SUBCLASSOF.stringValue(),
                    RDF.TYPE.stringValue()),
        
        WIKIDATASCHEMA(IriConstants.WIKIDATA_CLASS.stringValue(),
                        IriConstants.WIKIDATA_SUBCLASS.stringValue(),
                        IriConstants.WIKIDATA_TYPE.stringValue()),
        
        OWLSCHEMA(OWL.CLASS.stringValue(), RDFS.SUBCLASSOF.stringValue(),
                    RDF.TYPE.stringValue()),
        
        CUSTOMSCHEMA("", "", "");

        private final String classIriString;
        private final String subclassIriString;
        private final String typeIriString;

        private IriSchemaType(String aClassIriString, String aSubclassIriString,
                String aTypeIriString)
        {
            classIriString = aClassIriString;
            subclassIriString = aSubclassIriString;
            typeIriString = aTypeIriString;
        }

        public String getClassIriString()
        {
            return classIriString;
        }

        public String getSubclassIriString()
        {
            return subclassIriString;
        }

        public String getTypeIriString()
        {
            return typeIriString;
        }
    }
}
