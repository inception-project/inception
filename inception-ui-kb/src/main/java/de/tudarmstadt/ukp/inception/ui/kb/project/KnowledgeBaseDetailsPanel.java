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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.resource.IResourceStream;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.sparql.config.SPARQLRepositoryConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.agilecoders.wicket.core.markup.html.bootstrap.button.Buttons;
import de.agilecoders.wicket.core.markup.html.bootstrap.form.BootstrapCheckbox;
import de.agilecoders.wicket.core.markup.html.bootstrap.form.radio.BootstrapRadioGroup;
import de.agilecoders.wicket.core.markup.html.bootstrap.form.radio.EnumRadioChoiceRenderer;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ConfirmationDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModelAdapter;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.AjaxDownloadLink;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.TempFileResource;
import de.tudarmstadt.ukp.inception.app.bootstrap.DisabledBootstrapCheckbox;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.RepositoryType;
import de.tudarmstadt.ukp.inception.kb.io.FileUploadHelper;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public class KnowledgeBaseDetailsPanel extends Panel {

    private static final long serialVersionUID = -3550082954966752196L;
    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseDetailsPanel.class);

    private static final String TITLE_MARKUP_ID = "title";
    private static final String CONTENT_MARKUP_ID = "content";
    private static final String COMMON_WEBMARKUPCONTAINER_MARKUP_ID = "common-content";
    private static final String LOCAL_WEBMARKUPCONTAINER_MARKUP_ID = "local";
    private static final String REMOTE_WEBMARKUPCONTAINER_MARKUP_ID = "remote";
    private static final String FILE_UPLOAD_FIELD_MARKUP_ID = "upload";
    
    /**
     * Given the default file extension of an RDF format, returns the corresponding
     * {@link RDFFormat}. This factory method detour is necessary because {@link RDFFormat} should
     * be used as a model, but is not serializable.
     * 
     * @param fileExt
     * @return an {@link RDFFormat}
     */
    private static final RDFFormat getRdfFormatForFileExt(String fileExt) {
        return EXPORT_FORMATS.stream()
                .filter(f -> f.getDefaultFileExtension().equals(fileExt))
                .findAny()
                .get();
    }
    private static final List<RDFFormat> EXPORT_FORMATS = Arrays.asList(RDFFormat.RDFXML,
            RDFFormat.NTRIPLES, RDFFormat.TURTLE);
    private static final List<String> EXPORT_FORMAT_FILE_EXTENSIONS = EXPORT_FORMATS.stream()
            .map(f -> f.getDefaultFileExtension())
            .collect(Collectors.toList());

    private @SpringBean KnowledgeBaseService kbService;

    private final IModel<KnowledgeBase> kbModel;
    private final CompoundPropertyModel<KnowledgeBaseWrapper> kbwModel;
    
    private Component title;
    private Component content;
    private boolean isEditing;
    
    private ConfirmationDialog confirmationDialog;

    public KnowledgeBaseDetailsPanel(String aId, IModel<KnowledgeBase> aKbModel) {
        super(aId, null);
        setOutputMarkupPlaceholderTag(true);

        kbModel = aKbModel;

        KnowledgeBaseWrapper kbw = new KnowledgeBaseWrapper();
        KnowledgeBase kb = kbModel.getObject();
        kbw.setKb(kb);
        // set the URL of the KBW to the current SPARQL URL, if dealing with a remote repository
        if (kbw.getKb().getType() == RepositoryType.REMOTE) {
            RepositoryImplConfig cfg = kbService.getKnowledgeBaseConfig(kb);
            String url = ((SPARQLRepositoryConfig) cfg).getQueryEndpointUrl();
            kbw.setUrl(url);
        }

        // wrap the given knowledge base model, then set it as the default model
        kbwModel = new CompoundPropertyModel<>(Model.of(kbw));
        setDefaultModel(kbwModel);

        // this form contains all the wicket components in this panel; not only the components used
        // for editing, but also the ones for showing information about a KB (when in ViewMode)
        Form<KnowledgeBaseWrapper> form = new Form<KnowledgeBaseWrapper>("form", kbwModel) {
            private static final long serialVersionUID = -4253347478525087385L;

            /**
             * When submitting the form, file uploads need to be handled. We don't have an object
             * reference to the FUF at this point, since we can't know if the FUF is a child of the
             * form (we might be in view mode after all). Therefore, cheekily obtain the FUF when
             * submitting.
             */
            @Override
            protected void onSubmit() {
                Component c = get(CONTENT_MARKUP_ID + ":" + LOCAL_WEBMARKUPCONTAINER_MARKUP_ID + ":"
                        + FILE_UPLOAD_FIELD_MARKUP_ID);
                if (c == null) {
                    log.error("Could not find file upload component!");
                    return;
                }

                try {
                    FileUploadField fileUploadField = (FileUploadField) c;
                    FileUploadHelper fileUploadHelper = new FileUploadHelper(getApplication());
                    List<File> fileUploads = new ArrayList<>();
                    for (FileUpload fu : fileUploadField.getFileUploads()) {
                        File tmpFile = fileUploadHelper.writeToTemporaryFile(fu, kbw);
                        fileUploads.add(tmpFile);
                    }
                    kbwModel.getObject().setFiles(fileUploads);
                } catch (Exception e) {
                    log.error("Error while uploading files", e);
                    error("Could not upload files");
                }
            }
        };
        add(form);
        
        // add (disabled) radio choice for local/remote repository
        form.add(new BootstrapRadioGroup<RepositoryType>("type", kbwModel.bind("kb.type"),
                Arrays.asList(RepositoryType.values()),
                new EnumRadioChoiceRenderer<RepositoryType>(Buttons.Type.Default, this) {
                    private static final long serialVersionUID = 1073440402072678330L;

                    @Override
                    public String getButtonClass(RepositoryType option) {
                        return super.getButtonClass(option) + " disabled";
                    }
                }));

        // add (disabled) reification strategy
        form.add(new Label("reification", kbwModel.bind("kb.reification")));

        // title/content
        title = new ViewModeTitle(TITLE_MARKUP_ID, kbwModel);
        content = new ViewMode(CONTENT_MARKUP_ID, kbwModel);
        form.add(title);
        form.add(content);

        // set up form buttons: edit button only visible when not editing, cancel/save buttons only
        // visible when editing
        form.add(new LambdaAjaxLink("delete", KnowledgeBaseDetailsPanel.this::actionDelete));
        form.add(new LambdaAjaxLink("edit", KnowledgeBaseDetailsPanel.this::startEditing) {
            
            private static final long serialVersionUID = -2013888340002855855L;

            @Override
            public boolean isVisible() {
                return !isEditing;
            }
        });
        form.add(new AjaxButton("save", form) {

            private static final long serialVersionUID = 3393631640806116694L;
            
            @Override
            public boolean isVisible() {
                return isEditing;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void onAfterSubmit(AjaxRequestTarget target, Form<?> form) {
                // the call needs to occur in onAfterSubmit, otherwise the file uploads are
                // submitted after actionSave is called
                KnowledgeBaseDetailsPanel.this.actionSave(target,
                        (Form<KnowledgeBaseWrapper>) form);
            }
        });
        form.add(new LambdaAjaxLink("cancel", KnowledgeBaseDetailsPanel.this::stopEditing) {

            private static final long serialVersionUID = -6654306757363572019L;

            @Override
            public boolean isVisible() {
                return isEditing;
            }
        });

        confirmationDialog = new ConfirmationDialog("confirmationDialog");
        add(confirmationDialog);
    }
    
    @Override
    protected void onConfigure() {
        super.onConfigure();
        setVisible(kbModel != null && kbModel.getObject() != null);
    }

    @Override
    protected void onModelChanged() {
        // propagate the changes to the original knowledge base model
        kbModel.setObject(kbwModel.getObject().getKb());
    }

    private void actionSave(AjaxRequestTarget aTarget, Form<KnowledgeBaseWrapper> aForm)
    {
        try {
            KnowledgeBaseWrapper kbw = kbwModel.getObject();

            // if dealing with a remote repository and a non-empty URL, get a new
            // RepositoryImplConfig
            // for the new URL; otherwise keep using the existing config
            RepositoryImplConfig cfg;
            if (kbw.getKb().getType() == RepositoryType.REMOTE && kbw.getUrl() != null) {
                cfg = kbService.getRemoteConfig(kbw.getUrl());
            }
            else {
                cfg = kbService.getKnowledgeBaseConfig(kbw.getKb());
            }
            KnowledgeBaseWrapper.updateKb(kbw, cfg, kbService);
            modelChanged();
            stopEditing(aTarget);
            aTarget.add(findParentWithAssociatedMarkup());
        }
        catch (Exception e) {
            error("Unable to save knowledgebase: " + e.getLocalizedMessage());
            log.error("Unable to save knowledgebase.", e);
        }
    }

    private void actionDelete(AjaxRequestTarget aTarget) {
        // delete only if user confirms deletion
        confirmationDialog.setTitleModel(
                new StringResourceModel("kb.details.delete.confirmation.title", this));
        confirmationDialog.setContentModel(new StringResourceModel(
                "kb.details.delete.confirmation.content", this, kbwModel.bind("kb")));
        confirmationDialog.show(aTarget);
        confirmationDialog.setConfirmAction((t) -> {
            KnowledgeBase kb = kbwModel.getObject().getKb();
            try {
                kbService.removeKnowledgeBase(kb);
                kbwModel.getObject().setKb(null);
                modelChanged();
            }
            catch (RepositoryException | RepositoryConfigException e) {
                error("Unable to remove knowledge base: " + e.getLocalizedMessage());
                log.error("Unable to remove knowledge base.", e);
                
            }
            t.add(this);
            t.add(findParentWithAssociatedMarkup());
        });
    }

    private void actionClear(AjaxRequestTarget aTarget) {
        try {
            kbService.clear(kbwModel.getObject().getKb());
            info(new StringResourceModel("kb.details.local.contents.clear.feedback",
                    kbwModel.bind("kb")));
            aTarget.add(this);
        } catch (RepositoryException e) {
            error(e);
        }
    }
    
    private IResourceStream actionExport(String rdfFormatFileExt) {
        return new TempFileResource((os) -> kbService.exportData(kbModel.getObject(),
                getRdfFormatForFileExt(rdfFormatFileExt), os));
    }

    private void startEditing(AjaxRequestTarget aTarget) {
        title = title.replaceWith(new EditModeTitle(TITLE_MARKUP_ID, kbwModel));
        content = content.replaceWith(new EditMode(CONTENT_MARKUP_ID, kbwModel));
        aTarget.add(this);
        isEditing = true;
    }
    
    private void stopEditing(AjaxRequestTarget aTarget) {
        title = title.replaceWith(new ViewModeTitle(TITLE_MARKUP_ID, kbwModel));
        content = content.replaceWith(new ViewMode(CONTENT_MARKUP_ID, kbwModel));
        aTarget.add(this);
        isEditing = false;
    }
    
    /**
     * Fragment for viewing/editing knowledge bases, with built-in separation for form components
     * relevant to only local or remote knowledge bases.
     */
    private abstract class DetailFragment extends Fragment {

        private static final long serialVersionUID = 4325217938170626840L;
        
        protected CompoundPropertyModel<KnowledgeBaseWrapper> model;
        
        public DetailFragment(String id, String markupId,
                CompoundPropertyModel<KnowledgeBaseWrapper> model) {
            super(id, markupId, KnowledgeBaseDetailsPanel.this, model);
            
            this.model = model;
            boolean isHandlingLocalRepository = model.getObject()
                    .getKb()
                    .getType() == RepositoryType.LOCAL;

            // container for form components common to both local and remote KBs
            WebMarkupContainer common = new WebMarkupContainer(COMMON_WEBMARKUPCONTAINER_MARKUP_ID);
            add(common);
            setUpCommonComponents(common);

            // container for form components related to local KBs
            WebMarkupContainer local = new WebMarkupContainer(LOCAL_WEBMARKUPCONTAINER_MARKUP_ID);
            add(local);
            local.setVisibilityAllowed(isHandlingLocalRepository);
            setUpLocalKnowledgeBaseComponents(local);

            // container for form components related to remote KBs
            WebMarkupContainer remote = new WebMarkupContainer(REMOTE_WEBMARKUPCONTAINER_MARKUP_ID);
            add(remote);
            remote.setVisibilityAllowed(!isHandlingLocalRepository);
            setUpRemoteKnowledgeBaseComponents(remote);
        }

        protected abstract void setUpCommonComponents(WebMarkupContainer wmc);
        protected abstract void setUpLocalKnowledgeBaseComponents(WebMarkupContainer wmc);
        protected abstract void setUpRemoteKnowledgeBaseComponents(WebMarkupContainer wmc);
    }

    private class ViewModeTitle extends Fragment {

        private static final long serialVersionUID = -346255717342200090L;

        public ViewModeTitle(String id, CompoundPropertyModel<KnowledgeBaseWrapper> model) {
            super(id, "viewModeTitle", KnowledgeBaseDetailsPanel.this, model);
            add(new Label("name", model.bind("kb.name")));
        }
    }

    private class ViewMode extends DetailFragment {

        private static final long serialVersionUID = -6584701320032256335L;

        public ViewMode(String id, CompoundPropertyModel<KnowledgeBaseWrapper> model) {
            super(id, "viewModeContent", model);
        }

        @Override
        protected void setUpCommonComponents(WebMarkupContainer wmc) {
            // Schema configuration
            addDisabledIriField(wmc, "classIri", model.bind("kb.classIri"));
            addDisabledIriField(wmc, "subclassIri", model.bind("kb.subclassIri"));
            addDisabledIriField(wmc, "typeIri", model.bind("kb.typeIri"));
            addDisabledIriField(wmc, "descriptionIri", model.bind("kb.descriptionIri"));
            wmc.add(new CheckBox("enabled", model.bind("kb.enabled"))
                .add(LambdaBehavior.onConfigure(it -> it.setEnabled(false))));
            wmc.add(new CheckBox("supportConceptLinking", model.bind("kb.supportConceptLinking"))
                .add(LambdaBehavior.onConfigure(it -> it.setEnabled(false))));
        }

        @Override
        protected void setUpLocalKnowledgeBaseComponents(WebMarkupContainer wmc) {
            // creates a list of export buttons, one for each viable RDF format
            // MB 2018-01: would've been nicer to go for a split button with one recommended format
            // and several others to choose from, but SplitButton in wicket-bootstrap 0.10.16 is
            // totally broken, so we're doing this instead
            ListView<String> lv = new ListView<String>("exportButtons",
                    EXPORT_FORMAT_FILE_EXTENSIONS) {

                private static final long serialVersionUID = -1869762759620557362L;

                @Override
                protected void populateItem(ListItem<String> item) {
                    // creates an appropriately labeled {@link AjaxDownloadLink} which triggers the
                    // download of the contents of the current KB in the given format
                    String fileExtension = item.getModelObject();
                    Model<String> exportFileNameModel = Model
                            .of(kbModel.getObject().getName() + "." + fileExtension);
                    AjaxDownloadLink exportLink = new AjaxDownloadLink("link", exportFileNameModel,
                            LambdaModel.of(() -> KnowledgeBaseDetailsPanel.this
                                    .actionExport(fileExtension)));
                    exportLink.add(
                            new Label("label", new ResourceModel("kb.export." + fileExtension)));
                    item.add(exportLink);
                }
            };
            wmc.add(lv);
            
            wmc.add(new DisabledBootstrapCheckbox("writeprotection", model.bind("kb.readOnly"),
                    new StringResourceModel("kb.details.local.permissions.writeprotection")));
        }

        @Override
        protected void setUpRemoteKnowledgeBaseComponents(WebMarkupContainer wmc) {
            addDisabledUrlField(wmc, "url");
        }

        private void addDisabledIriField(WebMarkupContainer wmc, String id, IModel<IRI> model)
        {
            TextField<IRI> textField = new RequiredTextField<IRI>(id, model)
            {
                private static final long serialVersionUID = 5886070596284072382L;

                @Override
                protected String getModelValue()
                {
                    return getModelObject().stringValue();
                }

                @Override
                protected void onConfigure()
                {
                    setEnabled(false);
                }
            };
            wmc.add(textField);
        }

        private void addDisabledUrlField(WebMarkupContainer wmc, String id)
        {
            TextField<String> textField = new RequiredTextField<String>(id);
            textField.add(LambdaBehavior.onConfigure(tf -> tf.setEnabled(false)));
            wmc.add(textField);
        }
    }

    private class EditModeTitle extends Fragment {

        private static final long serialVersionUID = -5459222108913316798L;

        public EditModeTitle(String id, CompoundPropertyModel<KnowledgeBaseWrapper> model) {
            super(id, "editModeTitle", KnowledgeBaseDetailsPanel.this, model);
            add(new RequiredTextField<>("name", model.bind("kb.name")));
        }
    }

    private class EditMode extends DetailFragment {

        private static final long serialVersionUID = 7838564354437836375L;

        public EditMode(String id, CompoundPropertyModel<KnowledgeBaseWrapper> model) {
            super(id, "editModeContent", model);
        }

        @Override
        protected void setUpCommonComponents(WebMarkupContainer wmc) {
            // Schema configuration
            addIriField(wmc, "classIri", model.bind("kb.classIri"));
            addIriField(wmc, "subclassIri", model.bind("kb.subclassIri"));
            addIriField(wmc, "typeIri", model.bind("kb.typeIri"));
            addIriField(wmc, "descriptionIri", model.bind("kb.descriptionIri"));
            wmc.add(new CheckBox("enabled", model.bind("kb.enabled")));
            wmc.add(new CheckBox("supportConceptLinking", model.bind("kb.supportConceptLinking")));
        }

        @Override
        protected void setUpLocalKnowledgeBaseComponents(WebMarkupContainer wmc) {
            wmc.add(new FileUploadField(FILE_UPLOAD_FIELD_MARKUP_ID,
                    Model.of()));
            
            // add link for clearing the knowledge base contents, enabled only, if there is
            // something to clear            
            AjaxLink<Void> clearLink = new LambdaAjaxLink("clear",
                    KnowledgeBaseDetailsPanel.this::actionClear) {

                private static final long serialVersionUID = -6272361381689154558L;

                @Override
                public boolean isEnabled() {
                    return kbService.isEmpty(model.getObject().getKb());
                }
            };
            wmc.add(clearLink);
            
            wmc.add(new BootstrapCheckbox("writeprotection", model.bind("kb.readOnly"),
                    new StringResourceModel("kb.details.local.permissions.writeprotection")));
        }

        @Override
        protected void setUpRemoteKnowledgeBaseComponents(WebMarkupContainer wmc) {
            // this text field allows for _editing_the location for remote repositories
            addUrlField(wmc, "url");
        }

        private void addIriField(WebMarkupContainer wmc, String id, IModel<IRI> model) {
            IModel<String> adapter = new LambdaModelAdapter<String>(
                () -> model.getObject().stringValue(),
                str -> model.setObject(SimpleValueFactory.getInstance().createIRI(str)));
            TextField<String> textField = new RequiredTextField<String>(id, adapter);
            textField.add(Validators.IRI_VALIDATOR);
            wmc.add(textField);
        }

        private void addUrlField(WebMarkupContainer wmc, String id)
        {
            TextField<String> textField = new RequiredTextField<String>(id);
            textField.add(Validators.URL_VALIDATOR);
            wmc.add(textField);
        }
    }
}
