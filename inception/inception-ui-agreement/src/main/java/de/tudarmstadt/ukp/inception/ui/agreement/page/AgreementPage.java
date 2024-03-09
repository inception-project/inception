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
package de.tudarmstadt.ukp.inception.ui.agreement.page;

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.SHARED_READ_ONLY_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.AUTO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.NS_PROJECT;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.PAGE_PARAM_PROJECT;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.enabledWhen;
import static de.tudarmstadt.ukp.inception.support.logging.Logging.KEY_PROJECT_ID;
import static de.tudarmstadt.ukp.inception.support.logging.Logging.KEY_REPOSITORY_PATH;
import static de.tudarmstadt.ukp.inception.support.logging.Logging.KEY_USERNAME;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.groupingBy;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.util.FSUtil;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.wicketstuff.annotation.mount.MountPath;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.agreement.AgreementUtils;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasure;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasureSupport;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.DefaultAgreementTraits;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.coding.CodingAgreementMeasure_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.coding.FullCodingAgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.coding.event.PairwiseAgreementScoreClickedEvent;
import de.tudarmstadt.ukp.clarin.webanno.agreement.task.CalculatePairwiseAgreementTask;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.storage.CasStorageSession;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.scheduling.TaskScope;
import de.tudarmstadt.ukp.inception.scheduling.TaskState;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.WebAnnoConst;
import de.tudarmstadt.ukp.inception.support.help.DocLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaChoiceRenderer;
import de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil;
import de.tudarmstadt.ukp.inception.support.wicket.AjaxDownloadBehavior;
import de.tudarmstadt.ukp.inception.support.wicket.PipedStreamResource;

