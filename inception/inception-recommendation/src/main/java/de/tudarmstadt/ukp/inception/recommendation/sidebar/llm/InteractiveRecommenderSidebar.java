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
package de.tudarmstadt.ukp.inception.recommendation.sidebar.llm;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageBase2;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.SuggestionSupportRegistry;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;
import de.tudarmstadt.ukp.inception.recommendation.tasks.PredictionTask;
import de.tudarmstadt.ukp.inception.scheduling.SchedulingService;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;

public class InteractiveRecommenderSidebar
    extends AnnotationSidebar_ImplBase
{
    private static final long serialVersionUID = -1;

    private static final String MID_FORM = "form";
    private static final String MID_TRAITS_CONTAINER = "traitsContainer";
    private static final String MID_TRAITS = "traits";
    private static final String MID_EXECUTE = "execute";

    private @SpringBean UserDao userService;
    private @SpringBean RecommendationService recommendationService;
    private @SpringBean AnnotationSchemaService schemaService;
    private @SpringBean SuggestionSupportRegistry suggestionSupportRegistry;
    private @SpringBean SchedulingService schedulingService;

    private WebMarkupContainer traitsContainer;

    private IModel<Recommender> recommender;

    public InteractiveRecommenderSidebar(String aId, AnnotationActionHandler aActionHandler, CasProvider aCasProvider,
            AnnotationPageBase2 aAnnotationPage)
    {
        super(aId, aActionHandler, aCasProvider, aAnnotationPage);

        var interactiveRecommenders = listInteractiveRecommenders();
        if (interactiveRecommenders.isEmpty()) {
            recommender = Model.of();
        }
        else {
            recommender = Model.of(interactiveRecommenders.get(0));
        }

        var form = new Form<>(MID_FORM, CompoundPropertyModel.of(recommender));
        add(form);

        form.add(traitsContainer = new WebMarkupContainer(MID_TRAITS_CONTAINER));
        traitsContainer.setOutputMarkupPlaceholderTag(true);

        if (recommender.isPresent().getObject()) {
            var factory = recommendationService.getRecommenderFactory(recommender.getObject());
            if (factory.isPresent()) {
                traitsContainer.addOrReplace( //
                        factory.get().createInteractionPanel(MID_TRAITS, form.getModel()));
            }
        }
        else {
            traitsContainer.add(new EmptyPanel(MID_TRAITS));
        }

        form.add(new LambdaAjaxButton<Recommender>(MID_EXECUTE, this::execute));
    }

    private List<Recommender> listInteractiveRecommenders()
    {
        var state = getAnnotationPage().getModelObject();
        return recommendationService.listRecommenders(state.getProject()).stream() //
                .filter(rec -> rec.isEnabled()) //
                .filter(rec -> recommendationService.getRecommenderFactory(rec)
                        .map(factory -> factory.isInteractive(rec)).orElse(false)) //
                .toList();
    }

    private void execute(AjaxRequestTarget aTarget, Form<Recommender> aForm) throws Exception
    {
        var sessionOwner = userService.getCurrentUser();
        var state = getAnnotationPage().getModelObject();
        var document = state.getDocument();
        var dataOwner = state.getUser().getUsername();

        var predictionTask = PredictionTask.builder() //
                .withSessionOwner(sessionOwner) //
                .withTrigger("Interactivce recommender") //
                .withCurrentDocument(document) //
                .withDataOwner(dataOwner) //
                .withRecommender(recommender.getObject()) //
                .build();

        schedulingService.enqueue(predictionTask);
    }
}
