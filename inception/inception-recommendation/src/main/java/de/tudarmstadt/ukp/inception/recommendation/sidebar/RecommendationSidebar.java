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

import static de.tudarmstadt.ukp.inception.recommendation.api.RecommenderPredictionSources.RECOMMENDER_SOURCE;
import static de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents.CHANGE_EVENT;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhenNot;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.event.annotation.OnEvent;
import org.wicketstuff.jquery.core.Options;
import org.wicketstuff.kendo.ui.widget.tooltip.TooltipBehavior;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Preferences;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DocumentEditor;
import de.tudarmstadt.ukp.inception.rendering.request.RenderRequestedEvent;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.help.DocLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaModelAdapter;
import de.tudarmstadt.ukp.inception.support.logging.LogMessageGroup;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.SidebarContext;

public class RecommendationSidebar
    extends AnnotationSidebar_ImplBase
{
    private static final long serialVersionUID = 4306746527837380863L;

    private @SpringBean RecommendationService recommendationService;
    private @SpringBean AnnotationSchemaService annoService;
    private @SpringBean UserDao userRepository;

    private WebMarkupContainer warning;
    private StringResourceModel tipModel;
    private Form<Preferences> form;
    private RecommenderInfoPanel recommenderInfos;
    private LogDialog logDialog;

    public RecommendationSidebar(String aId, SidebarContext aContext)
    {
        super(aId, aContext);

        var mainContainer = new WebMarkupContainer("mainContainer");
        add(mainContainer);

        var sessionOwner = userRepository.getSessionOwner();
        var modelPreferences = LambdaModelAdapter.of(
                () -> recommendationService.getPreferences(sessionOwner, getProject()),
                (v) -> recommendationService.setPreferences(sessionOwner, getProject(), v));

        warning = new WebMarkupContainer("warning");
        warning.setOutputMarkupPlaceholderTag(true);
        add(warning);
        tipModel = new StringResourceModel("mismatch", this);
        var tip = new TooltipBehavior(tipModel);
        tip.setOption("width", Options.asString("300px"));
        warning.add(tip);

        var noRecommendersLabel = new Label("noRecommendersLabel",
                new StringResourceModel("noRecommenders"));
        var recommenders = recommendationService.listEnabledRecommenders(getProject());
        noRecommendersLabel.add(visibleWhen(() -> recommenders.isEmpty()));
        add(noRecommendersLabel);

        add(new LambdaAjaxLink("showLog", this::actionShowLog)
                .add(visibleWhenNot(recommenders::isEmpty)));

        add(new LambdaAjaxLink("retrain", this::actionRetrain)
                .add(visibleWhenNot(recommenders::isEmpty)));

        var modelEnabled = LambdaModelAdapter.of(
                () -> !recommendationService.isSuspended(sessionOwner.getUsername(), getProject()),
                (v) -> recommendationService.setSuspended(sessionOwner.getUsername(), getProject(),
                        !v));
        mainContainer.add(new CheckBox("enabled", modelEnabled).setOutputMarkupId(true)
                .add(new LambdaAjaxFormComponentUpdatingBehavior(CHANGE_EVENT)));
        mainContainer.add(new EvaluationProgressPanel("progress", Model.of(getProject())));

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

        form.add(new LambdaAjaxButton<>("save", (_target, _form) -> getActiveEditor()
                .ifPresent(editor -> editor.actionRefreshDocument(_target))));
        form.add(visibleWhen(() -> !recommenders.isEmpty()));

        add(form);

        recommenderInfos = new RecommenderInfoPanel("recommenders", Model.of(getProject()),
                getDocumentEditorManager());
        recommenderInfos.add(visibleWhen(() -> !recommenders.isEmpty()));
        mainContainer.add(recommenderInfos);

        logDialog = new LogDialog("logDialog");
        add(logDialog);
    }

    @Override
    protected void onConfigure()
    {
        // using onConfigure as last state in lifecycle to configure visibility
        super.onConfigure();

        configureMismatched();
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
        // Only react to renders of the editor this sidebar belongs to, not to renders of other
        // editors on the page even if they show the same document (#6146).
        var state = getModelObject();
        if (state == null || !aEvent.isFor(state)) {
            return;
        }

        aEvent.getRequestHandler().add(warning);
    }

    private void actionShowLog(AjaxRequestTarget aTarget)
    {
        var messages = recommendationService.getLog(userRepository.getCurrentUsername(),
                getProject(), RECOMMENDER_SOURCE);
        logDialog.setModel(new ListModel<LogMessageGroup>(messages));
        logDialog.show(aTarget);
    }

    private void actionRetrain(AjaxRequestTarget aTarget)
    {
        var sessionOwner = userRepository.getSessionOwner();

        // Recommenders train on the session owner's data only, whichever editor is active
        var document = getActiveEditor() //
                .map(DocumentEditor::getAnnotatorState) //
                .filter(state -> sessionOwner.equals(state.getUser())) //
                .map(AnnotatorState::getDocument) //
                .orElse(null);

        recommendationService.resetState(sessionOwner.getUsername());
        recommendationService.triggerSelectionTrainingAndPrediction(sessionOwner.getUsername(),
                getProject(), "User request via sidebar", document, sessionOwner.getUsername());

        info("Annotation state cleared - re-training from scratch...");
        getActiveEditor().ifPresent(editor -> editor.actionRefreshDocument(aTarget));
        aTarget.add(recommenderInfos);
        aTarget.addChildren(getPage(), IFeedback.class);
    }

    private List<String> findMismatchedRecommenders()
    {
        var mismatchedRecommenderNames = new ArrayList<String>();
        var project = getProject();
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
