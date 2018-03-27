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

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextField;
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
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.sparql.config.SPARQLRepositoryConfig;

import de.agilecoders.wicket.core.markup.html.bootstrap.button.Buttons;
import de.agilecoders.wicket.core.markup.html.bootstrap.form.radio.BootstrapRadioGroup;
import de.agilecoders.wicket.core.markup.html.bootstrap.form.radio.EnumRadioChoiceRenderer;
import de.tudarmstadt.ukp.clarin.webanno.support.dialog.ConfirmationDialog;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBase;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.RepositoryType;

public class KnowledgeBaseDetailsPanel extends Panel {

    private static final long serialVersionUID = -3550082954966752196L;

    private static final String TITLE_MARKUP_ID = "title";
    private static final String CONTENT_MARKUP_ID = "content";
    private static final String LOCAL_WEBMARKUPCONTAINER_MARKUP_ID = "local";
    private static final String FILE_UPLOAD_FIELD_MARKUP_ID = "upload";

    private @SpringBean KnowledgeBaseService kbService;

    private IModel<KnowledgeBase> kbModel;
    private CompoundPropertyModel<EnrichedKnowledgeBase> ekbModel;
    
    private Component title;
    private Component content;   
    private boolean isEditing;
    
    private ConfirmationDialog confirmationDialog;

    public KnowledgeBaseDetailsPanel(String aId, IModel<KnowledgeBase> aKbModel) {
        super(aId, null);
        setOutputMarkupPlaceholderTag(true);
        
        kbModel = aKbModel;

        EnrichedKnowledgeBase ekb = new EnrichedKnowledgeBase();
        ekb.setKb(kbModel.getObject());
        // set the URL of the EKB to the current SPARQL URL, if dealing with a remote repository
        if (ekb.getKb().getType() == RepositoryType.REMOTE) {
            RepositoryImplConfig cfg = kbService.getKnowledgeBaseConfig(kbModel.getObject());
            String url = ((SPARQLRepositoryConfig) cfg).getQueryEndpointUrl();
            ekb.setUrl(url);
        }
        
        // wrap the given knowledge base model, then set it as the default model
        ekbModel = new CompoundPropertyModel<>(Model.of(ekb));
        setDefaultModel(ekbModel);

        // this form contains all the wicket components in this panel; not only the components used
        // for editing, but also the ones for showing information about a KB (when in ViewMode)
        Form<EnrichedKnowledgeBase> form = new Form<EnrichedKnowledgeBase>("form", ekbModel) {
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
                if (c != null) {
                    FileUploadField fileUploadField = (FileUploadField) c;
                    List<FileUpload> ul = fileUploadField.getFileUploads();
                    ekbModel.getObject().setFiles(ul);
                }
            }
        };
        add(form);
        
        // add (disabled) radio choice for local/remote repository
        form.add(new BootstrapRadioGroup<RepositoryType>("type", ekbModel.bind("kb.type"),
                Arrays.asList(RepositoryType.values()),
                new EnumRadioChoiceRenderer<RepositoryType>(Buttons.Type.Default, this) {
                    private static final long serialVersionUID = 1073440402072678330L;

                    @Override
                    public String getButtonClass(RepositoryType option) {
                        return super.getButtonClass(option) + " disabled";
                    }
                }));