@MountPath(NS_PROJECT + "/${" + PAGE_PARAM_PROJECT + "}/agreement")
public class AgreementPage
    extends ProjectPageBase
{
    private static final long serialVersionUID = 5333662917247971912L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String MID_TRAITS_CONTAINER = "traitsContainer";
    private static final String MID_TRAITS = "traits";
    private static final String MID_RESULTS = "results";

    private @SpringBean DocumentService documentService;
    private @SpringBean ProjectService projectService;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean UserDao userRepository;
    private @SpringBean AgreementMeasureSupportRegistry agreementRegistry;
    private @SpringBean SchedulingService schedulingService;
    private @SpringBean RepositoryProperties repositoryProperties;

    private AgreementForm agreementForm;
    private WebMarkupContainer resultsContainer;
    private LambdaAjaxBehavior refreshResultsBehavior;
    private AjaxDownloadBehavior downloadBehavior;

    public AgreementPage(final PageParameters aPageParameters)
    {
        super(aPageParameters);
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();

        var user = userRepository.getCurrentUser();

        requireProjectRole(user, MANAGER, CURATOR);

        add(agreementForm = new AgreementForm("agreementForm", Model.of(new AgreementFormModel())));

        add(resultsContainer = new WebMarkupContainer("resultsContainer"));
        resultsContainer.setOutputMarkupPlaceholderTag(true);
        resultsContainer.add(new EmptyPanel(MID_RESULTS));

        downloadBehavior = new AjaxDownloadBehavior();
        add(downloadBehavior);

        // add(new TaskMonitorPanel("runningProcesses") //
        // .setPopupMode(false) //
        // .setTypePattern(CalculatePairwiseAgreementTask.TYPE) //
        // .setKeepRemovedTasks(true));
        //
        // refreshResultsBehavior = new LambdaAjaxBehavior(this::actionRefreshResults);
        // add(refreshResultsBehavior);
    }

    private CalculatePairwiseAgreementTask getCurrentTask()
    {
        var maybeTask = schedulingService.findTask(t -> t instanceof CalculatePairwiseAgreementTask
                && t.getUser().map(userRepository.getCurationUser()::equals).orElse(false)
                && getProject().equals(t.getProject()));

        if (maybeTask.isEmpty()) {
            return null;
        }

        return (CalculatePairwiseAgreementTask) maybeTask.get();

    }

    private void actionRefreshResults(AjaxRequestTarget aTarget,
            CalculatePairwiseAgreementTask aTask)
    {
        // var task = getCurrentTask();
        var task = aTask;
        if (task != null && task.getMonitor().getState() == TaskState.COMPLETED) {
            AgreementMeasureSupport<?, ?, ?> ams = agreementRegistry.getAgreementMeasureSupport(
                    agreementForm.measureDropDown.getModelObject().getKey());

            var result = task.getResult();
            var resultsPanel = ams.createResultsPanel(MID_RESULTS, Model.of(result));
            resultsContainer.addOrReplace(resultsPanel);
            aTarget.add(resultsContainer);
        }
    }

    private class AgreementForm
        extends Form<AgreementFormModel>
    {
        private static final long serialVersionUID = -1L;

        private final DropDownChoice<AnnotationFeature> featureList;

        private final DropDownChoice<Pair<String, String>> measureDropDown;

        private final LambdaAjaxButton<Void> runCalculationsButton;

        private final WebMarkupContainer traitsContainer;

        public AgreementForm(String id, IModel<AgreementFormModel> aModel)
        {
            super(id, CompoundPropertyModel.of(aModel));

            setOutputMarkupPlaceholderTag(true);

            add(new DocLink("agreementHelpLink", "sect_monitoring_agreement"));

            add(traitsContainer = new WebMarkupContainer(MID_TRAITS_CONTAINER));
            traitsContainer.setOutputMarkupPlaceholderTag(true);
            traitsContainer.add(new EmptyPanel(MID_TRAITS));

            add(new Label("name", getProject().getName()));

            add(featureList = new DropDownChoice<>("feature"));
            featureList.setOutputMarkupId(true);
            featureList.setChoices(LoadableDetachableModel.of(this::getEligibleFeatures));
            featureList.setChoiceRenderer(new LambdaChoiceRenderer<AnnotationFeature>(
                    feature -> feature.getLayer().getUiName() + " : " + feature.getUiName()));
            featureList.add(new LambdaAjaxFormComponentUpdatingBehavior("change",
                    this::actionSelectFeature));

            runCalculationsButton = new LambdaAjaxButton<>("run", this::actionRunCalculations);
            runCalculationsButton.triggerAfterSubmit();
            add(runCalculationsButton);

            add(measureDropDown = new DropDownChoice<Pair<String, String>>("measure",
                    this::listMeasures)
            {
                private static final long serialVersionUID = -2666048788050249581L;

                @SuppressWarnings({ "rawtypes", "unchecked" })
                @Override
                protected void onModelChanged()
                {
                    super.onModelChanged();

                    // If the feature type has changed, we need to set up a new traits
                    // editor
                    Component newTraits;
                    if (getModelObject() != null) {
                        AgreementMeasureSupport ams = agreementRegistry
                                .getAgreementMeasureSupport(getModelObject().getKey());
                        newTraits = ams.createTraitsEditor(MID_TRAITS, featureList.getModel(),
                                Model.of((DefaultAgreementTraits) ams.createTraits()));
                    }
                    else {
                        newTraits = new EmptyPanel(MID_TRAITS);
                    }

                    traitsContainer.addOrReplace(newTraits);
                }
            });
            measureDropDown.setChoiceRenderer(new ChoiceRenderer<>("value"));
            measureDropDown.add(new LambdaAjaxFormComponentUpdatingBehavior("change",
                    _target -> _target.add(runCalculationsButton, traitsContainer)));

            runCalculationsButton.add(enabledWhen(() -> measureDropDown.getModelObject() != null));
        }

        private void actionSelectFeature(AjaxRequestTarget aTarget)
        {
            // If the currently selected measure is not compatible with the selected feature, then
            // we clear the measure selection.
            AnnotationFeature selectedFeature = featureList.getModelObject();
            boolean measureCompatibleWithFeature = measureDropDown.getModel() //
                    .map(k -> agreementRegistry.getAgreementMeasureSupport(k.getKey())) //
                    .map(s -> selectedFeature != null && s.accepts(selectedFeature)) //
                    .orElse(false) //
                    .getObject();
            if (!measureCompatibleWithFeature) {
                measureDropDown.setModelObject(null);
            }

            aTarget.add(measureDropDown, runCalculationsButton, traitsContainer);
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        private void actionRunCalculations(AjaxRequestTarget aTarget, Form<?> aForm)
        {
            var feature = featureList.getModelObject();
            var measureHandle = measureDropDown.getModelObject();

            // Do not do any agreement if no feature or measure has been selected yet.
            if (feature == null || measureHandle == null) {
                return;
            }

            var traits = (DefaultAgreementTraits) agreementForm.traitsContainer.get(MID_TRAITS)
                    .getDefaultModelObject();

            AgreementMeasureSupport ams = agreementRegistry
                    .getAgreementMeasureSupport(measureDropDown.getModelObject().getKey());

            var measure = ams.createMeasure(feature, traits);

            var states = new ArrayList<AnnotationDocumentState>();
            states.add(AnnotationDocumentState.FINISHED);
            if (!traits.isLimitToFinishedDocuments()) {
                states.add(AnnotationDocumentState.IN_PROGRESS);
            }

            var allAnnDocs = documentService.listAnnotationDocumentsInState(getProject(), //
                    states.toArray(AnnotationDocumentState[]::new)).stream() //
                    .collect(groupingBy(AnnotationDocument::getDocument));

            if (allAnnDocs.isEmpty()) {
                error("No documents with annotations were found.");
                aTarget.addChildren(getPage(), IFeedback.class);
                return;
            }

            var aAnnotators = projectService.listProjectUsersWithPermissions(getProject(),
                    ANNOTATOR);

            var task = CalculatePairwiseAgreementTask.builder() //
                    .withSessionOwner(userRepository.getCurrentUser()) //
                    .withProject(getProject()) //
                    .withTrigger("Agreement page") //
                    .withAnnotators(aAnnotators) //
                    .withTraits(traits) //
                    .withFeature(feature) //
                    .withMeasure(measure) //
                    .withDocuments(allAnnDocs) //
                    .withScope(TaskScope.LAST_USER_SESSION) //
                    // When running sync, we cannot cancel because the browser will still be in an
                    // AJAX request when we try to fire a second one and that second one will fail
                    // then. This would only work if the cancel action would be sent through
                    // WebSocket
                    .withCancellable(false).build();

            schedulingService.executeSync(task);
            actionRefreshResults(aTarget, task);
        }

        List<Pair<String, String>> listMeasures()
        {
            if (getModelObject().feature == null) {
                return Collections.emptyList();
            }

            return agreementRegistry.getAgreementMeasureSupports(getModelObject().feature).stream()
                    .map(s -> Pair.of(s.getId(), s.getName())) //
                    .toList();
        }

        private List<AnnotationFeature> getEligibleFeatures()
        {
            var features = annotationService.listAnnotationFeature(getProject());
            var unusedFeatures = new ArrayList<AnnotationFeature>();
            for (var feature : features) {
                if (feature.getLayer().getName().equals(Token.class.getName())
                        || feature.getLayer().getName().equals(WebAnnoConst.COREFERENCE_LAYER)) {
                    unusedFeatures.add(feature);
                }
            }
            features.removeAll(unusedFeatures);
            return features;
        }
    }

    @OnEvent
    public void onPairwiseAgreementScoreClicked(PairwiseAgreementScoreClickedEvent aEvent)
    {
        // Copy the relevant information from the event to avoid having to pass the event into the
        // lambda which would cause problems here since the event is not serializable
        var annotator1 = aEvent.getAnnotator1();
        var annotator2 = aEvent.getAnnotator2();
        var currentUser = userRepository.getCurrentUser();

        var measure = createSelectedMeasure(getTraits());
        if (!(measure instanceof CodingAgreementMeasure_ImplBase)) {
            aEvent.getTarget().addChildren(getPage(), IFeedback.class);
            warn("Aggreement report currently only supported for coding measures.");
            return;
        }

        downloadBehavior.initiate(aEvent.getTarget(), "agreement.csv",
                new PipedStreamResource((os) -> {
                    // PipedStreamResource runs the lambda in a separate thread, so we need to make
                    // sure the MDC is correctly set up here.
                    try (var ctx = new DefaultMdcSetup(repositoryProperties, getProject(),
                            currentUser)) {
                        exportAgreement(os, annotator1, annotator2, currentUser);
                    }
                }));
    }

    private void exportAgreement(OutputStream aOut, String aAnnotator1, String aAnnotator2,
            User aCurrentUser)
    {
        var traits = getTraits();
        var measure = createSelectedMeasure(traits);

        var states = new ArrayList<AnnotationDocumentState>();
        states.add(AnnotationDocumentState.FINISHED);
        if (!traits.isLimitToFinishedDocuments()) {
            states.add(AnnotationDocumentState.IN_PROGRESS);
        }

        var allAnnDocs = documentService.listAnnotationDocumentsInState(getProject(), //
                states.toArray(AnnotationDocumentState[]::new)).stream() //
                .collect(groupingBy(AnnotationDocument::getDocument));

        var docs = allAnnDocs.keySet().stream() //
                .sorted(comparing(SourceDocument::getName)) //
                .toList();
        var countWritten = 0;
        for (var doc : docs) {
            try (var session = CasStorageSession.openNested()) {
                var maybeCas1 = loadCas(doc, aAnnotator1, allAnnDocs);
                var maybeCas2 = loadCas(doc, aAnnotator1, allAnnDocs);
                var cas1 = maybeCas1.isPresent() ? maybeCas1.get() : loadInitialCas(doc);
                var cas2 = maybeCas2.isPresent() ? maybeCas2.get() : loadInitialCas(doc);

                var casMap = new LinkedHashMap<String, CAS>();
                casMap.put(aAnnotator1, cas1);
                casMap.put(aAnnotator2, cas2);
                var res = measure.getAgreement(casMap);
                AgreementUtils.generateCsvReport(aOut, (FullCodingAgreementResult) res,
                        countWritten == 0);
                countWritten++;
            }
            catch (Exception e) {
                LOG.error("Unable to load data", e);
            }
        }
    }

    private DefaultAgreementTraits getTraits()
    {
        return (DefaultAgreementTraits) agreementForm.traitsContainer.get(MID_TRAITS)
                .getDefaultModelObject();
    }

    private AgreementMeasure createSelectedMeasure(DefaultAgreementTraits traits)
    {
        AgreementMeasureSupport ams = agreementRegistry.getAgreementMeasureSupport(
                agreementForm.measureDropDown.getModelObject().getKey());

        var measure = ams.createMeasure(agreementForm.featureList.getModelObject(), traits);
        return measure;
    }

    private CAS loadInitialCas(SourceDocument aDocument) throws IOException
    {
        var cas = documentService.createOrReadInitialCas(aDocument, AUTO_CAS_UPGRADE,
                SHARED_READ_ONLY_ACCESS);

        // Set the CAS name in the DocumentMetaData so that we can pick it
        // up in the Diff position for the purpose of debugging / transparency.
        var dmd = WebAnnoCasUtil.getDocumentMetadata(cas);
        FSUtil.setFeature(dmd, "documentId", aDocument.getName());
        FSUtil.setFeature(dmd, "collectionId", aDocument.getProject().getName());

        return cas;
    }

    private Optional<CAS> loadCas(SourceDocument aDocument, String aDataOwner,
            Map<SourceDocument, List<AnnotationDocument>> aAllAnnDocs)
        throws IOException
    {
        var annDocs = aAllAnnDocs.get(aDocument);

        if (annDocs.stream().noneMatch(annDoc -> aDataOwner.equals(annDoc.getUser()))) {
            return Optional.empty();
        }

        if (!documentService.existsCas(aDocument, aDataOwner)) {
            Optional.empty();
        }

        var cas = documentService.readAnnotationCas(aDocument, aDataOwner, AUTO_CAS_UPGRADE,
                SHARED_READ_ONLY_ACCESS);

        // Set the CAS name in the DocumentMetaData so that we can pick it
        // up in the Diff position for the purpose of debugging / transparency.
        var dmd = WebAnnoCasUtil.getDocumentMetadata(cas);
        FSUtil.setFeature(dmd, "documentId", aDocument.getName());
        FSUtil.setFeature(dmd, "collectionId", aDocument.getProject().getName());

        return Optional.of(cas);
    }

    static class AgreementFormModel
        implements Serializable
    {
        private static final long serialVersionUID = -1L;

        AnnotationFeature feature;

        Pair<String, String> measure;
    }

    // FIXME: Would be good to move this somewhere else to make it properly reusable
    static class DefaultMdcSetup
        implements AutoCloseable
    {

        public DefaultMdcSetup(RepositoryProperties repositoryProperties, Project aProject,
                User aUser)
        {
            // We are in a new thread. Set up thread-specific MDC
            if (repositoryProperties != null) {
                MDC.put(KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());
            }

            if (aUser != null) {
                MDC.put(KEY_USERNAME, aUser.getUsername());
            }

            if (aProject != null) {
                MDC.put(KEY_PROJECT_ID, String.valueOf(aProject.getId()));
            }
        }

        @Override
        public void close()
        {
            MDC.remove(KEY_REPOSITORY_PATH);
            MDC.remove(KEY_USERNAME);
            MDC.remove(KEY_PROJECT_ID);
        }
    }

    // private final DropDownChoice<AgreementReportExportFormat> formatField;

    // add(formatField = new DropDownChoice<AgreementReportExportFormat>("exportFormat",
    // Model.of(CSV), asList(AgreementReportExportFormat.values()),
    // new EnumChoiceRenderer<>(this)));
    // formatField.add(new LambdaAjaxFormComponentUpdatingBehavior("change"));
    //

    // private Behavior makeDownloadBehavior(final String aKey1, final String aKey2)
    // {
    // return new AjaxEventBehavior("click")
    // {
    // private static final long serialVersionUID = 1L;
    //
    // @Override
    // protected void onEvent(AjaxRequestTarget aTarget)
    // {
    // var download = new AjaxDownloadBehavior(
    // LoadableDetachableModel.of(PairwiseCodingAgreementTable.this::getFilename),
    // LoadableDetachableModel.of(() -> getAgreementTableData(aKey1, aKey2)));
    // getComponent().add(download);
    // download.initiate(aTarget);
    // }
    // };
    // }
    //
    // private AbstractResourceStream getAgreementTableData(final String aKey1, final String aKey2)
    // {
    // return new AbstractResourceStream()
    // {
    // private static final long serialVersionUID = 1L;
    //
    // @Override
    // public InputStream getInputStream() throws ResourceStreamNotFoundException
    // {
    // try {
    // var result = PairwiseCodingAgreementTable.this.getModelObject().getStudy(aKey1,
    // aKey2);
    //
    // switch (formatField.getModelObject()) {
    // case CSV:
    // return AgreementUtils.generateCsvReport(result);
    // case DEBUG:
    // return generateDebugReport(result);
    // default:
    // throw new IllegalStateException(
    // "Unknown export format [" + formatField.getModelObject() + "]");
    // }
    // }
    // catch (Exception e) {
    // // FIXME Is there some better error handling here?
    // LOG.error("Unable to generate agreement report", e);
    // throw new ResourceStreamNotFoundException(e);
    // }
    // }
    //
    // @Override
    // public void close() throws IOException
    // {
    // // Nothing to do
    // }
    // };
    // }
    //
    // private String getFilename()
    // {
    // return "agreement" + formatField.getModelObject().getExtension();
    // }
    //
    // private InputStream generateDebugReport(FullCodingAgreementResult aResult)
    // {
    // var buf = new ByteArrayOutputStream();
    // AgreementUtils.dumpAgreementStudy(new PrintStream(buf), aResult);
    // return new ByteArrayInputStream(buf.toByteArray());
    // }
}
