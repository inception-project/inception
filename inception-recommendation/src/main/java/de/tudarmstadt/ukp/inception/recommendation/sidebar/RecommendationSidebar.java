/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
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
package de.tudarmstadt.ukp.inception.recommendation.sidebar;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.event.annotation.OnEvent;

import com.googlecode.wicket.jquery.core.Options;
import com.googlecode.wicket.kendo.ui.widget.tooltip.TooltipBehavior;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.event.RenderAnnotationsEvent;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModelAdapter;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Preferences;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.api.recommender.RecommendationEngineFactory;

public class RecommendationSidebar
    extends AnnotationSidebar_ImplBase
{
    private static final long serialVersionUID = 4306746527837380863L;
    
    private static final String LEARNING_CURVE = "learningCurve";
    
    private @SpringBean RecommendationService recommendationService;
    private @SpringBean AnnotationSchemaService annoService;
    private @SpringBean UserDao userRepository;

    private WebMarkupContainer warning;
    private StringResourceModel tipModel;
    private Form<Preferences> form;
    private RecommenderInfoPanel recommenderInfos;
    
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
        add(warning);
        tipModel = new StringResourceModel("mismatch", this);
        TooltipBehavior tip = new TooltipBehavior(tipModel);
        tip.setOption("width", Options.asString("300px"));
        warning.add(tip);
        
        form = new Form<>("form", CompoundPropertyModel.of(modelPreferences));

        form.add(new NumberTextField<Integer>("maxPredictions", Integer.class)
                .setMinimum(1)
                .setMaximum(10)
                .setStep(1));

        form.add(new CheckBox("showAllPredictions"));

        form.add(new LambdaAjaxLink("retrain", this::actionRetrain));

        form.add(new LambdaAjaxButton<>("save", (_target, _form) -> 
                aAnnotationPage.actionRefreshDocument(_target)));

        add(form);

        add(new LearningCurveChartPanel(LEARNING_CURVE, aModel));
        
        recommenderInfos = new RecommenderInfoPanel("recommenders", aModel);
        add(recommenderInfos);
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
        
        String recommendersStr = mismatchedRecommenders.stream()
                .collect(Collectors.joining(", "));
        tipModel.setParameters(recommendersStr);
        warning.setVisible(true);
    }
    
    @OnEvent
    public void onRenderAnnotations(RenderAnnotationsEvent aEvent)
    {
        aEvent.getRequestHandler().add(warning);
    }

    private void actionRetrain(AjaxRequestTarget aTarget)
    {
        AnnotatorState state = getModelObject();
        recommendationService.clearState(state.getUser().getUsername());
        recommendationService.triggerTrainingAndClassification(state.getUser().getUsername(),
                state.getProject(), "User request via sidebar");
    }
    
    private List<String> findMismatchedRecommenders()
    {
        List<String> mismatchedRecommenderNames = new ArrayList<>();
        Project project = getModelObject().getProject();
        for (AnnotationLayer layer : annoService.listAnnotationLayer(project)) {
            if (!layer.isEnabled()) {
                continue;
            }
            for (Recommender recommender : recommendationService
                    .listEnabledRecommenders(layer)) {
                RecommendationEngineFactory<?> factory = recommendationService
                        .getRecommenderFactory(recommender);
                if (!factory.accepts(recommender.getLayer(), recommender.getFeature())) {
                    mismatchedRecommenderNames.add(recommender.getName());
                }
            }
        }
        return mismatchedRecommenderNames;
    }
}