        // title/content
        title = new ViewModeTitle(TITLE_MARKUP_ID, ekbModel);
        content = new ViewMode(CONTENT_MARKUP_ID, ekbModel);
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
                        (Form<EnrichedKnowledgeBase>) form);
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
        kbModel.setObject(ekbModel.getObject().getKb());
    }

    private void actionSave(AjaxRequestTarget aTarget, Form<EnrichedKnowledgeBase> aForm) {
        EnrichedKnowledgeBase ekb = ekbModel.getObject();

        // if dealing with a remote repository and a non-empty URL, get a new RepositoryImplConfig
        // for the new URL; otherwise keep using the existing config
        RepositoryImplConfig cfg;
        if (ekb.getKb().getType() == RepositoryType.REMOTE && ekb.getUrl() != null) {
            cfg = kbService.getRemoteConfig(ekb.getUrl());
        } else {
            cfg = kbService.getKnowledgeBaseConfig(ekb.getKb());
        }

        try {
            EnrichedKnowledgeBaseUtils.updateEkb(ekb, cfg, kbService);
        } catch (Exception e) {
            error(e.getMessage());
        }
        modelChanged();

        stopEditing(aTarget);
        aTarget.add(findParentWithAssociatedMarkup());        
    }

    private void actionDelete(AjaxRequestTarget aTarget) {
        // delete only if user confirms deletion
        confirmationDialog.setTitleModel(
                new StringResourceModel("kb.details.delete.confirmation.title", this));
        confirmationDialog.setContentModel(new StringResourceModel(
                "kb.details.delete.confirmation.content", this, ekbModel.bind("kb")));
        confirmationDialog.show(aTarget);
        confirmationDialog.setConfirmAction((t) -> {
            KnowledgeBase kb = ekbModel.getObject().getKb();
            kbService.removeKnowledgeBase(kb);
            ekbModel.getObject().setKb(null);
            modelChanged();

            t.add(this);
            t.add(findParentWithAssociatedMarkup());
        });
    }

    private void actionClear(AjaxRequestTarget aTarget) {
        try {
            kbService.clear(ekbModel.getObject().getKb());
            info(new StringResourceModel("kb.details.local.contents.clear.feedback",
                    ekbModel.bind("kb")));
            aTarget.add(this);
        } catch (RepositoryException e) {
            error(e);
        }
    }
    
    private void startEditing(AjaxRequestTarget aTarget) {
        title = title.replaceWith(new EditModeTitle(TITLE_MARKUP_ID, ekbModel));
        content = content.replaceWith(new EditMode(CONTENT_MARKUP_ID, ekbModel));
        aTarget.add(this);
        isEditing = true;
    }
    
    private void stopEditing(AjaxRequestTarget aTarget) {
        title = title.replaceWith(new ViewModeTitle(TITLE_MARKUP_ID, ekbModel));
        content = content.replaceWith(new ViewMode(CONTENT_MARKUP_ID, ekbModel));
        aTarget.add(this);
        isEditing = false;
    }

    private class ViewModeTitle extends Fragment {

        private static final long serialVersionUID = -346255717342200090L;

        public ViewModeTitle(String id, CompoundPropertyModel<EnrichedKnowledgeBase> model) {
            super(id, "viewModeTitle", KnowledgeBaseDetailsPanel.this, model);
            add(new Label("name", model.bind("kb.name")));
        }
    }

    private class ViewMode extends Fragment {

        private static final long serialVersionUID = -6584701320032256335L;

        public ViewMode(String id, CompoundPropertyModel<EnrichedKnowledgeBase> model) {
            super(id, "viewModeContent", KnowledgeBaseDetailsPanel.this, model);
            
            boolean isShowingLocalRepository = model.getObject()
                    .getKb()
                    .getType() == RepositoryType.LOCAL;

            TextField<String> location = new TextField<String>("location", Model.of(
                    kbService.getKnowledgeBaseInfo(kbModel.getObject()).getLocation().toString()));
            location.setVisibilityAllowed(isShowingLocalRepository);
            add(location);

            TextField<String> url = new TextField<>("url");
            url.setVisibilityAllowed(!isShowingLocalRepository);
            add(url);
        }
    }

    private class EditModeTitle extends Fragment {

        private static final long serialVersionUID = -5459222108913316798L;

        public EditModeTitle(String id, CompoundPropertyModel<EnrichedKnowledgeBase> model) {
            super(id, "editModeTitle", KnowledgeBaseDetailsPanel.this, model);
            add(new RequiredTextField<>("name", model.bind("kb.name")));
        }
    }

    private class EditMode extends Fragment {

        private static final long serialVersionUID = 7838564354437836375L;

        public EditMode(String id, CompoundPropertyModel<EnrichedKnowledgeBase> model) {
            super(id, "editModeContent", KnowledgeBaseDetailsPanel.this, model);
            
            KnowledgeBase kb = model.getObject().getKb();
            boolean isEditingLocalRepository = kb.getType() == RepositoryType.LOCAL;
            
            // container for form components related to local KBs: for uploading additional RDF
            // files and for viewing the file system location of the KB
            WebMarkupContainer local = new WebMarkupContainer(LOCAL_WEBMARKUPCONTAINER_MARKUP_ID);
            add(local);
            local.setVisibilityAllowed(isEditingLocalRepository);
            local.add(new FileUploadField(FILE_UPLOAD_FIELD_MARKUP_ID,
                    Model.of()));
            local.add(new TextField<String>("location", Model.of(
                    kbService.getKnowledgeBaseInfo(kbModel.getObject()).getLocation().toString())));
            
            // add link for clearing the knowledge base contents, enabled only, if there is
            // something to clear            
            AjaxLink<Void> clearLink = new LambdaAjaxLink("clear",
                    KnowledgeBaseDetailsPanel.this::actionClear) {

                private static final long serialVersionUID = -6272361381689154558L;

                @Override
                public boolean isEnabled() {
                    return kbService.isEmpty(kb);
                }
            };
            local.add(clearLink);            

            // this text field allows for _editing_the location for remote repositories
            RequiredTextField<String> urlField = new RequiredTextField<>("url");
            urlField.add(EnrichedKnowledgeBaseUtils.URL_VALIDATOR);
            urlField.setVisibilityAllowed(!isEditingLocalRepository);
            add(urlField);
        }
    }
}
