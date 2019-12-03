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
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListChoice;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.annotation.mount.MountPath;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.select.BootstrapSelect;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AggreementMeasure;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AggreementMeasureSupport;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AggreementMeasureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.DefaultAgreementTraits;
import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaChoiceRenderer;
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
    private static final String MID_RESULTS = "results";
    
    private @SpringBean DocumentService documentService;
    private @SpringBean ProjectService projectService;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean UserDao userRepository;
    private @SpringBean AggreementMeasureSupportRegistry agreementRegistry;

    private ProjectSelectionForm projectSelectionForm;
    private AgreementForm agreementForm;
    private WebMarkupContainer resultsContainer;

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
            Project p = project.get();
            
            // Check access to project
            if (!(projectService.isCurator(p, user) || projectService.isManager(p, user))) {
                error("You have no permission to access project [" + p.getId() + "]");
                setResponsePage(getApplication().getHomePage());
            }

            projectSelectionForm.getModelObject().project = p;
        }
        else {
            error("Project [" + projectParameter + "] does not exist");
            setResponsePage(getApplication().getHomePage());
        }
    }    
    
    private void commonInit()
    {
        add(projectSelectionForm = new ProjectSelectionForm("projectSelectionForm"));
        
        add(agreementForm = new AgreementForm("agreementForm", Model.of(new AgreementFormModel())));
        
        add(resultsContainer = new WebMarkupContainer("resultsContainer"));
        resultsContainer.setOutputMarkupPlaceholderTag(true);
        resultsContainer.add(new EmptyPanel(MID_RESULTS));
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
                    _target.add(runCalculationsButton, traitsContainer)));
            
            runCalculationsButton.add(enabledWhen(() -> measureDropDown.getModelObject() != null));
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
            
            aTarget.add(measureDropDown, runCalculationsButton);
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        private void actionRunCalculations(AjaxRequestTarget aTarget, Form<?> aForm)
        {
            AnnotationFeature feature = featureList.getModelObject();
            Pair<String, String> measureHandle = measureDropDown.getModelObject();

            // Do not do any agreement if no feature or measure has been selected yet.
            if (feature == null || measureHandle == null) {
                return;
            }
            
            AggreementMeasureSupport ams = agreementRegistry
                    .getAgreementMeasureSupport(measureDropDown.getModelObject().getKey());
            
            AggreementMeasure measure = ams.createMeasure(feature,
                    (DefaultAgreementTraits) traitsContainer.get(MID_TRAITS)
                            .getDefaultModelObject());
            
            Serializable result = measure.getAgreement(getCasMap());
            
            resultsContainer.addOrReplace(ams.createResultsPanel(MID_RESULTS, Model.of(result),
                    AgreementPage.this::getCasMap));
            
            aTarget.add(resultsContainer);
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
            agreementForm.setModelObject(new AgreementFormModel());
            resultsContainer.addOrReplace(new EmptyPanel(MID_RESULTS));
            aTarget.add(resultsContainer, agreementForm);
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
    
    // The CASes cannot be serialized, so we make them transient here. However, it does not matter
    // as we do not access the field directly but via getCases() which will re-load them if
    // necessary, e.g. if the transient field is empty after a session is restored from a
    // persisted state.
    private transient Map<String, List<CAS>> cachedCASes;
    private transient Project cachedProject;
    private transient boolean cachedLimitToFinishedDocuments;

    public Map<String, List<CAS>> getCasMap()
    {
        if (agreementForm.featureList.getModelObject() == null) {
            return Collections.emptyMap();
        }

        Project project = agreementForm.featureList.getModelObject().getProject();

        DefaultAgreementTraits traits = (DefaultAgreementTraits) agreementForm.traitsContainer
                .get(MID_TRAITS).getDefaultModelObject();
        
        // Avoid reloading the CASes when switching features within the same project
        if (
                cachedCASes != null && 
                project.equals(cachedProject) &&
                cachedLimitToFinishedDocuments == traits.isLimitToFinishedDocuments()
        ) {
            return cachedCASes;
        }
        
        List<User> users = projectService.listProjectUsersWithPermissions(project, ANNOTATOR);

        List<SourceDocument> sourceDocuments = documentService
                .listSourceDocuments(project);
        
        cachedCASes = new LinkedHashMap<>();
        for (User user : users) {
            List<CAS> cases = new ArrayList<>();

            // Bulk-fetch all source documents for which there is already an annoation document for
            // the user which is faster then checking for their existence individually
            List<SourceDocument> docsForUser = documentService
                    .listAnnotationDocuments(project, user).stream()
                    .map(AnnotationDocument::getDocument)
                    .distinct()
                    .collect(Collectors.toList());
            
            nextDocument: for (SourceDocument document : sourceDocuments) {
                CAS cas = null;

                try {
                    if (docsForUser.contains(document)) {
                        AnnotationDocument annotationDocument = documentService
                                .getAnnotationDocument(document, user);
                        
                        if (traits.isLimitToFinishedDocuments()
                                && !annotationDocument.getState().equals(FINISHED)) {
                            // Add a skip marker for the current CAS to the CAS list - this is 
                            // necessary because we expect the CAS lists for all users to have the
                            // same size
                            cases.add(null);
                            continue nextDocument;
                        }
                        
                        cas = documentService.readAnnotationCas(annotationDocument);
                    }
                    else if (!traits.isLimitToFinishedDocuments()) {
                        // ... if we are not limited to finished documents and if there is no
                        // annotation document, then we use the initial CAS for that user.
                        cas = documentService.createOrReadInitialCas(document);
                    }
                }
                catch (Exception e) {
                    LOG.error("Unable to load data", e);
                    error("Unable to load data: " + ExceptionUtils.getRootCauseMessage(e));
                }
                
                if (cas != null) {
                    // Set the CAS name in the DocumentMetaData so that we can pick it
                    // up in the Diff position for the purpose of debugging / transparency.
                    FeatureStructure dmd = WebAnnoCasUtil.getDocumentMetadata(cas);
                    FSUtil.setFeature(dmd, "documentId", document.getName());
                    FSUtil.setFeature(dmd, "collectionId", document.getProject().getName());
                    
                }

                // The next line can enter null values into the list if a user didn't work on this
                // source document yet.
                cases.add(cas);
            }
            
            // Bulk-upgrade CASes - this is faster than upgrading them individually since the
            // bulk upgrade only loads the project type system once.
            try {
                annotationService.upgradeCasIfRequired(cases, project);
                // REC: I think there is no need to write the CASes here. We would not
                // want to interfere with currently active annotator users
            }
            catch (Exception e) {
                LOG.error("Unable to upgrade CAS", e);
                error("Unable to upgrade CAS: " + ExceptionUtils.getRootCauseMessage(e));
                continue;
            }
                
            cachedCASes.put(user.getUsername(), cases);
        }

        cachedProject = project;
        cachedLimitToFinishedDocuments = traits.isLimitToFinishedDocuments();

        return cachedCASes;
    }
}
