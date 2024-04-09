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

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.NS_PROJECT;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.PAGE_PARAM_PROJECT;
import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CHANGE_EVENT;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.enabledWhen;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.LambdaChoiceRenderer;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.annotation.mount.MountPath;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.agreement.AgreementResult_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasureSupport;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.DefaultAgreementTraits;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.coding.event.PairwiseAgreementScoreClickedEvent;
import de.tudarmstadt.ukp.clarin.webanno.agreement.task.CalculatePairwiseAgreementTask;
import de.tudarmstadt.ukp.clarin.webanno.agreement.task.CalculatePerDocumentAgreementTask;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectUserPermissions;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.project.users.ProjectUserPermissionChoiceRenderer;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.ChainLayerSupport;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.scheduling.TaskScope;
import de.tudarmstadt.ukp.inception.scheduling.TaskState;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.help.DocLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.wicket.AjaxDownloadBehavior;
import de.tudarmstadt.ukp.inception.support.wicket.PipedStreamResource;
import de.tudarmstadt.ukp.inception.ui.core.config.DefaultMdcSetup;

@MountPath(NS_PROJECT + "/${" + PAGE_PARAM_PROJECT + "}/agreement")
public class AgreementPage
    extends ProjectPageBase
{
    private static final long serialVersionUID = 5333662917247971912L;

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
    private @SpringBean AgreementService agreementService;

    private Form<AgreementFormModel> form;
    private WebMarkupContainer resultsContainer;
    // private LambdaAjaxBehavior refreshResultsBehavior;
    private AjaxDownloadBehavior downloadBehavior;
    private DropDownChoice<AnnotationFeature> featureList;
    private DropDownChoice<Pair<String, String>> measureDropDown;
    private LambdaAjaxButton<AgreementFormModel> calculatePairwiseAgreementButton;
    private LambdaAjaxButton<AgreementFormModel> calculatePerDocumentAgreement;
    private LambdaAjaxButton<AgreementFormModel> exportAgreementButton;
    private WebMarkupContainer traitsContainer;

    public AgreementPage(final PageParameters aPageParameters)
    {
        super(aPageParameters);

        var sessionOwner = userRepository.getCurrentUser();

        requireProjectRole(sessionOwner, MANAGER, CURATOR);
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();

        setOutputMarkupPlaceholderTag(true);

        queue(new Label("name", getProject().getName()));

        queue(resultsContainer = new WebMarkupContainer("resultsContainer"));
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

        var formModel = CompoundPropertyModel.of(new AgreementFormModel());
        queue(form = new Form<>("agreementForm", formModel));

        queue(new DocLink("agreementHelpLink", "sect_monitoring_agreement"));

        queue(traitsContainer = new WebMarkupContainer(MID_TRAITS_CONTAINER));
        traitsContainer.setOutputMarkupPlaceholderTag(true);

        queue(new EmptyPanel(MID_TRAITS));

        queue(featureList = makeFeatureChoice("feature"));
        queue(measureDropDown = makeMeasuresDropdown("measure"));

        var annotatorList = new ListMultipleChoice<ProjectUserPermissions>("annotators");
        annotatorList.setChoiceRenderer(new ProjectUserPermissionChoiceRenderer());
        annotatorList.setChoices(listUsersWithPermissions());
        queue(annotatorList);

        var documentList = new ListMultipleChoice<SourceDocument>("documents");
        documentList.setChoiceRenderer(new ChoiceRenderer<>("name"));
        documentList.setChoices(listDocuments());
        queue(documentList);

        calculatePairwiseAgreementButton = new LambdaAjaxButton<>("calculatePairwiseAgreement",
                this::actionCalculatePairwiseAgreement);
        calculatePairwiseAgreementButton.triggerAfterSubmit();
        calculatePairwiseAgreementButton
                .add(enabledWhen(() -> measureDropDown.getModelObject() != null));
        queue(calculatePairwiseAgreementButton);

        calculatePerDocumentAgreement = new LambdaAjaxButton<>("calculatePerDocumentAgreement",
                this::actionCalculatePerDocumentAgreement);
        calculatePerDocumentAgreement.triggerAfterSubmit();
        calculatePerDocumentAgreement.add(enabledWhen(this::isMeasureSupportingMoreThanTwoRaters));
        queue(calculatePerDocumentAgreement);

        exportAgreementButton = new LambdaAjaxButton<>("export", this::actionExportDiff);
        exportAgreementButton.triggerAfterSubmit();
        exportAgreementButton.add(enabledWhen(() -> measureDropDown.getModelObject() != null));
        queue(exportAgreementButton);

    }

    private boolean isMeasureSupportingMoreThanTwoRaters()
    {
        var measure = measureDropDown.getModelObject();
        if (measure == null) {
            return false;
        }

        AgreementMeasureSupport ams = agreementRegistry
                .getAgreementMeasureSupport(measure.getKey());
        return ams.isSupportingMoreThanTwoRaters();
    }

    private List<ProjectUserPermissions> listUsersWithPermissions()
    {
        return projectService.listProjectUserPermissions(getProject()).stream() //
                .filter(p -> p.getRoles().contains(PermissionLevel.ANNOTATOR)) //
                .toList();
    }

    private List<SourceDocument> listDocuments()
    {
        return documentService.listSourceDocuments(getProject());
    }

    private DropDownChoice<AnnotationFeature> makeFeatureChoice(String aId)
    {
        var choice = new DropDownChoice<AnnotationFeature>(aId);
        choice.setOutputMarkupId(true);
        choice.setChoices(LoadableDetachableModel.of(this::getEligibleFeatures));
        choice.setChoiceRenderer(new LambdaChoiceRenderer<AnnotationFeature>(
                feature -> feature.getLayer().getUiName() + " : " + feature.getUiName()));
        choice.add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                this::actionSelectFeature));
        return choice;
    }

    private DropDownChoice<Pair<String, String>> makeMeasuresDropdown(String aId)
    {
        var dropdown = new DropDownChoice<Pair<String, String>>(aId, this::listMeasures)
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
        };
        dropdown.setChoiceRenderer(new ChoiceRenderer<>("value"));
        dropdown.add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                _target -> _target.add(calculatePairwiseAgreementButton,
                        calculatePerDocumentAgreement, exportAgreementButton, traitsContainer)));
        return dropdown;
    }

    // private CalculatePairwiseAgreementTask getCurrentTask()
    // {
    // var maybeTask = schedulingService.findTask(t -> t instanceof CalculatePairwiseAgreementTask
    // && t.getUser().map(userRepository.getCurationUser()::equals).orElse(false)
    // && getProject().equals(t.getProject()));
    //
    // if (maybeTask.isEmpty()) {
    // return null;
    // }
    //
    // return (CalculatePairwiseAgreementTask) maybeTask.get();
    //
    // }

    private void refreshResultsWhenFinished(AjaxRequestTarget aTarget,
            CalculatePairwiseAgreementTask aTask)
    {
        // var task = getCurrentTask();
        var task = aTask;

        if (task == null || task.getMonitor().getState() != TaskState.COMPLETED) {
            return;
        }

        refreshResults(aTarget, task.getResult());
    }

    private void refreshResults(AjaxRequestTarget aTarget, AgreementResult_ImplBase aResult)
    {
        var ams = agreementRegistry
                .getAgreementMeasureSupport(measureDropDown.getModelObject().getKey());
        var resultsPanel = ams.createResultsPanel(MID_RESULTS, Model.of(aResult));
        resultsContainer.addOrReplace(resultsPanel);
        aTarget.add(resultsContainer);
    }

    private void actionSelectFeature(AjaxRequestTarget aTarget)
    {
        // If the currently selected measure is not compatible with the selected feature, then
        // we clear the measure selection.
        var selectedFeature = featureList.getModelObject();
        var measureCompatibleWithFeature = measureDropDown.getModel() //
                .map(k -> agreementRegistry.getAgreementMeasureSupport(k.getKey())) //
                .map(s -> selectedFeature != null && s.accepts(selectedFeature)) //
                .orElse(false) //
                .getObject();

        if (!measureCompatibleWithFeature) {
            measureDropDown.setModelObject(null);
        }

        aTarget.add(measureDropDown, calculatePerDocumentAgreement,
                calculatePairwiseAgreementButton, traitsContainer);
    }

    private void actionExportDiff(AjaxRequestTarget aTarget, Form<AgreementFormModel> aForm)
    {
        var filename = getProject().getSlug() + "-diff.csv";
        downloadBehavior.initiate(aTarget, filename, new PipedStreamResource((os) -> {
            var sessionOwner = userRepository.getCurrentUser();
            var model = aForm.getModelObject();
            var project = getProject();
            var annotators = getAnnotators(aForm.getModelObject());
            var traits = getTraits();
            var documents = agreementService
                    .getDocumentsToEvaluate(project, model.documents, traits).keySet().stream()
                    .toList();

            // PipedStreamResource runs the lambda in a separate thread, so we need to make
            // sure the MDC is correctly set up here.
            try (var ctx = new DefaultMdcSetup(repositoryProperties, getProject(), sessionOwner)) {
                agreementService.exportDiff(os, model.feature, model.measure.getKey(), getTraits(),
                        sessionOwner, documents, annotators);
            }
        }));

    }

    private void actionCalculatePairwiseAgreement(AjaxRequestTarget aTarget,
            Form<AgreementFormModel> aForm)
    {
        var model = aForm.getModelObject();
        var project = getProject();

        // Do not do any agreement if no feature or measure has been selected yet.
        if (model.feature == null || model.measure == null) {
            return;
        }

        var traits = getTraits();

        var measure = agreementRegistry.getMeasure(model.feature, model.measure.getKey(), traits);

        var allAnnDocs = agreementService.getDocumentsToEvaluate(project, model.documents, traits);

        if (allAnnDocs.isEmpty()) {
            error("At least one document needs to be selected.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        var annotators = getAnnotators(model);

        if (annotators.size() < 2) {
            error("At least two annotators need to be selected.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        var task = CalculatePairwiseAgreementTask.builder() //
                .withSessionOwner(userRepository.getCurrentUser()) //
                .withProject(project) //
                .withTrigger("Agreement page") //
                .withAnnotators(annotators) //
                .withTraits(traits) //
                .withFeature(model.feature) //
                .withMeasure(measure) //
                .withDocuments(allAnnDocs) //
                .withScope(TaskScope.LAST_USER_SESSION) //
                // When running sync, we cannot cancel because the browser will still be in an
                // AJAX request when we try to fire a second one and that second one will fail
                // then. This would only work if the cancel action would be sent through
                // WebSocket
                .withCancellable(false) //
                .build();

        schedulingService.executeSync(task);

        refreshResults(aTarget, task.getResult());
    }

    private void actionCalculatePerDocumentAgreement(AjaxRequestTarget aTarget,
            Form<AgreementFormModel> aForm)
    {
        var model = aForm.getModelObject();
        var project = getProject();

        // Do not do any agreement if no feature or measure has been selected yet.
        if (model.feature == null || model.measure == null) {
            return;
        }

        var traits = getTraits();

        var measure = agreementRegistry.getMeasure(model.feature, model.measure.getKey(), traits);

        var allAnnDocs = agreementService.getDocumentsToEvaluate(project, model.documents, traits);

        if (allAnnDocs.isEmpty()) {
            error("At least one document needs to be selected.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        var annotators = getAnnotators(model);

        if (annotators.size() < 2) {
            error("At least two annotators need to be selected.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        var task = CalculatePerDocumentAgreementTask.builder() //
                .withSessionOwner(userRepository.getCurrentUser()) //
                .withProject(project) //
                .withTrigger("Agreement page") //
                .withAnnotators(annotators) //
                .withTraits(traits) //
                .withFeature(model.feature) //
                .withMeasure(measure) //
                .withDocuments(allAnnDocs) //
                .withScope(TaskScope.LAST_USER_SESSION) //
                // When running sync, we cannot cancel because the browser will still be in an
                // AJAX request when we try to fire a second one and that second one will fail
                // then. This would only work if the cancel action would be sent through
                // WebSocket
                .withCancellable(false) //
                .build();

        schedulingService.executeSync(task);

        refreshResults(aTarget, task.getResult());
    }

    private List<String> getAnnotators(AgreementFormModel model)
    {
        List<String> annotators;
        if (isEmpty(model.annotators)) {
            annotators = listUsersWithPermissions().stream().map(t -> t.getUsername()).toList();
        }
        else {
            annotators = model.annotators.stream().map(t -> t.getUsername()).toList();
        }
        return annotators;
    }

    List<Pair<String, String>> listMeasures()
    {
        if (form.getModelObject().feature == null) {
            return Collections.emptyList();
        }

        return agreementRegistry.getAgreementMeasureSupports(form.getModelObject().feature).stream()
                .map(s -> Pair.of(s.getId(), s.getName())) //
                .toList();
    }

    private List<AnnotationFeature> getEligibleFeatures()
    {
        return annotationService.listEnabledFeatures(getProject()).stream() //
                .filter(f -> !Token._TypeName.equals(f.getLayer().getName())) //
                .filter(f -> !ChainLayerSupport.TYPE.equals(f.getLayer().getType())) //
                .toList();
    }

    @OnEvent
    public void onPairwiseAgreementScoreClicked(PairwiseAgreementScoreClickedEvent aEvent)
    {
        // Copy the relevant information from the event to avoid having to pass the event into the
        // lambda which would cause problems here since the event is not serializable
        var annotator1 = aEvent.getAnnotator1();
        var annotator2 = aEvent.getAnnotator2();

        var filename = getProject().getSlug() + "-diff.csv";
        downloadBehavior.initiate(aEvent.getTarget(), filename, new PipedStreamResource((os) -> {
            var sessionOwner = userRepository.getCurrentUser();
            var model = form.getModelObject();

            // PipedStreamResource runs the lambda in a separate thread, so we need to make
            // sure the MDC is correctly set up here.
            try (var ctx = new DefaultMdcSetup(repositoryProperties, getProject(), sessionOwner)) {
                agreementService.exportPairwiseDiff(os, model.feature, model.measure.getKey(),
                        getTraits(), sessionOwner, model.documents, annotator1, annotator2);
            }
        }));
    }

    private DefaultAgreementTraits getTraits()
    {
        return (DefaultAgreementTraits) traitsContainer.get(MID_TRAITS).getDefaultModelObject();
    }

    static class AgreementFormModel
        implements Serializable
    {
        private static final long serialVersionUID = -1L;

        AnnotationFeature feature;

        Pair<String, String> measure;

        List<ProjectUserPermissions> annotators = new ArrayList<>();

        List<SourceDocument> documents = new ArrayList<>();
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
