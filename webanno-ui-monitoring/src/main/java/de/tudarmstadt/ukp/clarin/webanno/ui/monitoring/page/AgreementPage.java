/*
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universit?t Darmstadt
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
package de.tudarmstadt.ukp.clarin.webanno.ui.monitoring.page;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.PAGE_PARAM_PROJECT_ID;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.enabledWhen;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.persistence.NoResultException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.util.FSUtil;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.resource.AbstractResourceStream;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.apache.wicket.util.string.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.annotation.mount.MountPath;

import de.agilecoders.wicket.core.markup.html.bootstrap.components.PopoverBehavior;
import de.agilecoders.wicket.core.markup.html.bootstrap.components.PopoverConfig;
import de.agilecoders.wicket.core.markup.html.bootstrap.components.TooltipConfig.Placement;
import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.curation.agreement.AgreementReportExportFormat;
import de.tudarmstadt.ukp.clarin.webanno.curation.agreement.AgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.agreement.AgreementUtils;
import de.tudarmstadt.ukp.clarin.webanno.curation.agreement.PairwiseAnnotationResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.agreement.measures.AggreementMeasure;
import de.tudarmstadt.ukp.clarin.webanno.curation.agreement.measures.AggreementMeasureSupport;
import de.tudarmstadt.ukp.clarin.webanno.curation.agreement.measures.AggreementMeasureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.curation.agreement.measures.DefaultAgreementTraits;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.LinkCompareBehavior;
import de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.api.DiffAdapter;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.bootstrap.select.BootstrapSelect;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaChoiceRenderer;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.AjaxDownloadLink;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.OverviewListChoice;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

@MountPath("/agreement.html")
public class AgreementPage
    extends ApplicationPageBase
{
    private static final long serialVersionUID = 5333662917247971912L;

    private static final Logger LOG = LoggerFactory.getLogger(AgreementPage.class);

    private static final String MID_TRAITS_CONTAINER = "traitsContainer";
    private static final String MID_TRAITS = "traits";
    
    private @SpringBean DocumentService documentService;
    private @SpringBean ProjectService projectService;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean UserDao userRepository;
    private @SpringBean AggreementMeasureSupportRegistry agreementRegistry;

    private ProjectSelectionForm projectSelectionForm;
    private AgreementForm agreementForm;

    public AgreementPage()
    {
        super();
        
        commonInit();
    }

    public AgreementPage(final PageParameters aPageParameters)
    {
        super(aPageParameters);
        
        commonInit();
       
        projectSelectionForm.setVisibilityAllowed(false);
        
        User user = userRepository.getCurrentUser();
        
        // Get current project from parameters
        StringValue projectParameter = aPageParameters.get(PAGE_PARAM_PROJECT_ID);
        Optional<Project> project = getProjectFromParameters(projectParameter);
        
        if (project.isPresent()) {
            // Check access to project
            if (project != null && !(projectService.isCurator(project.get(), user)
                    || projectService.isManager(project.get(), user))) {
                error("You have no permission to access project [" + project.get().getId() + "]");
                setResponsePage(getApplication().getHomePage());
            }
            
            projectSelectionForm.selectProject(project.get());
        }
        else {
            error("Project [" + projectParameter + "] does not exist");
            setResponsePage(getApplication().getHomePage());
        }
    }    
    
    private void commonInit()
    {
        add(projectSelectionForm = new ProjectSelectionForm("projectSelectionForm"));
        add(agreementForm = new AgreementForm("agreementForm"));
    }
    
    private void updateAgreementTable(AjaxRequestTarget aTarget, boolean aClearCache)
    {
        try {
            if (aClearCache) {
                cachedCASes = null;
            }
            agreementForm.agreementTable.getDefaultModel().detach();
            if (aTarget != null && agreementForm.agreementTable.isVisibleInHierarchy()) {
                aTarget.add(agreementForm.agreementTable);
            }
        }
        catch (Throwable e) {
            LOG.error("Error updating agreement table", e);
            error("Error updating agreement table: " + ExceptionUtils.getRootCauseMessage(e));
            if (aTarget != null) {
                aTarget.addChildren(getPage(), IFeedback.class);
            }
        }
    }

    // The CASes cannot be serialized, so we make them transient here. However, it does not matter
    // as we do not access the field directly but via getCases() which will re-load them if
    // necessary, e.g. if the transient field is empty after a session is restored from a
    // persisted state.
    private transient Map<String, List<CAS>> cachedCASes;

    /**
     * Get the finished CASes used to compute agreement.
     */
    private Map<String, List<CAS>> getCases()
    {
        // Avoid reloading the CASes when switching features.
        if (cachedCASes != null) {
            return cachedCASes;
        }

        Project project = projectSelectionForm.getModelObject().project;

        List<User> users = projectService.listProjectUsersWithPermissions(project, ANNOTATOR);

        List<SourceDocument> sourceDocuments = documentService.listSourceDocuments(project);

        cachedCASes = new LinkedHashMap<>();
        for (User user : users) {
            List<CAS> cases = new ArrayList<>();

            for (SourceDocument document : sourceDocuments) {
                CAS cas = null;

                // Load the CAS if there is a finished one.
                if (documentService.existsAnnotationDocument(document, user)) {
                    AnnotationDocument annotationDocument = documentService
                            .getAnnotationDocument(document, user);
                    if (annotationDocument.getState().equals(FINISHED)) {
                        try {
                            cas = documentService.readAnnotationCas(annotationDocument);
                            annotationService.upgradeCasIfRequired(cas, annotationDocument);
                            // REC: I think there is no need to write the CASes here. We would not
                            // want to interfere with currently active annotator users

                            // Set the CAS name in the DocumentMetaData so that we can pick it
                            // up in the Diff position for the purpose of debugging / transparency.
                            FeatureStructure dmd = WebAnnoCasUtil.getDocumentMetadata(cas);
                            FSUtil.setFeature(dmd, "documentId",
                                    annotationDocument.getDocument().getName());
                            FSUtil.setFeature(dmd, "collectionId",
                                    annotationDocument.getProject().getName());
                        }
                        catch (Exception e) {
                            LOG.error("Unable to load data", e);
                            error("Unable to load data: " + ExceptionUtils.getRootCauseMessage(e));
                        }
                    }
                }

                // The next line can enter null values into the list if a user didn't work on this
                // source document yet.
                cases.add(cas);
            }

            cachedCASes.put(user.getUsername(), cases);
        }

        return cachedCASes;
    }

    private class AgreementForm
        extends Form<AgreementFormModel>
    {
        private static final long serialVersionUID = -1L;

        private final DropDownChoice<AnnotationFeature> featureList;

        private final AgreementTable agreementTable;

        private final DropDownChoice<Pair<String, String>> measureDropDown;

        private final AjaxDownloadLink exportAllButton;
        
        private final LambdaAjaxButton<Void> runCalculationsButton;
        
        private WebMarkupContainer traitsContainer;

        public AgreementForm(String id)
        {
            super(id, new CompoundPropertyModel<>(new AgreementFormModel()));

            setOutputMarkupPlaceholderTag(true);

            add(traitsContainer = new WebMarkupContainer(MID_TRAITS_CONTAINER));
            traitsContainer.setOutputMarkupPlaceholderTag(true);
            traitsContainer.add(new EmptyPanel(MID_TRAITS));
            
            add(new Label("name",
                    PropertyModel.of(projectSelectionForm.getModel(), "project.name")));
            
            add(featureList = new BootstrapSelect<AnnotationFeature>("feature"));
            featureList.setOutputMarkupId(true);
            featureList.setChoices(LoadableDetachableModel.of(this::getEligibleFeatures));
            featureList.setChoiceRenderer(new LambdaChoiceRenderer<AnnotationFeature>(feature -> 
                    feature.getLayer().getUiName() + " : " + feature.getUiName()));
            featureList.add(new LambdaAjaxFormComponentUpdatingBehavior("change", 
                    this::actionSelectFeature));

            runCalculationsButton = new LambdaAjaxButton<>("run", this::actionRunCalculations);
            runCalculationsButton.triggerAfterSubmit();
            add(runCalculationsButton);

            add(new BootstrapSelect<AgreementReportExportFormat>("exportFormat",
                    asList(AgreementReportExportFormat.values()),
                    new EnumChoiceRenderer<>(AgreementPage.this))
                            .add(new LambdaAjaxFormComponentUpdatingBehavior("change")));

            exportAllButton = new AjaxDownloadLink("exportAll", () -> "agreement"
                    + AgreementForm.this.getModelObject().exportFormat.getExtension(),
                    this::exportAllAgreements);
            exportAllButton.add(enabledWhen(() -> featureList.getModelObject() != null));
            add(exportAllButton);
            
            add(measureDropDown = new BootstrapSelect<Pair<String, String>>("measure", 
                    this::listMeasures) {
                private static final long serialVersionUID = -2666048788050249581L;

                @Override
                protected void onModelChanged()
                {
                    super.onModelChanged();
                    
                    // If the feature type has changed, we need to set up a new traits editor
                    Component newTraits;
                    if (getModelObject() != null) {
                        AggreementMeasureSupport ams = agreementRegistry.getAgreementMeasureSupport(
                                getModelObject().getKey());
                        newTraits = ams.createTraitsEditor(MID_TRAITS, featureList.getModel(),
                                Model.of(ams.createTraits()));
                    }
                    else {
                        newTraits = new EmptyPanel(MID_TRAITS);
                    }

                    traitsContainer.addOrReplace(newTraits);
                }
            });
            measureDropDown.setChoiceRenderer(new ChoiceRenderer<>("value"));
            measureDropDown.add(new LambdaAjaxFormComponentUpdatingBehavior("change", _target -> 
                    _target.add(runCalculationsButton, traitsContainer, exportAllButton)));
            
            exportAllButton.add(enabledWhen(() -> measureDropDown.getModelObject() != null));
            runCalculationsButton.add(enabledWhen(() -> measureDropDown.getModelObject() != null));

            WebMarkupContainer agreementResults = new WebMarkupContainer("agreementResults");
            add(agreementResults);

            agreementResults.add(
                    agreementTable = new AgreementTable("agreementTable", getModel(), Model.of()));
            
            agreementResults.add(visibleWhen(() -> agreementTable.getModelObject() != null));
            
            PopoverConfig config = new PopoverConfig()
                    .withPlacement(Placement.left)
                    .withHtml(true);
            WebMarkupContainer legend = new WebMarkupContainer("legend");
            legend.add(new PopoverBehavior(new ResourceModel("legend"), 
                    new StringResourceModel("legend.content", legend), config));
            agreementResults.add(legend);
        }

        private void actionSelectFeature(AjaxRequestTarget aTarget)
        {
            // If the currently selected measure is not compatible with the selected feature, then
            // we clear the measure selection.
            boolean measureCompatibleWithFeature = measureDropDown.getModel()
                    .map(k -> agreementRegistry.getAgreementMeasureSupport(k.getKey()))
                    .map(s -> s.accepts(featureList.getModelObject()))
                    .orElse(false).getObject();
            if (!measureCompatibleWithFeature) {
                measureDropDown.setModelObject(null);
            }
            
            aTarget.add(measureDropDown, runCalculationsButton, exportAllButton);
        }

        private void actionRunCalculations(AjaxRequestTarget aTarget, Form<?> aForm)
        {
            agreementTable.setModelObject(getAgreementResult());
            
            // We may get errors when loading the CASes but at that time we can no longer
            // add the feedback panel to the cycle, so let's do it here.
            aTarget.add(getFeedbackPanel());

            AgreementPage.this.updateAgreementTable(aTarget, false);
            // // Adding this as well because when choosing a different measure, it may
            // affect
            // // the ability to exclude incomplete configurations.
            // aTarget.add(excludeIncomplete);
            // aTarget.add(linkCompareBehaviorDropDown);

            // #1791 - for some reason the updateAgreementTableBehavior does not work
            // anymore on the linkCompareBehaviorDropDown if we add it explicitly here/
            // control its visibility in onConfigure()
            // as a workaround, we currently just re-render the whole form
            aTarget.add(agreementForm);
        }
        
        List<Pair<String, String>> listMeasures()
        {
            if (getModelObject().feature == null) {
                return Collections.emptyList();
            }
            
            return agreementRegistry.getAgreementMeasureSupports(getModelObject().feature).stream()
                    .map(s -> Pair.of(s.getId(), s.getName()))
                    .collect(Collectors.toList());
        }
        
        private IResourceStream exportAllAgreements()
        {
            return new AbstractResourceStream()
            {
                private static final long serialVersionUID = 1L;

                @Override
                public InputStream getInputStream()
                    throws ResourceStreamNotFoundException
                {
                    AnnotationFeature feature = featureList.getModelObject();

                    // Do not do any agreement if no feature has been selected yet.
                    if (feature == null) {
                        return null;
                    }

                    Map<String, List<CAS>> casMap = getCases();

                    Project project = projectSelectionForm.getModelObject().project;
                    List<DiffAdapter> adapters = CasDiff
                            .getAdapters(annotationService, project);

//                    AgreementFormModel pref = AgreementForm.this.getModelObject();
                    DiffResult diff = CasDiff.doDiff(
                            asList(feature.getLayer().getName()), adapters,
//                            pref.linkCompareBehavior, casMap);
                            LinkCompareBehavior.LINK_TARGET_AS_LABEL, casMap);

//                    AgreementResult agreementResult = AgreementUtils.makeStudy(diff,
//                            feature.getLayer().getName(), feature.getName(),
//                            pref.excludeIncomplete, casMap);
                    // TODO: for the moment, we always include incomplete annotations during this
                    // export.
                    AgreementResult agreementResult = AgreementUtils.makeStudy(diff,
                            feature.getLayer().getName(), feature.getName(),
                            false, casMap);
                    
                    try {
                        return AgreementUtils.generateCsvReport(agreementResult);
                    }
                    catch (Exception e) {
                        // FIXME Is there some better error handling here?
                        LOG.error("Unable to generate report", e);
                        throw new ResourceStreamNotFoundException(e);
                    }
                }

                @Override
                public void close()
                    throws IOException
                {
                    // Nothing to do
                }
            };
        }

        
        private List<AnnotationFeature> getEligibleFeatures()
        {
            List<AnnotationFeature> features = annotationService
                    .listAnnotationFeature(
                            (projectSelectionForm.getModelObject().project));
            List<AnnotationFeature> unusedFeatures = new ArrayList<>();
            for (AnnotationFeature feature : features) {
                if (feature.getLayer().getName().equals(Token.class.getName())
                        || feature.getLayer().getName()
                                .equals(WebAnnoConst.COREFERENCE_LAYER)) {
                    unusedFeatures.add(feature);
                }
            }
            features.removeAll(unusedFeatures);
            return features;
        }
        
        private PairwiseAnnotationResult getAgreementResult()
        {
            AnnotationFeature feature = featureList.getModelObject();
            Pair<String, String> measureHandle = measureDropDown.getModelObject();

            // Do not do any agreement if no feature or measure has been selected yet.
            if (feature == null || measureHandle == null) {
                return null;
            }

            AggreementMeasureSupport support = agreementRegistry
                    .getAgreementMeasureSupport(measureHandle.getKey());
            
            AggreementMeasure measure = support.createMeasure(feature,
                    (DefaultAgreementTraits) traitsContainer.get(MID_TRAITS)
                            .getDefaultModelObject());
            
            Map<String, List<CAS>> casMap = getCases();
            
            return AgreementUtils.getPairwiseAgreement(measure, casMap);
        }        

        @Override
        protected void onConfigure()
        {
            super.onConfigure();

            ProjectSelectionModel model = projectSelectionForm.getModelObject();
            setVisible(model != null && model.project != null);
        }
    }

    static class AgreementFormModel
        implements Serializable
    {
        private static final long serialVersionUID = -1L;

        AnnotationFeature feature;

        Pair<String, String> measure;

        AgreementReportExportFormat exportFormat = AgreementReportExportFormat.CSV;
    }

    private class ProjectSelectionForm
        extends Form<ProjectSelectionModel>
    {
        private static final long serialVersionUID = -1L;

        public ProjectSelectionForm(String id)
        {
            super(id, new CompoundPropertyModel<>(new ProjectSelectionModel()));

            ListChoice<Project> projectList = new OverviewListChoice<>("project");
            projectList.setChoiceRenderer(new ChoiceRenderer<>("name"));
            projectList.setChoices(LoadableDetachableModel.of(this::listAllowedProjects));
            projectList.add(new LambdaAjaxFormComponentUpdatingBehavior("change",
                    this::onSelectionChanged));
            add(projectList);
        }
        
        private void onSelectionChanged(AjaxRequestTarget aTarget)
        {
            selectProject(getModelObject().project);
            aTarget.add(agreementForm);
        }
        
        private List<Project> listAllowedProjects()
        {
            List<Project> allowedProject = new ArrayList<>();

            User user = userRepository.getCurrentUser();

            List<Project> allProjects = projectService.listProjects();
            for (Project project : allProjects) {
                if (projectService.isManager(project, user)
                        || projectService.isCurator(project, user)) {
                    allowedProject.add(project);
                }
            }
            return allowedProject;
        }
        
        private void selectProject(Project aProject)
        {
            getModelObject().project = aProject;
            agreementForm.setModelObject(new AgreementFormModel());

            // Clear the cached CASes. When we switch to another project, we'll have to reload them.
            updateAgreementTable(RequestCycle.get().find(AjaxRequestTarget.class).orElse(null),
                    true);
        }
    }

    static public class ProjectSelectionModel
        implements Serializable
    {
        protected int totalDocuments;

        private static final long serialVersionUID = -1L;

        public Project project;
        public Map<String, Integer> annotatorsProgress = new TreeMap<>();
        public Map<String, Integer> annotatorsProgressInPercent = new TreeMap<>();
    }
    
    private Optional<Project> getProjectFromParameters(StringValue projectParam)
    {
        if (projectParam == null || projectParam.isEmpty()) {
            return Optional.empty();
        }
        
        try {
            return Optional.of(projectService.getProject(projectParam.toLong()));
        }
        catch (NoResultException e) {
            return Optional.empty();
        }
    }
}
