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
package de.tudarmstadt.ukp.inception.recommendation.sidebar;

import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.KEY_RECOMMENDER_GENERAL_SETTINGS;
import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CHANGE_EVENT;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhenNot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.event.annotation.OnEvent;
import org.wicketstuff.jquery.core.Options;
import org.wicketstuff.kendo.ui.widget.tooltip.TooltipBehavior;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageBase2;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Preferences;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.request.RenderRequestedEvent;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.help.DocLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaModelAdapter;
import de.tudarmstadt.ukp.inception.support.logging.LogMessageGroup;

public class RecommendationSidebar
    extends AnnotationSidebar_ImplBase
{
    private static final long serialVersionUID = 4306746527837380863L;

    private @SpringBean RecommendationService recommendationService;
    private @SpringBean AnnotationSchemaService annoService;
    private @SpringBean UserDao userRepository;
    private @SpringBean PreferencesService preferencesService;

    private IModel<Boolean> recommendersAvailable;
    private WebMarkupContainer warning;
    private StringResourceModel tipModel;
    private Form<Preferences> form;
    private RecommenderInfoPanel recommenderInfos;
    private LogDialog logDialog;

    public RecommendationSidebar(String aId, AnnotationActionHandler aActionHandler,
            CasProvider aCasProvider, AnnotationPageBase2 aAnnotationPage)
    {
        super(aId, aActionHandler, aCasProvider, aAnnotationPage);

        recommendersAvailable = LoadableDetachableModel.of(this::isRecommendersAvailable);

        var mainContainer = new WebMarkupContainer("mainContainer");
        mainContainer.add(visibleWhen(recommendersAvailable));
        add(mainContainer);

        var sessionOwner = userRepository.getCurrentUser();
        var modelPreferences = LambdaModelAdapter.of(
                () -> recommendationService.getPreferences(sessionOwner,
                        aAnnotationPage.getModelObject().getProject()),
                (v) -> recommendationService.setPreferences(sessionOwner,
                        aAnnotationPage.getModelObject().getProject(), v));

        warning = new WebMarkupContainer("warning");
        warning.setOutputMarkupPlaceholderTag(true);
        add(warning);
        tipModel = new StringResourceModel("mismatch", this);
        var tip = new TooltipBehavior(tipModel);
        tip.setOption("width", Options.asString("300px"));
        warning.add(tip);

        var noRecommendersLabel = new Label("noRecommendersLabel",
                new StringResourceModel("noRecommenders"));
        var recommenders = recommendationService
                .listEnabledRecommenders(aAnnotationPage.getModelObject().getProject());
        noRecommendersLabel.add(visibleWhen(() -> recommenders.isEmpty()));
        add(noRecommendersLabel);

        var notAvailableNotice = new WebMarkupContainer("notAvailableNotice");
        notAvailableNotice.add(visibleWhenNot(recommendersAvailable));
        add(notAvailableNotice);

        add(new LambdaAjaxLink("showLog", this::actionShowLog)
                .add(visibleWhenNot(recommenders::isEmpty)));

        add(new LambdaAjaxLink("retrain", this::actionRetrain)
                .add(visibleWhenNot(recommenders::isEmpty)));

        var modelEnabled = LambdaModelAdapter.of(
                () -> !recommendationService.isSuspended(sessionOwner.getUsername(),
                        aAnnotationPage.getModelObject().getProject()),
                (v) -> recommendationService.setSuspended(sessionOwner.getUsername(),
                        aAnnotationPage.getModelObject().getProject(), !v));
        mainContainer.add(new CheckBox("enabled", modelEnabled).setOutputMarkupId(true)
                .add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT)));
        mainContainer.add(new EvaluationProgressPanel("progress",
                aAnnotationPage.getModel().map(AnnotatorState::getProject)));

        form = new Form<>("form", CompoundPropertyModel.of(modelPreferences));
        form.setOutputMarkupId(true);

        form.add(new DocLink("maxSuggestionsHelpLink", "_recommendation_sidebar"));

        form.add(new NumberTextField<Integer>("maxPredictions", Integer.class) //
                .setMinimum(1).setMaximum(10).setStep(1) //
                .add(visibleWhen(() -> !form.getModelObject().isShowAllPredictions())));

        form.add(new NumberTextField<Double>("scoreThreshold", Double.class) //
                .setStep(0.1d) //
                .add(visibleWhen(() -> !form.getModelObject().isShowAllPredictions())));

        form.add(new CheckBox("showAllPredictions").setOutputMarkupId(true)
                .add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT,
                        _target -> _target.add(form))));

        form.add(new LambdaAjaxButton<>("save",
                (_target, _form) -> aAnnotationPage.actionRefreshDocument(_target)));
        form.add(visibleWhen(() -> !recommenders.isEmpty()));

        add(form);

        recommenderInfos = new RecommenderInfoPanel("recommenders", aAnnotationPage.getModel());
        recommenderInfos.add(visibleWhen(() -> !recommenders.isEmpty()));
        mainContainer.add(recommenderInfos);

        logDialog = new LogDialog("logDialog");
        add(logDialog);
    }

    @Override
    protected void onDetach()
    {
        super.onDetach();
        recommendersAvailable.detach();
    }

    @Override
    protected void onConfigure()
    {
        // using onConfigure as last state in lifecycle to configure visibility
        super.onConfigure();
        configureMismatched();
        var enabled = getModelObject().getUser().equals(userRepository.getCurrentUser());
        form.setEnabled(enabled);
        recommenderInfos.setEnabled(enabled);
    }

    private boolean isRecommendersAvailable()
    {
        var state = getModelObject();
        var prefs = preferencesService.loadDefaultTraitsForProject(KEY_RECOMMENDER_GENERAL_SETTINGS,
                state.getProject());

        // Do not show predictions when viewing annotations of another user
        if (!prefs.isShowRecommendationsWhenViewingOtherUser()
                && !Objects.equals(state.getUser(), userRepository.getCurrentUser())) {
            return false;
        }

        // Do not show predictions when viewing annotations of curation user
        if (!prefs.isShowRecommendationsWhenViewingCurationUser()
                && Objects.equals(state.getUser(), userRepository.getCurationUser())) {
            return false;
        }

        return true;
    }

    protected void configureMismatched()
    {
        var mismatchedRecommenders = findMismatchedRecommenders();

        if (mismatchedRecommenders.isEmpty()) {
            warning.setVisible(false);
            return;
        }

        var recommendersStr = mismatchedRecommenders.stream().collect(Collectors.joining(", "));
        tipModel.setParameters(recommendersStr);
        warning.setVisible(true);
    }

    @OnEvent
    public void onRenderRequested(RenderRequestedEvent aEvent)
    {
        aEvent.getRequestHandler().add(warning);
    }

    private void actionShowLog(AjaxRequestTarget aTarget)
    {
        var messages = recommendationService.getLog(userRepository.getCurrentUsername(),
                getModelObject().getProject());
        logDialog.setModel(new ListModel<LogMessageGroup>(messages));
        logDialog.show(aTarget);
    }

    private void actionRetrain(AjaxRequestTarget aTarget)
    {
        var state = getModelObject();
        var sessionOwner = userRepository.getCurrentUsername();
        var dataOwner = state.getUser().getUsername();

        recommendationService.resetState(sessionOwner);
        recommendationService.triggerSelectionTrainingAndPrediction(sessionOwner,
                state.getProject(), "User request via sidebar", state.getDocument(), dataOwner);

        info("Annotation state cleared - re-training from scratch...");
        getAnnotationPage().actionRefreshDocument(aTarget);
        aTarget.add(recommenderInfos);
        aTarget.addChildren(getPage(), IFeedback.class);
    }

    private List<String> findMismatchedRecommenders()
    {
        var mismatchedRecommenderNames = new ArrayList<String>();
        var project = getModelObject().getProject();
        for (var layer : annoService.listAnnotationLayer(project)) {
            if (!layer.isEnabled()) {
                continue;
            }
            for (var recommender : recommendationService.listEnabledRecommenders(layer)) {
                var factory = recommendationService.getRecommenderFactory(recommender).orElse(null);

                // E.g. if the module providing a configured recommender has been disabled but the
                // recommender is still configured.
                if (factory == null) {
                    continue;
                }

                if (!factory.accepts(recommender)) {
                    mismatchedRecommenderNames.add(recommender.getName());
                }
            }
        }
        return mismatchedRecommenderNames;
    }
}
