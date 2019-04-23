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

import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.JCasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.event.RenderAnnotationsEvent;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
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

    public RecommendationSidebar(String aId, IModel<AnnotatorState> aModel,
            AnnotationActionHandler aActionHandler, JCasProvider aJCasProvider,
            AnnotationPage aAnnotationPage)
    {
        super(aId, aModel, aActionHandler, aJCasProvider, aAnnotationPage);
        IModel<Preferences> modelPreferences = LambdaModelAdapter.of(
            () -> recommendationService.getPreferences(aModel.getObject().getUser(),
                    aModel.getObject().getProject()),
            (v) -> recommendationService.setPreferences(aModel.getObject().getUser(),
                    aModel.getObject().getProject(), v));

        Form<Preferences> form = new Form<>("form", CompoundPropertyModel.of(modelPreferences));

        form.add(new NumberTextField<Integer>("maxPredictions", Integer.class)
                .setMinimum(1)
                .setMaximum(10)
                .setStep(1));

        form.add(new CheckBox("showAllPredictions"));

        form.add(new LambdaAjaxButton<>("save", (_target, _form) -> 
                aAnnotationPage.actionRefreshDocument(_target)));

        add(form);

        LearningCurveChartPanel chartContainer = new LearningCurveChartPanel(LEARNING_CURVE,aModel);
        chartContainer.setVisibilityAllowed(recommendationService.showLearningCurveDiagram());
        add(chartContainer);
    }

    @OnEvent
    /**
     * Inform whether recommender and annotation layers are a valid match.
     */
    public void onRenderAnnotations(RenderAnnotationsEvent aEvent)
    {
        Project project = getModelObject().getProject();
        for (AnnotationLayer layer : annoService.listAnnotationLayer(project)) {
            if (!layer.isEnabled()) {
                continue;
            }
            for (Recommender recommender : recommendationService.listEnabledRecommenders(layer)) {
                RecommendationEngineFactory<?> factory = recommendationService
                        .getRecommenderFactory(recommender);
                if (!factory.accepts(recommender.getLayer(), recommender.getFeature())) {
                    error(String.format("The recommender %s is configured for an invalid layer "
                            + "and therefore skipped.", recommender.getName()));
                    aEvent.getRequestHandler().addChildren(getPage(), IFeedback.class);
                }
            }
        }
    }
}
