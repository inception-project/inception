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

import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;

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
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.event.annotation.OnEvent;

import com.googlecode.wicket.jquery.core.Options;
import com.googlecode.wicket.kendo.ui.widget.tooltip.TooltipBehavior;

import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModelAdapter;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessageGroup;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Preferences;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactory;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.request.RenderRequestedEvent;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.help.DocLink;

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

    public RecommendationSidebar(String aId, IModel<AnnotatorState> aModel,
            AnnotationActionHandler aActionHandler, CasProvider aCasProvider,
            AnnotationPage aAnnotationPage)
    {
        super(aId, aModel, aActionHandler, aCasProvider, aAnnotationPage);

        IModel<Preferences> modelPreferences = LambdaModelAdapter.of(
                () -> recommendationService.getPreferences(aModel.getObject().getUser(),
                        aModel.getObject().getProject()),
                (v) -> recommendationService.setPreferences(aModel.getObject().getUser(),
                        aModel.getObject().getProject(), v));

        warning = new WebMarkupContainer("warning");
        warning.setOutputMarkupPlaceholderTag(true);
        add(warning);
        tipModel = new StringResourceModel("mismatch", this);
        TooltipBehavior tip = new TooltipBehavior(tipModel);
        tip.setOption("width", Options.asString("300px"));
        warning.add(tip);

        Label noRecommendersLabel = new Label("noRecommendersLabel",
                new StringResourceModel("noRecommenders"));
        List<Recommender> recommenders = recommendationService
                .listEnabledRecommenders(aModel.getObject().getProject());
        noRecommendersLabel.add(visibleWhen(() -> recommenders.isEmpty()));
        add(noRecommendersLabel);

        add(new LambdaAjaxLink("showLog", this::actionShowLog)
                .add(visibleWhen(() -> !recommenders.isEmpty())));

        add(new LambdaAjaxLink("retrain", this::actionRetrain)
                .add(visibleWhen(() -> !recommenders.isEmpty())));

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
                .add(new LambdaAjaxFormComponentUpdatingBehavior("change",
                        _target -> _target.add(form))));

        form.add(new LambdaAjaxButton<>("save",
                (_target, _form) -> aAnnotationPage.actionRefreshDocument(_target)));
        form.add(visibleWhen(() -> !recommenders.isEmpty()));

        add(form);

        // add(new LearningCurveChartPanel(LEARNING_CURVE, aModel)
        // .add(visibleWhen(() -> !recommenders.isEmpty())));

        recommenderInfos = new RecommenderInfoPanel("recommenders", aModel);
        recommenderInfos.add(visibleWhen(() -> !recommenders.isEmpty()));
        add(recommenderInfos);

        logDialog = new LogDialog("logDialog");
        add(logDialog);

    }

    @Override
    protected void onConfigure()
    {
        // using onConfigure as last state in lifecycle to configure visibility
        super.onConfigure();
        configureMismatched();
        boolean enabled = getModelObject().getUser().equals(userRepository.getCurrentUser());
        form.setEnabled(enabled);
        recommenderInfos.setEnabled(enabled);
    }

    protected void configureMismatched()
    {
        List<String> mismatchedRecommenders = findMismatchedRecommenders();

        if (mismatchedRecommenders.isEmpty()) {
            warning.setVisible(false);
            return;
        }

        String recommendersStr = mismatchedRecommenders.stream().collect(Collectors.joining(", "));
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
        List<LogMessageGroup> messages = recommendationService
                .getLog(getModelObject().getUser().getUsername(), getModelObject().getProject());
        logDialog.setModel(new ListModel<LogMessageGroup>(messages));
        logDialog.show(aTarget);
    }

    private void actionRetrain(AjaxRequestTarget aTarget)
    {
        AnnotatorState state = getModelObject();
        recommendationService.clearState(state.getUser().getUsername());
        recommendationService.triggerSelectionTrainingAndPrediction(state.getUser().getUsername(),
                state.getProject(), "User request via sidebar", state.getDocument());
        info("Annotation state cleared - re-training from scratch...");
        getAnnotationPage().actionRefreshDocument(aTarget);
        aTarget.add(recommenderInfos);
        aTarget.addChildren(getPage(), IFeedback.class);
    }

    private List<String> findMismatchedRecommenders()
    {
        List<String> mismatchedRecommenderNames = new ArrayList<>();
        Project project = getModelObject().getProject();
        for (AnnotationLayer layer : annoService.listAnnotationLayer(project)) {
            if (!layer.isEnabled()) {
                continue;
            }
            for (Recommender recommender : recommendationService.listEnabledRecommenders(layer)) {
                RecommendationEngineFactory<?> factory = recommendationService
                        .getRecommenderFactory(recommender).orElse(null);

                // E.g. if the module providing a configured recommender has been disabled but the
                // recommender is still configured.
                if (factory == null) {
                    continue;
                }

                if (!factory.accepts(recommender.getLayer(), recommender.getFeature())) {
                    mismatchedRecommenderNames.add(recommender.getName());
                }
            }
        }
        return mismatchedRecommenderNames;
    }
}
