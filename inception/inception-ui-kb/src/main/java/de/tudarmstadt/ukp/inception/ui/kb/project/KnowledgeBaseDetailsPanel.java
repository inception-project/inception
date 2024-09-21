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
package de.tudarmstadt.ukp.inception.ui.kb.project;

import static de.tudarmstadt.ukp.inception.kb.RepositoryType.LOCAL;
import static de.tudarmstadt.ukp.inception.kb.RepositoryType.REMOTE;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

import java.io.FileInputStream;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.config.RepositoryImplConfig;
import org.eclipse.rdf4j.repository.sparql.config.SPARQLRepositoryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.bootstrap.BootstrapModalDialog;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBaseProperties;
import de.tudarmstadt.ukp.inception.kb.event.KnowledgeBaseConfigurationChangedEvent;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.spring.ApplicationEventPublisherHolder;

public class KnowledgeBaseDetailsPanel
    extends Panel
{
    private static final long serialVersionUID = -3550082954966752196L;

    private static final Logger LOG = LoggerFactory.getLogger(KnowledgeBaseDetailsPanel.class);

    private static final String CID_FORM = "form";
    private static final String CID_SAVE = "save";
    private static final String CID_REINDEX = "reindex";
    private static final String CID_DELETE = "delete";
    private static final String CID_TITLE = "title";
    private static final String CID_CONTENT = "content";

    private @SpringBean ApplicationEventPublisherHolder applicationEventPublisherHolder;
    private @SpringBean KnowledgeBaseService kbService;
    private @SpringBean KnowledgeBaseProperties kbProperties;

    private final IModel<KnowledgeBase> kbModel;
    private final CompoundPropertyModel<KnowledgeBaseWrapper> kbwModel;

    private final KBSettingsContent content;
    private final BootstrapModalDialog confirmationDialog;

    public KnowledgeBaseDetailsPanel(String aId, IModel<KnowledgeBase> aKbModel)
    {
        super(aId, null);
        setOutputMarkupPlaceholderTag(true);

        kbModel = aKbModel;

        var kbw = new KnowledgeBaseWrapper(kbModel.getObject());

        // set the URL of the KBW to the current SPARQL URL, if dealing with a remote repository
        if (kbw.getKb().getType() == REMOTE) {
            var cfg = kbService.getKnowledgeBaseConfig(kbw.getKb());
            if (cfg != null) {
                String url = ((SPARQLRepositoryConfig) cfg).getQueryEndpointUrl();
                kbw.setUrl(url);
            }
            else {
                kbw.setUrl(null);
            }
        }

        // wrap the given knowledge base model, then set it as the default model
        kbwModel = new CompoundPropertyModel<>(Model.of(kbw));
        setDefaultModel(kbwModel);

        // this form contains all the wicket components in this panel; not only the components used
        // for editing, but also the ones for showing information about a KB (when in ViewMode)
        queue(new Form<KnowledgeBaseWrapper>(CID_FORM, kbwModel));

        // title/content
        queue(new KBSettingsTitle(CID_TITLE, kbwModel));
        queue(content = new KBSettingsContent(CID_CONTENT, kbwModel));

        // re-index button only visible for local KBs
        queue(new LambdaAjaxLink(CID_DELETE, this::actionDelete));
        queue(new LambdaAjaxLink(CID_REINDEX, this::actionReindex) //
                .add(visibleWhen(kbwModel.map($ -> $.getKb().getType() == LOCAL).orElse(false))));
        queue(new LambdaAjaxButton<>(CID_SAVE, this::actionSave).triggerAfterSubmit());

        confirmationDialog = new BootstrapModalDialog("confirmationDialog");
        confirmationDialog.trapFocus();
        queue(confirmationDialog);
    }

    @Override
    protected void onConfigure()
    {
        super.onConfigure();

        setVisible(kbModel != null && kbModel.getObject() != null);
    }

    @Override
    protected void onModelChanged()
    {
        // propagate the changes to the original knowledge base model
        kbModel.setObject(kbwModel.getObject().getKb());

        // Forget any in-transit uploaded files
        kbwModel.getObject().clearFiles();
    }

    private void actionSave(AjaxRequestTarget aTarget, Form<KnowledgeBaseWrapper> aForm)
    {
        aTarget.addChildren(getPage(), IFeedback.class);
        aTarget.add(this);

        try {
            var kbw = kbwModel.getObject();

            // if dealing with a remote repository and a non-empty URL, get a new
            // RepositoryImplConfig for the new URL; otherwise keep using the existing config
            RepositoryImplConfig cfg;
            if (kbw.getKb().getType() == REMOTE && kbw.getUrl() != null) {
                cfg = kbService.getRemoteConfig(kbw.getUrl());
            }
            else {
                cfg = kbService.getKnowledgeBaseConfig(kbw.getKb());
            }

            content.applyState();

            var kb = kbw.getKb();
            kb.setTraits(JSONUtil.toJsonString(kbw.getTraits()));
            kbService.updateKnowledgeBase(kb, cfg);

            if (kb.getType() == LOCAL) {
                kbService.defineBaseProperties(kb);
                for (var f : kbw.getFiles()) {
                    try (var is = new FileInputStream(f.getValue())) {
                        kbService.importData(kb, f.getValue().getName(), is);
                        success("Imported: " + f.getKey());
                    }
                    catch (Exception e) {
                        error("Failed to import [" + f.getKey() + "]: " + getRootCauseMessage(e));
                        LOG.error("Failed to import [{}]: ", f.getKey(), e);
                    }
                }
            }

            modelChanged();

            success("Knowledge base settings saved.");
            applicationEventPublisherHolder.get().publishEvent(
                    new KnowledgeBaseConfigurationChangedEvent(this, kbw.getKb().getProject()));
        }
        catch (Exception e) {
            error("Unable to save knowledge base: " + e.getLocalizedMessage());
            LOG.error("Unable to save knowledge base.", e);
        }
    }

    private void actionReindex(AjaxRequestTarget aTarget)
    {
        aTarget.addChildren(getPage(), IFeedback.class);

        var kb = kbwModel.getObject().getKb();
        try {
            LOG.info("Starting rebuilding full-text index of {} ... this may take a while ...", kb);
            kbService.rebuildFullTextIndex(kb);
            LOG.info("Completed rebuilding full-text index of {}", kb);
            success("Completed rebuilding full-text index");
        }
        catch (Exception e) {
            error("Unable to rebuild full text index: " + e.getLocalizedMessage());
            LOG.error("Unable to rebuild full text index for KB [{}]({}) in project [{}]({})",
                    kb.getName(), kb.getRepositoryId(), kb.getProject().getName(),
                    kb.getProject().getId(), e);
        }
    }

    private void actionDelete(AjaxRequestTarget aTarget)
    {
        var dialogContent = new DeleteKnowledgeBaseConfirmationDialogPanel(
                BootstrapModalDialog.CONTENT_ID, kbModel);

        dialogContent.setConfirmAction(_target -> {
            KnowledgeBase kb = kbwModel.getObject().getKb();
            try {
                kbService.removeKnowledgeBase(kb);
                kbwModel.getObject().setKb(null);
                modelChanged();
            }
            catch (RepositoryException | RepositoryConfigException e) {
                error("Unable to remove knowledge base: " + e.getLocalizedMessage());
                LOG.error("Unable to remove knowledge base.", e);
                _target.addChildren(getPage(), IFeedback.class);
            }
            _target.add(this);
            _target.add(findParentWithAssociatedMarkup());
        });

        confirmationDialog.open(dialogContent, aTarget);
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

        private final CompoundPropertyModel<KnowledgeBaseWrapper> localKbwModel;
        private final AccessSpecificSettingsPanel accessSpecificSettings;

        public KBSettingsContent(String id, CompoundPropertyModel<KnowledgeBaseWrapper> aKbwModel)
        {
            super(id, "kbSettingsContent", KnowledgeBaseDetailsPanel.this, aKbwModel);

            localKbwModel = aKbwModel;

            var generalSettings = new GeneralSettingsPanel("generalSettings",
                    kbModel.map(KnowledgeBase::getProject), localKbwModel);
            generalSettings.get("name").setVisible(false);
            add(generalSettings);

            var accessSettings = new AccessSettingsPanel("accessSettings", localKbwModel);
            accessSettings.get("type").setEnabled(false);
            accessSettings.get("writeprotection")
                    .setEnabled(localKbwModel.getObject().getKb().getType() == LOCAL);
            add(accessSettings);

            accessSpecificSettings = new AccessSpecificSettingsPanel("accessSpecificSettings",
                    localKbwModel, emptyMap());
            add(accessSpecificSettings);

            add(new QuerySettingsPanel("querySettings", localKbwModel));

            add(new KnowledgeBaseIriPanel("schemaMapping", localKbwModel));

            add(new RootConceptsPanel("rootConcepts", localKbwModel));

            add(new AdditionalMatchingPropertiesPanel("additionalMatchingProperties",
                    localKbwModel));
        }

        public void applyState()
        {
            accessSpecificSettings.applyState();
        }
    }
}
