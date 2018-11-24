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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.sparql.config.SPARQLRepositoryConfig;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.agilecoders.wicket.core.markup.html.bootstrap.button.Buttons;
import de.agilecoders.wicket.core.markup.html.bootstrap.form.radio.BootstrapRadioGroup;
import de.agilecoders.wicket.core.markup.html.bootstrap.form.radio.EnumRadioChoiceRenderer;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ConfirmationDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.spring.ApplicationEventPublisherHolder;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.RepositoryType;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBaseProperties;
import de.tudarmstadt.ukp.inception.kb.event.KnowledgeBaseConfigurationChangedEvent;
import de.tudarmstadt.ukp.inception.kb.io.FileUploadDownloadHelper;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public class KnowledgeBaseDetailsPanel
    extends Panel
{
    private @SpringBean ApplicationEventPublisherHolder applicationEventPublisherHolder;

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
    private static final RDFFormat getRdfFormatForFileExt(String fileExt)
    {
        return EXPORT_FORMATS.stream().filter(f -> f.getDefaultFileExtension().equals(fileExt))
            .findAny().get();
    }

    private static final List<RDFFormat> EXPORT_FORMATS = Arrays
        .asList(RDFFormat.RDFXML, RDFFormat.NTRIPLES, RDFFormat.TURTLE);
    private static final List<String> EXPORT_FORMAT_FILE_EXTENSIONS = EXPORT_FORMATS.stream()
        .map(f -> f.getDefaultFileExtension()).collect(Collectors.toList());

    private @SpringBean KnowledgeBaseService kbService;
    private @SpringBean KnowledgeBaseProperties kbProperties;

    private final IModel<KnowledgeBase> kbModel;
    private final CompoundPropertyModel<KnowledgeBaseWrapper> kbwModel;

    private Component title;
    private Component content;

    private ConfirmationDialog confirmationDialog;

    public KnowledgeBaseDetailsPanel(String aId, IModel<KnowledgeBase> aKbModel)
    {
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
        Form<KnowledgeBaseWrapper> form = new Form<KnowledgeBaseWrapper>("form", kbwModel)
        {
            private static final long serialVersionUID = -4253347478525087385L;

            /**
             * When submitting the form, file uploads need to be handled. We don't have an object
             * reference to the FUF at this point, since we can't know if the FUF is a child of the
             * form (we might be in view mode after all). Therefore, cheekily obtain the FUF when
             * submitting.
             */
            @Override protected void onSubmit()
            {
                Component c = get(CONTENT_MARKUP_ID + ":" + LOCAL_WEBMARKUPCONTAINER_MARKUP_ID + ":"
                    + FILE_UPLOAD_FIELD_MARKUP_ID);
                if (c == null) {
                    log.error("Could not find file upload component!");
                    return;
                }

                try {
                    FileUploadField fileUploadField = (FileUploadField) c;
                    FileUploadDownloadHelper fileUploadDownloadHelper =
                        new FileUploadDownloadHelper(getApplication());
                    List<File> fileUploads = new ArrayList<>();
                    for (FileUpload fu : fileUploadField.getFileUploads()) {
                        File tmpFile = fileUploadDownloadHelper
                            .writeFileUploadToTemporaryFile(fu, kbw);
                        fileUploads.add(tmpFile);
                    }
                    kbwModel.getObject().setFiles(fileUploads);
                }
                catch (Exception e) {
                    log.error("Error while uploading files", e);
                    error("Could not upload files");
                }
            }
        };
        add(form);

        // add (disabled) radio choice for local/remote repository
        form.add(new BootstrapRadioGroup<>("type", kbwModel.bind("kb.type"),
            Arrays.asList(RepositoryType.values()),
            new EnumRadioChoiceRenderer<RepositoryType>(Buttons.Type.Default, this)
            {
                private static final long serialVersionUID = 1073440402072678330L;

                @Override public String getButtonClass(RepositoryType option)
                {
                    return super.getButtonClass(option) + " disabled";
                }
            }));

        // add (disabled) reification strategy
        form.add(new Label("reification", kbwModel.bind("kb.reification")));

        // title/content
        title = new KBSettingsTitle(TITLE_MARKUP_ID, kbwModel);
        content = new KBSettingsContent(CONTENT_MARKUP_ID, kbwModel);
        form.add(title);
        form.add(content);

        // re-index button only visible for local KBs
        form.add(new LambdaAjaxLink("delete", KnowledgeBaseDetailsPanel.this::actionDelete));
        form.add(new LambdaAjaxLink("reindex", KnowledgeBaseDetailsPanel.this::actionReindex)
                .add(LambdaBehavior.visibleWhen(() -> RepositoryType.LOCAL
                        .equals(kbwModel.getObject().getKb().getType()))));
        form.add(new AjaxButton("save", form)
        {
            private static final long serialVersionUID = 3393631640806116694L;
            
            @Override
            protected void onError(AjaxRequestTarget aTarget) {
                aTarget.addChildren(getPage(), IFeedback.class);
            }
            
            @Override
            protected void onAfterSubmit(AjaxRequestTarget aTarget)
            {
                // the call needs to occur in onAfterSubmit, otherwise the file uploads are
                // submitted after actionSave is called
                KnowledgeBaseDetailsPanel.this
                    .actionSave(aTarget, (Form<KnowledgeBaseWrapper>) form);
                applicationEventPublisherHolder.get().publishEvent(
                    new KnowledgeBaseConfigurationChangedEvent(this,
                        aKbModel.getObject().getProject()));
            }
        });

        confirmationDialog = new ConfirmationDialog("confirmationDialog");
        add(confirmationDialog);
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();

        setVisible(kbModel != null && kbModel.getObject() != null);
    }

    @Override protected void onModelChanged()
    {
        // propagate the changes to the original knowledge base model
        kbModel.setObject(kbwModel.getObject().getKb());
    }

    private void actionSave(AjaxRequestTarget aTarget, Form<KnowledgeBaseWrapper> aForm)
    {
        aTarget.addChildren(getPage(), IFeedback.class);
        aTarget.add(findParentWithAssociatedMarkup());
        
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
            aTarget.add(this);
        }
        catch (Exception e) {
            error("Unable to save knowledge base: " + e.getLocalizedMessage());
            log.error("Unable to save knowledge base.", e);
        }
    }

    private void actionReindex(AjaxRequestTarget aTarget)
    {
        aTarget.addChildren(getPage(), IFeedback.class);

        KnowledgeBase kb = kbwModel.getObject().getKb();
        try {
            log.info("Starting rebuilding full-text index of {} ... this may take a while ...", kb);
            kbService.rebuildFullTextIndex(kb);
            log.info("Completed rebuilding full-text index of {}", kb);
            success("Completed rebuilding full-text index");
        }
        catch (Exception e) {
            error("Unable to rebuild full text index: " + e.getLocalizedMessage());
            log.error("Unable to rebuild full text index for KB [{}]({}) in project [{}]({})",
                    kb.getName(), kb.getRepositoryId(), kb.getProject().getName(),
                    kb.getProject().getId(), e);
        }
    }
    
    private void actionDelete(AjaxRequestTarget aTarget)
    {
        // delete only if user confirms deletion
        confirmationDialog
            .setTitleModel(new StringResourceModel("kb.details.delete.confirmation.title", this));
        confirmationDialog.setContentModel(
            new StringResourceModel("kb.details.delete.confirmation.content", this,
                kbwModel.bind("kb")));
        confirmationDialog.show(aTarget);
        confirmationDialog.setConfirmAction(_target -> {
            KnowledgeBase kb = kbwModel.getObject().getKb();
            try {
                kbService.removeKnowledgeBase(kb);
                kbwModel.getObject().setKb(null);
                modelChanged();
            }
            catch (RepositoryException | RepositoryConfigException e) {
                error("Unable to remove knowledge base: " + e.getLocalizedMessage());
                log.error("Unable to remove knowledge base.", e);
                _target.addChildren(getPage(), IFeedback.class);

            }
            _target.add(this);
            _target.add(findParentWithAssociatedMarkup());
        });
    }

    private class KBSettingsTitle
        extends Fragment
    {

        private static final long serialVersionUID = -5459222108913316798L;

        public KBSettingsTitle(String id, CompoundPropertyModel<KnowledgeBaseWrapper> model)
        {
            super(id, "kbSettingsTitle", KnowledgeBaseDetailsPanel.this, model);
            add(new RequiredTextField<>("name", model.bind("kb.name")));
        }
    }

    private class KBSettingsContent
        extends Fragment
    {

        private static final long serialVersionUID = 7838564354437836375L;
        protected CompoundPropertyModel<KnowledgeBaseWrapper> kbwModel;

        public KBSettingsContent(String id, CompoundPropertyModel<KnowledgeBaseWrapper> aKbwModel)
        {
            super(id, "kbSettingsContent", KnowledgeBaseDetailsPanel.this, aKbwModel);

            kbwModel = aKbwModel;

            Component generalSettings = new GeneralSettingsPanel("generalSettings",
                Model.of(kbModel.getObject().getProject()), kbwModel);
            add(generalSettings);
            generalSettings.get("name").setVisible(false);


            Component accessSpecificSettings = new AccessSpecificSettingsPanel(
                "accessSpecificSettings", Model.of(kbModel.getObject().getProject()), kbwModel,
                Collections.emptyMap());
            add(accessSpecificSettings);
            accessSpecificSettings.get("remoteSpecificSettings:suggestions").setVisible(false);

            Component querySettings = new QuerySettingsPanel("querySettings",
                Model.of(kbModel.getObject().getProject()), kbwModel);
            add(querySettings);

            Component schemaMapping = new KnowledgeBaseIriPanel("schemaMapping", kbwModel);
            add(schemaMapping);

            Component rootConcepts = new RootConceptsPanel("rootConcepts", kbwModel);
            add(rootConcepts);
        }
    }
}
