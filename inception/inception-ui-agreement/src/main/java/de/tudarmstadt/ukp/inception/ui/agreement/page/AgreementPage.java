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

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet.CURATION_SET;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet.INITIAL_SET;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet.forUser;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.NS_PROJECT;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.PAGE_PARAM_PROJECT;
import static de.tudarmstadt.ukp.inception.documents.api.export.CrossDocumentExporter.EXT_CSV;
import static de.tudarmstadt.ukp.inception.documents.api.export.CrossDocumentExporter.EXT_JSON;
import static de.tudarmstadt.ukp.inception.scheduling.TaskScope.LAST_USER_SESSION;
import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CHANGE_EVENT;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.enabledWhen;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;
import static java.util.stream.Collectors.groupingBy;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.wicketstuff.annotation.mount.MountPath;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.agreement.AgreementResult_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.agreement.AgreementService;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasureSupport;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.DefaultAgreementTraits;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.krippendorffalpha.KrippendorffAlphaAgreementMeasureSupport;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.krippendorffalphaunitizing.KrippendorffAlphaUnitizingAgreementMeasureSupport;
import de.tudarmstadt.ukp.clarin.webanno.agreement.results.coding.event.PairwiseAgreementScoreClickedEvent;
import de.tudarmstadt.ukp.clarin.webanno.agreement.task.CalculatePairwiseAgreementTask;
import de.tudarmstadt.ukp.clarin.webanno.agreement.task.CalculatePerDocumentAgreementTask;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectUserPermissions;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.project.users.ProjectUserPermissionChoiceRenderer;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.layer.chain.api.ChainLayerSupport;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.documents.api.RepositoryProperties;
import de.tudarmstadt.ukp.inception.documents.api.export.CrossDocumentExporter;
import de.tudarmstadt.ukp.inception.documents.api.export.CrossDocumentExporterRegistry;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
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
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final long serialVersionUID = 5333662917247971912L;

    private static final String MID_TRAITS_CONTAINER = "traitsContainer";
    private static final String MID_TRAITS = "traits";
    private static final String MID_RESULTS = "results";

    private @SpringBean DocumentService documentService;
    private @SpringBean ProjectService projectService;
    private @SpringBean AnnotationSchemaService schemaService;
    private @SpringBean UserDao userRepository;
    private @SpringBean AgreementMeasureSupportRegistry agreementRegistry;
    private @SpringBean SchedulingService schedulingService;
    private @SpringBean RepositoryProperties repositoryProperties;
    private @SpringBean AgreementService agreementService;
    private @SpringBean CrossDocumentExporterRegistry crossDocumentExporterRegistry;

    private Form<AgreementFormModel> form;
    private WebMarkupContainer resultsContainer;
    // private LambdaAjaxBehavior refreshResultsBehavior;
    private AjaxDownloadBehavior downloadBehavior;
    private DropDownChoice<Pair<AnnotationLayer, AnnotationFeature>> featureList;
    private DropDownChoice<Pair<String, String>> measureDropDown;
    private LambdaAjaxButton<AgreementFormModel> calculatePairwiseAgreementButton;
    private LambdaAjaxButton<AgreementFormModel> calculatePerDocumentAgreement;
    private LambdaAjaxButton<AgreementFormModel> exportCsvDiffButton;
    private LambdaAjaxButton<AgreementFormModel> exportJsonButton;
    private LambdaAjaxButton<AgreementFormModel> exportCsvButton;
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

        queue(featureList = makeFeatureChoice("layerAndFeature"));
        queue(measureDropDown = makeMeasuresDropdown("measure"));

        queue(new CheckBox("compareWithCurator").setOutputMarkupId(true));

        queue(new CheckBox("compareWithInitialCas").setOutputMarkupId(true));

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

        exportCsvDiffButton = new LambdaAjaxButton<>("exportCsvDiff", this::actionExportDiff);
        exportCsvDiffButton.triggerAfterSubmit();
        exportCsvDiffButton.add(enabledWhen(() -> measureDropDown.getModelObject() != null));
        queue(exportCsvDiffButton);

        exportJsonButton = new LambdaAjaxButton<>("exportJson", this::actionExportJson);
        exportJsonButton.triggerAfterSubmit();
        exportJsonButton.add(enabledWhen(() -> featureList.getModelObject() != null));
        queue(exportJsonButton);

        exportCsvButton = new LambdaAjaxButton<>("exportCsv", this::actionExportCsv);
        exportCsvButton.triggerAfterSubmit();
        exportCsvButton.add(enabledWhen(() -> featureList.getModelObject() != null));
        queue(exportCsvButton);

        if (featureList.getChoices().size() == 1) {
            featureList.setModelObject(featureList.getChoices().get(0));
        }

        preselectBestAgreementMeasures();
    }

    private void preselectBestAgreementMeasures()
    {
        if (measureDropDown.getChoices().isEmpty()) {
            measureDropDown.setModelObject(null);
            return;
        }

        // If possible use Krippendorff Alpha
        measureDropDown.getChoices().stream() //
                .filter(p -> p.getKey().equals(KrippendorffAlphaAgreementMeasureSupport.ID))
                .findFirst().ifPresent(measureDropDown::setModelObject);

        // ... or even better - if available use Krippendorff Alpha Unitizing
        measureDropDown.getChoices().stream() //
                .filter(p -> p.getKey()
                        .equals(KrippendorffAlphaUnitizingAgreementMeasureSupport.ID))
                .findFirst().ifPresent(measureDropDown::setModelObject);
    }

    private boolean isMeasureSupportingMoreThanTwoRaters()
    {
        var measure = measureDropDown.getModelObject();
        if (measure == null) {
            return false;
        }

        var ams = agreementRegistry.getAgreementMeasureSupport(measure.getKey());
        return ams.isSupportingMoreThanTwoRaters();
    }

    private List<ProjectUserPermissions> listUsersWithPermissions()
    {
        return projectService.listProjectUserPermissions(getProject()).stream() //
                .filter(p -> p.getRoles().contains(ANNOTATOR)) //
                .sorted(comparing(p -> p.getUser().map(User::getUiName).orElse(p.getUsername()))) //
                .toList();
    }

    private List<SourceDocument> listDocuments()
    {
        return documentService.listSourceDocuments(getProject());
    }

    private DropDownChoice<Pair<AnnotationLayer, AnnotationFeature>> makeFeatureChoice(String aId)
    {
        var choice = new DropDownChoice<Pair<AnnotationLayer, AnnotationFeature>>(aId);
        choice.setOutputMarkupId(true);
        choice.setChoices(LoadableDetachableModel.of(this::getEligibleFeatures));
        choice.setChoiceRenderer(new LambdaChoiceRenderer<Pair<AnnotationLayer, AnnotationFeature>>(
                pair -> pair.getKey().getUiName() + " : "
                        + (pair.getValue() != null ? pair.getValue().getUiName() : "<position>")));
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
                    var layer = featureList.getModel().map(Pair::getKey);
                    var feature = featureList.getModel().map(Pair::getValue);
                    newTraits = ams.createTraitsEditor(MID_TRAITS, layer, feature,
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
                        calculatePerDocumentAgreement, exportCsvDiffButton, exportJsonButton,
                        exportCsvButton, traitsContainer)));
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

    // private void refreshResultsWhenFinished(AjaxRequestTarget aTarget,
    // CalculatePairwiseAgreementTask aTask)
    // {
    // // var task = getCurrentTask();
    // var task = aTask;
    //
    // if (task == null || task.getMonitor().getState() != TaskState.COMPLETED) {
    // return;
    // }
    //
    // refreshResults(aTarget, task.getResult());
    // }

    private void refreshResults(AjaxRequestTarget aTarget, AgreementResult_ImplBase aResult)
    {
        var ams = agreementRegistry
                .getAgreementMeasureSupport(measureDropDown.getModelObject().getKey());
        var resultsPanel = ams.createResultsPanel(MID_RESULTS, Model.of(aResult), getTraits());
        resultsContainer.addOrReplace(resultsPanel);
        aTarget.add(resultsContainer);
    }

    private void actionSelectFeature(AjaxRequestTarget aTarget)
    {
        // // If the currently selected measure is not compatible with the selected feature, then
        // // we clear the measure selection.
        // var selectedFeature = featureList.getModelObject();
        // var measureCompatibleWithFeature = measureDropDown.getModel() //
        // .map(k -> agreementRegistry.getAgreementMeasureSupport(k.getKey())) //
        // .map(s -> selectedFeature != null && s.accepts(selectedFeature)) //
        // .orElse(false) //
        // .getObject();
        //
        // if (!measureCompatibleWithFeature) {
        // preselectBestAgreementMeasures();
        // }

        preselectBestAgreementMeasures();

        aTarget.add(measureDropDown, calculatePerDocumentAgreement,
                calculatePairwiseAgreementButton, traitsContainer, exportCsvDiffButton,
                exportJsonButton, exportCsvButton);
    }

    private void actionExportDiff(AjaxRequestTarget aTarget, Form<AgreementFormModel> aForm)
    {
        var filename = getProject().getSlug() + "-diff.csv";
        downloadBehavior.initiate(aTarget, filename, new PipedStreamResource(os -> {
            // PipedStreamResource runs the lambda in a separate thread, so we need to make
            // sure the MDC is correctly set up here.
            var sessionOwner = userRepository.getCurrentUser();
            var project = getProject();
            try (var ctx = new DefaultMdcSetup(repositoryProperties, project, sessionOwner)) {
                exportDiff(os, aForm.getModelObject());
            }
            catch (Exception e) {
                os.write("Unexpected error during export, see log for details.".getBytes(UTF_8));
                LOG.error("Unexpected error while exporting diff", e);
            }
        }, MediaType.valueOf("text/csv")));

    }

    private void exportDiff(OutputStream os, AgreementFormModel model) throws IOException
    {
        var annotators = getAnnotators(model);
        var feature = model.layerAndFeature.getValue();
        var layer = feature == null ? model.layerAndFeature.getKey() : feature.getLayer();
        var selectedDocuments = model.documents;
        var traits = getTraits();

        agreementService.exportDiff(os, layer, feature, traits, selectedDocuments, annotators);
    }

    private void actionExportJson(AjaxRequestTarget aTarget, Form<AgreementFormModel> aForm)
    {
        initiateExport(aTarget, aForm, exp -> EXT_JSON.equals(exp.getFileExtension()));
    }

    private void actionExportCsv(AjaxRequestTarget aTarget, Form<AgreementFormModel> aForm)
    {
        initiateExport(aTarget, aForm, exp -> EXT_CSV.equals(exp.getFileExtension()));
    }

    private void initiateExport(AjaxRequestTarget aTarget, Form<AgreementFormModel> aForm,
            Predicate<CrossDocumentExporter> selector)
    {
        var model = aForm.getModelObject();
        var feature = model.layerAndFeature.getValue();
        var layer = feature == null ? model.layerAndFeature.getKey() : feature.getLayer();

        var maybeExporter = crossDocumentExporterRegistry.getExtensions(layer, feature).stream() //
                .filter(selector) //
                .findFirst();

        if (!maybeExporter.isPresent()) {
            warn("No exporter available for layer / feature of type [" + layer.getType() + "] / ["
                    + (feature != null ? feature.getType() : "<position>") + "]");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        var annotators = getAnnotators(model);
        var selectedDocuments = model.documents;
        var exporter = maybeExporter.get();
        var filename = getLayerExportFilename(layer, feature, exporter.getFileExtension());
        var mediaType = exporter.getMediaType();
        var traits = getTraits();
        var exporterId = exporter.getId();

        var exportResource = new PipedStreamResource(os -> performExport(os, exporterId, layer,
                feature, selectedDocuments, annotators, traits), mediaType);

        downloadBehavior.initiate(aTarget, filename, exportResource);
    }

    private void performExport(OutputStream aOut, String aExporterId, AnnotationLayer aLayer,
            AnnotationFeature aFeature, List<SourceDocument> aSelectedDocuments,
            List<AnnotationSet> aDataOwners, DefaultAgreementTraits aTraits)
        throws IOException
    {
        // Exporter is not serializable (and should not be), so we just pass the ID
        // and fetch the exporter again here.
        var exporter = crossDocumentExporterRegistry.getExtension(aExporterId).get();

        // PipedStreamResource runs the lambda in a separate thread, so we need to make
        // sure the MDC is correctly set up here.
        var project = aLayer.getProject();
        var sessionOwner = userRepository.getCurrentUser();
        try (var ctx = new DefaultMdcSetup(repositoryProperties, project, sessionOwner)) {
            var allAnnDocs = agreementService.getDocumentsToEvaluate(project, aSelectedDocuments,
                    aTraits);
            exporter.export(aOut, aLayer, aFeature, allAnnDocs, aDataOwners);
        }
        catch (Exception e) {
            aOut.write("Unexpected error during export, see log for details.".getBytes(UTF_8));
            LOG.error("Unexpected error while exporting diff", e);
        }
    }

    private String getLayerExportFilename(AnnotationLayer aLayer, AnnotationFeature aFeature,
            String aSuffix)
    {
        var project = aLayer.getProject();
        var filename = project.getSlug() + "-" + aLayer.getName();
        if (aFeature != null) {
            filename += "-" + aFeature.getName();
        }
        filename += aSuffix;
        return filename;
    }

    private void actionCalculatePairwiseAgreement(AjaxRequestTarget aTarget,
            Form<AgreementFormModel> aForm)
    {
        var model = aForm.getModelObject();
        var project = getProject();

        // Do not do any agreement if no feature or measure has been selected yet.
        if (model.layerAndFeature == null || model.measure == null) {
            return;
        }

        var traits = getTraits();
        var layer = model.layerAndFeature.getKey();
        var feature = model.layerAndFeature.getValue();

        var measure = agreementRegistry.getMeasure(layer, feature, model.measure.getKey(), traits);
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
                .withLayer(model.layerAndFeature.getKey()) //
                .withFeature(model.layerAndFeature.getValue()) //
                .withMeasure(measure) //
                .withDocuments(allAnnDocs) //
                .withScope(LAST_USER_SESSION) //
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
        if (model.layerAndFeature == null || model.measure == null) {
            return;
        }

        var traits = getTraits();
        var layer = model.layerAndFeature.getKey();
        var feature = model.layerAndFeature.getValue();

        var measure = agreementRegistry.getMeasure(layer, feature, model.measure.getKey(), traits);

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
                .withFeature(model.layerAndFeature.getValue()) //
                .withMeasure(measure) //
                .withDocuments(allAnnDocs) //
                .withScope(LAST_USER_SESSION) //
                // When running sync, we cannot cancel because the browser will still be in an
                // AJAX request when we try to fire a second one and that second one will fail
                // then. This would only work if the cancel action would be sent through
                // WebSocket
                .withCancellable(false) //
                .build();

        schedulingService.executeSync(task);

        refreshResults(aTarget, task.getResult());
    }

    private List<AnnotationSet> getAnnotators(AgreementFormModel model)
    {
        var annotators = new ArrayList<AnnotationSet>();

        if (model.compareWithInitialCas) {
            annotators.add(INITIAL_SET);
        }

        if (model.compareWithCurator) {
            annotators.add(CURATION_SET);
        }

        if (isEmpty(model.annotators)) {
            listUsersWithPermissions().stream() //
                    .map(t -> forUser(t.getUsername())) //
                    .forEach(annotators::add);
        }
        else {
            model.annotators.stream() //
                    .map(t -> forUser(t.getUsername())) //
                    .forEach(annotators::add);
        }

        return annotators;
    }

    private List<Pair<String, String>> listMeasures()
    {
        if (form.getModelObject().layerAndFeature == null) {
            return emptyList();
        }

        var layer = form.getModelObject().layerAndFeature.getKey();
        var feature = form.getModelObject().layerAndFeature.getValue();

        return agreementRegistry.getAgreementMeasureSupports(layer, feature) //
                .stream().map(s -> Pair.of(s.getId(), s.getName())) //
                .toList();
    }

    private List<Pair<AnnotationLayer, AnnotationFeature>> getEligibleFeatures()
    {
        var result = new ArrayList<Pair<AnnotationLayer, AnnotationFeature>>();

        // Collect layers (even without features)
        var layers = schemaService.listEnabledLayers(getProject());
        for (var layer : layers) {
            if (Token._TypeName.equals(layer.getName())
                    || Sentence._TypeName.equals(layer.getName())) {
                continue;
            }

            result.add(Pair.of(layer, null));
        }

        // Now collect the features
        var groupedFeatures = schemaService.listEnabledFeatures(getProject()).stream()
                .collect(groupingBy(AnnotationFeature::getLayer));
        for (var entry : groupedFeatures.entrySet()) {
            var layer = entry.getKey();
            var features = entry.getValue();

            // We don't need chain layers here atm since they do not support custom
            // features, in the CSV/JSON export, both built-in features are always
            // included and in the pairwise agreement, they are not supported yet.
            if (Token._TypeName.equals(layer.getName())
                    || Sentence._TypeName.equals(layer.getName())
                    || ChainLayerSupport.TYPE.equals(layer.getType())) {
                continue;
            }

            for (var feature : features) {
                result.add(Pair.of(layer, feature));
            }
        }

        result.sort(comparing(
                (Pair<AnnotationLayer, AnnotationFeature> pair) -> pair.getKey().getUiName()) //
                        .thenComparing(pair -> pair.getValue() != null ? pair.getValue().getUiName()
                                : null, nullsFirst(naturalOrder())));
        return result;
    }

    @OnEvent
    public void onPairwiseAgreementScoreClicked(PairwiseAgreementScoreClickedEvent aEvent)
    {
        // Copy the relevant information from the event to avoid having to pass the event into the
        // lambda which would cause problems here since the event is not serializable
        var annotator1 = forUser(aEvent.getAnnotator1());
        var annotator2 = forUser(aEvent.getAnnotator2());

        var filename = getProject().getSlug() + "-diff.csv";
        downloadBehavior.initiate(aEvent.getTarget(), filename, new PipedStreamResource((os) -> {
            var sessionOwner = userRepository.getCurrentUser();
            var model = form.getModelObject();

            // PipedStreamResource runs the lambda in a separate thread, so we need to make
            // sure the MDC is correctly set up here.
            try (var ctx = new DefaultMdcSetup(repositoryProperties, getProject(), sessionOwner)) {
                var layer = model.layerAndFeature.getKey();
                var feature = model.layerAndFeature.getValue();

                if (feature != null) {
                    agreementService.exportDiff(os, feature.getLayer(), feature, getTraits(),
                            model.documents, asList(annotator1, annotator2));
                }
                else {
                    agreementService.exportDiff(os, layer, null, getTraits(), model.documents,
                            asList(annotator1, annotator2));
                }

            }
        }, MediaType.valueOf("text/csv")));
    }

    private DefaultAgreementTraits getTraits()
    {
        var traitsPanel = traitsContainer.get(MID_TRAITS);
        if (traitsPanel instanceof EmptyPanel) {
            return new DefaultAgreementTraits();
        }

        return (DefaultAgreementTraits) traitsContainer.get(MID_TRAITS).getDefaultModelObject();
    }

    static class AgreementFormModel
        implements Serializable
    {
        private static final long serialVersionUID = -1L;

        Pair<AnnotationLayer, AnnotationFeature> layerAndFeature;

        Pair<String, String> measure;

        List<ProjectUserPermissions> annotators = new ArrayList<>();

        List<SourceDocument> documents = new ArrayList<>();

        boolean compareWithCurator;

        boolean compareWithInitialCas;
    }
}
