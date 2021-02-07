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
package de.tudarmstadt.ukp.clarin.webanno.ui.monitoring.page;

import static de.tudarmstadt.ukp.clarin.webanno.api.CasUpgradeMode.AUTO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.SHARED_READ_ONLY_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.MANAGER;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.enabledWhen;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.NS_PROJECT;
import static de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase.PAGE_PARAM_PROJECT;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import org.wicketstuff.annotation.mount.MountPath;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.select.BootstrapSelect;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasure;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasureSupport;
import de.tudarmstadt.ukp.clarin.webanno.agreement.measures.AgreementMeasureSupportRegistry;
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
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

@MountPath(NS_PROJECT + "/${" + PAGE_PARAM_PROJECT + "}/agreement")
public class AgreementPage
    extends ProjectPageBase
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
    private @SpringBean AgreementMeasureSupportRegistry agreementRegistry;

    private AgreementForm agreementForm;
    private WebMarkupContainer resultsContainer;

    public AgreementPage(final PageParameters aPageParameters)
    {
        super(aPageParameters);
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();

        User user = userRepository.getCurrentUser();

        requireProjectRole(user, MANAGER, CURATOR);

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

            add(new Label("name", getProject().getName()));

            add(featureList = new BootstrapSelect<AnnotationFeature>("feature"));
            featureList.setOutputMarkupId(true);
            featureList.setChoices(LoadableDetachableModel.of(this::getEligibleFeatures));
            featureList.setChoiceRenderer(new LambdaChoiceRenderer<AnnotationFeature>(
                    feature -> feature.getLayer().getUiName() + " : " + feature.getUiName()));
            featureList.add(new LambdaAjaxFormComponentUpdatingBehavior("change",
                    this::actionSelectFeature));

            runCalculationsButton = new LambdaAjaxButton<>("run", this::actionRunCalculations);
            runCalculationsButton.triggerAfterSubmit();
            add(runCalculationsButton);

            add(measureDropDown = new BootstrapSelect<Pair<String, String>>("measure",
                    this::listMeasures)
            {
                private static final long serialVersionUID = -2666048788050249581L;

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
                                Model.of(ams.createTraits()));
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
            boolean measureCompatibleWithFeature = measureDropDown.getModel()
                    .map(k -> agreementRegistry.getAgreementMeasureSupport(k.getKey()))
                    .map(s -> selectedFeature != null && s.accepts(selectedFeature)).orElse(false)
                    .getObject();
            if (!measureCompatibleWithFeature) {
                measureDropDown.setModelObject(null);
            }

            aTarget.add(measureDropDown, runCalculationsButton, traitsContainer);
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

            AgreementMeasureSupport ams = agreementRegistry
                    .getAgreementMeasureSupport(measureDropDown.getModelObject().getKey());

            AgreementMeasure measure = ams.createMeasure(feature,
                    (DefaultAgreementTraits) traitsContainer.get(MID_TRAITS)
                            .getDefaultModelObject());

            Map<String, List<CAS>> casMap = getCasMap();

            if (casMap.values().stream().allMatch(list -> list == null || list.isEmpty())) {
                error("No documents with annotations were found.");
                aTarget.addChildren(getPage(), IFeedback.class);
            }
            else {
                Serializable result = measure.getAgreement(casMap);
                resultsContainer.addOrReplace(ams.createResultsPanel(MID_RESULTS, Model.of(result),
                        AgreementPage.this::getCasMap));
                aTarget.add(resultsContainer);
            }

        }

        List<Pair<String, String>> listMeasures()
        {
            if (getModelObject().feature == null) {
                return Collections.emptyList();
            }

            return agreementRegistry.getAgreementMeasureSupports(getModelObject().feature).stream()
                    .map(s -> Pair.of(s.getId(), s.getName())).collect(Collectors.toList());
        }

        private List<AnnotationFeature> getEligibleFeatures()
        {
            List<AnnotationFeature> features = annotationService
                    .listAnnotationFeature(getProject());
            List<AnnotationFeature> unusedFeatures = new ArrayList<>();
            for (AnnotationFeature feature : features) {
                if (feature.getLayer().getName().equals(Token.class.getName())
                        || feature.getLayer().getName().equals(WebAnnoConst.COREFERENCE_LAYER)) {
                    unusedFeatures.add(feature);
                }
            }
            features.removeAll(unusedFeatures);
            return features;
        }
    }

    static class AgreementFormModel
        implements Serializable
    {
        private static final long serialVersionUID = -1L;

        AnnotationFeature feature;

        Pair<String, String> measure;
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
        if (cachedCASes != null && project.equals(cachedProject)
                && cachedLimitToFinishedDocuments == traits.isLimitToFinishedDocuments()) {
            return cachedCASes;
        }

        List<User> users = projectService.listProjectUsersWithPermissions(project, ANNOTATOR);

        List<SourceDocument> sourceDocuments = documentService.listSourceDocuments(project);

        cachedCASes = new LinkedHashMap<>();
        for (User user : users) {
            List<CAS> cases = new ArrayList<>();

            // Bulk-fetch all source documents for which there is already an annotation document for
            // the user which is faster then checking for their existence individually
            List<SourceDocument> docsForUser = documentService
                    .listAnnotationDocuments(project, user).stream()
                    .map(AnnotationDocument::getDocument).distinct().collect(Collectors.toList());

            nextDocument: for (SourceDocument document : sourceDocuments) {
                CAS cas = null;

                try {
                    if (docsForUser.contains(document)) {
                        AnnotationDocument annotationDocument = documentService
                                .getAnnotationDocument(document, user);

                        if (traits.isLimitToFinishedDocuments()
                                && !annotationDocument.getState().equals(FINISHED)) {
                            // Add a skip marker (null) for the current CAS to the CAS list - this
                            // is necessary because we expect the CAS lists for all users to have
                            // the same size
                            cases.add(null);
                            continue nextDocument;
                        }
                    }

                    // Reads the user's annotation document or the initial source document -
                    // depending on what is available
                    cas = documentService.readAnnotationCas(document, user.getUsername(),
                            AUTO_CAS_UPGRADE, SHARED_READ_ONLY_ACCESS);
                }
                catch (Exception e) {
                    error("Unable to load data: " + ExceptionUtils.getRootCauseMessage(e));
                    LOG.error("Unable to load data", e);
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

            cachedCASes.put(user.getUsername(), cases);
        }

        cachedProject = project;
        cachedLimitToFinishedDocuments = traits.isLimitToFinishedDocuments();

        return cachedCASes;
    }
}
