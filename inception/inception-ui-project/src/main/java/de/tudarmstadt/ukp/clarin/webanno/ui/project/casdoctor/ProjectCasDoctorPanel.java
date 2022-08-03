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
package de.tudarmstadt.ukp.clarin.webanno.ui.project.casdoctor;

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.UNMANAGED_NON_INITIALIZING_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.INITIAL_CAS_PSEUDO_USER;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.AbstractChoice.LabelPosition;
import org.apache.wicket.markup.html.form.CheckBoxMultipleChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.diag.CasDoctor;
import de.tudarmstadt.ukp.clarin.webanno.diag.ChecksRegistry;
import de.tudarmstadt.ukp.clarin.webanno.diag.RepairsRegistry;
import de.tudarmstadt.ukp.clarin.webanno.diag.repairs.Repair.Safe;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.settings.ProjectSettingsPanelBase;

public class ProjectCasDoctorPanel
    extends ProjectSettingsPanelBase
{
    private final static Logger LOG = LoggerFactory.getLogger(ProjectCasDoctorPanel.class);

    private static final long serialVersionUID = 2116717853865353733L;

    private @SpringBean DocumentService documentService;
    private @SpringBean CasStorageService casStorageService;
    private @SpringBean DocumentImportExportService importExportService;
    private @SpringBean RepairsRegistry repairsRegistry;
    private @SpringBean ChecksRegistry checksRegistry;

    // Data properties
    private FormModel formModel = new FormModel();

    public ProjectCasDoctorPanel(String id, IModel<Project> aProjectModel)
    {
        super(id, aProjectModel);

        setOutputMarkupId(true);

        Form<FormModel> form = new Form<>("casDoctorForm", PropertyModel.of(this, "formModel"));
        add(form);

        CheckBoxMultipleChoice<String> repairs = new CheckBoxMultipleChoice<>("repairs");
        repairs.setModel(PropertyModel.of(this, "formModel.repairs"));
        repairs.setChoices(repairsRegistry.getExtensions().stream() //
                .map(r -> r.getId()).collect(toList()));
        repairs.setPrefix("<div class=\"checkbox\">");
        repairs.setSuffix("</div>");
        repairs.setLabelPosition(LabelPosition.WRAP_AFTER);
        form.add(repairs);

        form.add(new LambdaAjaxButton<FormModel>("check", this::actionCheck));
        form.add(new LambdaAjaxButton<FormModel>("repair", this::actionRepair));
        add(createMessageSetsView());
    }

    @Override
    protected void onModelChanged()
    {
        super.onModelChanged();
        formModel = new FormModel();
    }

    private ListView<LogMessageSet> createMessageSetsView()
    {
        return new ListView<LogMessageSet>("messageSets",
                PropertyModel.of(this, "formModel.messageSets"))
        {
            private static final long serialVersionUID = 8957632000765128508L;

            @Override
            protected void populateItem(ListItem<LogMessageSet> aItem)
            {
                IModel<LogMessageSet> set = aItem.getModel();
                aItem.add(new Label("name", PropertyModel.of(set, "name")));
                aItem.add(createMessagesView(set));
            }
        };
    }

    private ListView<LogMessage> createMessagesView(IModel<LogMessageSet> aModel)
    {
        return new ListView<LogMessage>("messages", PropertyModel.of(aModel, "messages"))
        {
            private static final long serialVersionUID = 8957632000765128508L;

            @Override
            protected void populateItem(ListItem<LogMessage> aItem)
            {
                IModel<LogMessage> msg = aItem.getModel();
                aItem.add(new Label("level", PropertyModel.of(msg, "level")));
                aItem.add(new Label("source", PropertyModel.of(msg, "source")));
                aItem.add(new Label("message", PropertyModel.of(msg, "message")));
            }
        };
    }

    private void actionRepair(AjaxRequestTarget aTarget, Form<?> aForm)
        throws IOException, UIMAException, ClassNotFoundException
    {
        CasDoctor casDoctor = new CasDoctor(checksRegistry, repairsRegistry);
        casDoctor.setFatalChecks(false);
        casDoctor.setActiveRepairs(formModel.repairs.toArray(String[]::new));

        Project project = getModelObject();

        formModel.messageSets = new ArrayList<>();

        for (SourceDocument sd : documentService.listSourceDocuments(project)) {
            // Repair INITIAL CAS
            {
                LogMessageSet messageSet = new LogMessageSet(sd.getName() + " [INITIAL]");

                try {
                    casStorageService.forceActionOnCas(sd, INITIAL_CAS_PSEUDO_USER,
                            (doc, user) -> createOrReadInitialCasWithoutSaving(doc, messageSet),
                            (cas) -> casDoctor.repair(project, cas, messageSet.messages), //
                            true);
                }
                catch (Exception e) {
                    messageSet.messages.add(
                            LogMessage.error(getClass(), "Error repairing initial CAS for [%s]: %s",
                                    sd.getName(), e.getMessage()));
                    LOG.error("Error repairing initial CAS for [{}]", sd.getName(), e);
                }

                noticeIfThereAreNoMessages(messageSet);
                formModel.messageSets.add(messageSet);
            }

            // Repair CURATION_USER CAS
            {
                LogMessageSet messageSet = new LogMessageSet(
                        sd.getName() + " [" + CURATION_USER + "]");
                try {
                    casStorageService.forceActionOnCas(sd, CURATION_USER,
                            (doc, user) -> casStorageService.readCas(doc, user,
                                    UNMANAGED_NON_INITIALIZING_ACCESS),
                            (cas) -> casDoctor.repair(project, cas, messageSet.messages), //
                            true);
                }
                catch (FileNotFoundException e) {
                    if (asList(CURATION_IN_PROGRESS, CURATION_FINISHED).contains(sd.getState())) {
                        messageSet.messages
                                .add(LogMessage.error(getClass(), "Curation CAS missing."));
                    }
                    else {
                        // If there is no CAS for the curation user, then curation has not started
                        // yet. This is not a problem, so we can ignore it.
                        messageSet.messages
                                .add(LogMessage.info(getClass(), "Curation has not started."));
                    }
                }
                catch (Exception e) {
                    messageSet.messages.add(LogMessage.error(getClass(),
                            "Error checking annotations for [%s] for [%s]: %s", CURATION_USER,
                            sd.getName(), e.getMessage()));
                    LOG.error("Error checking annotations for [{}] for [{}]", CURATION_USER,
                            sd.getName(), e);
                }

                noticeIfThereAreNoMessages(messageSet);
                formModel.messageSets.add(messageSet);
            }

            // Repair regular annotator CASes
            for (AnnotationDocument ad : documentService.listAnnotationDocuments(sd)) {
                if (documentService.existsCas(ad)) {
                    LogMessageSet messageSet = new LogMessageSet(
                            sd.getName() + " [" + ad.getUser() + "]");
                    try {
                        casStorageService.forceActionOnCas(sd, ad.getUser(),
                                (doc, user) -> casStorageService.readCas(doc, user,
                                        UNMANAGED_NON_INITIALIZING_ACCESS),
                                (cas) -> casDoctor.repair(project, cas, messageSet.messages), //
                                true);
                    }
                    catch (Exception e) {
                        messageSet.messages.add(LogMessage.error(getClass(),
                                "Error repairing annotations of user [%s] for [%s]: %s",
                                ad.getUser(), sd.getName(), e.getMessage()));
                        LOG.error("Error repairing annotations of user [{}] for [{}]", ad.getUser(),
                                sd.getName(), e);
                    }
                    noticeIfThereAreNoMessages(messageSet);
                    formModel.messageSets.add(messageSet);
                }
            }
        }

        aTarget.add(this);
    }

    private void actionCheck(AjaxRequestTarget aTarget, Form<?> aForm)
        throws IOException, UIMAException, ClassNotFoundException
    {
        CasDoctor casDoctor = new CasDoctor(checksRegistry, repairsRegistry);
        casDoctor.setActiveChecks(
                checksRegistry.getExtensions().stream().map(c -> c.getId()).toArray(String[]::new));

        Project project = getModelObject();

        formModel.messageSets = new ArrayList<>();

        int objectCount = 0;
        for (SourceDocument sd : documentService.listSourceDocuments(project)) {
            // Check INITIAL CAS
            {
                LogMessageSet messageSet = new LogMessageSet(sd.getName() + " [INITIAL]");

                try {
                    objectCount++;
                    casStorageService.forceActionOnCas(sd, INITIAL_CAS_PSEUDO_USER,
                            (doc, user) -> createOrReadInitialCasWithoutSaving(doc, messageSet),
                            (cas) -> casDoctor.analyze(project, cas, messageSet.messages), //
                            false);
                }
                catch (Exception e) {
                    messageSet.messages.add(
                            LogMessage.error(getClass(), "Error checking initial CAS for [%s]: %s",
                                    sd.getName(), e.getMessage()));
                    LOG.error("Error checking initial CAS for [{}]", sd.getName(), e);
                }

                noticeIfThereAreNoMessages(messageSet);
                formModel.messageSets.add(messageSet);
            }

            // Check CURATION_USER CAS
            {
                LogMessageSet messageSet = new LogMessageSet(
                        sd.getName() + " [" + CURATION_USER + "]");
                try {
                    objectCount++;
                    casStorageService.forceActionOnCas(sd, CURATION_USER,
                            (doc, user) -> casStorageService.readCas(doc, user,
                                    UNMANAGED_NON_INITIALIZING_ACCESS),
                            (cas) -> casDoctor.analyze(project, cas, messageSet.messages), //
                            false);
                }
                catch (FileNotFoundException e) {
                    // If there is no CAS for the curation user, then curation has not started yet.
                    // This is not a problem, so we can ignore it.
                    messageSet.messages.add(
                            LogMessage.info(getClass(), "Curation seems to have not yet started."));
                }
                catch (Exception e) {
                    messageSet.messages.add(LogMessage.error(getClass(),
                            "Error checking annotations for [%s] for [%s]: %s", CURATION_USER,
                            sd.getName(), e.getMessage()));
                    LOG.error("Error checking annotations for [{}] for [{}]", CURATION_USER,
                            sd.getName(), e);
                }

                noticeIfThereAreNoMessages(messageSet);
                formModel.messageSets.add(messageSet);
            }

            // Check regular annotator CASes
            for (AnnotationDocument ad : documentService.listAnnotationDocuments(sd)) {
                if (documentService.existsCas(ad)) {
                    LogMessageSet messageSet = new LogMessageSet(
                            sd.getName() + " [" + ad.getUser() + "]");
                    try {
                        objectCount++;
                        casStorageService.forceActionOnCas(ad.getDocument(), ad.getUser(),
                                (doc, user) -> casStorageService.readCas(doc, user,
                                        UNMANAGED_NON_INITIALIZING_ACCESS),
                                (cas) -> casDoctor.analyze(project, cas, messageSet.messages), //
                                false);
                    }
                    catch (Exception e) {
                        messageSet.messages.add(LogMessage.error(getClass(),
                                "Error checking annotations of user [%s] for [%s]: %s",
                                ad.getUser(), sd.getName(), e.getMessage()));
                        LOG.error("Error checking annotations of user [{}] for [{}]", ad.getUser(),
                                sd.getName(), e);
                    }

                    noticeIfThereAreNoMessages(messageSet);
                    formModel.messageSets.add(messageSet);
                }
            }
        }

        if (objectCount > 0) {
            info("Applied " + casDoctor.getActiveChecks().size() + " checks to " + objectCount
                    + " annotation objects - see report for details");
        }
        else {
            warn("Project does not contain any annotation objects that can be checked");
        }
        aTarget.addChildren(getPage(), IFeedback.class);

        aTarget.add(this);
    }

    private CAS createOrReadInitialCasWithoutSaving(SourceDocument aDocument,
            LogMessageSet aMessageSet)
        throws IOException, UIMAException
    {
        if (casStorageService.existsCas(aDocument, INITIAL_CAS_PSEUDO_USER)) {
            return casStorageService.readCas(aDocument, INITIAL_CAS_PSEUDO_USER,
                    UNMANAGED_NON_INITIALIZING_ACCESS);
        }

        CAS cas = importExportService
                .importCasFromFile(documentService.getSourceDocumentFile(aDocument), aDocument);
        aMessageSet.messages.add(
                LogMessage.info(getClass(), "Created initial CAS for [%s]", aDocument.getName()));
        return cas;
    }

    private void noticeIfThereAreNoMessages(LogMessageSet aSet)
    {
        if (aSet.messages.isEmpty()) {
            aSet.messages.add(LogMessage.info(getClass(), "Nothing to report."));
        }
    }

    private class FormModel
        implements Serializable
    {
        private static final long serialVersionUID = 5421427363671176637L;

        private List<LogMessageSet> messageSets = new ArrayList<>();
        private List<String> repairs;

        {
            // Fetch only the safe/non-destructive repairs
            repairs = repairsRegistry.getExtensions().stream() //
                    .filter(r -> {
                        Safe s = r.getClass().getAnnotation(Safe.class);
                        return s != null && s.value();
                    }) //
                    .map(r -> r.getId()) //
                    .collect(toList());
        }
    }

    @SuppressWarnings("unused")
    private static class LogMessageSet
        implements Serializable
    {
        private static final long serialVersionUID = 997324549494420840L;

        private String name;
        private List<LogMessage> messages = new ArrayList<>();

        public LogMessageSet(String aName)
        {
            name = aName;
        }
    }
}
