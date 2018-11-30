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

import java.io.IOException;
import java.util.List;

import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.JCasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.rendering.event.RenderAnnotationsEvent;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModelAdapter;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.inception.log.EventRepository;
import de.tudarmstadt.ukp.inception.log.model.LoggedEvent;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Preferences;
import de.tudarmstadt.ukp.inception.recommendation.log.RecommenderEvaluationResultEventAdapter.Details;
import de.tudarmstadt.ukp.inception.recommendation.sidebar.resource.C3CssReference;
import de.tudarmstadt.ukp.inception.recommendation.sidebar.resource.C3JsReference;
import de.tudarmstadt.ukp.inception.recommendation.sidebar.resource.D3JsReference;

public class RecommendationSidebar
    extends AnnotationSidebar_ImplBase
{
    private static final long serialVersionUID = 4306746527837380863L;
    
	private Logger log = LoggerFactory.getLogger(getClass());
	private WebComponent chartContainer;

    private @SpringBean RecommendationService recommendationService;
    private @SpringBean EventRepository eventRepo;


    public RecommendationSidebar(String aId, IModel<AnnotatorState> aModel,
            AnnotationActionHandler aActionHandler, JCasProvider aJCasProvider,
            AnnotationPage aAnnotationPage)
    {
        super(aId, aModel, aActionHandler, aJCasProvider, aAnnotationPage);

        IModel<Preferences> model = LambdaModelAdapter.of(
            () -> recommendationService.getPreferences(aModel.getObject().getUser(), 
                    aModel.getObject().getProject()),
            (v) -> recommendationService.setPreferences(aModel.getObject().getUser(), 
                    aModel.getObject().getProject(), v));

        Form<Preferences> form = new Form<>("form",
                CompoundPropertyModel.of(model));

        form.add(new NumberTextField<Integer>("maxPredictions", Integer.class)
                .setMinimum(1)
                .setMaximum(10)
                .setStep(1));

        form.add(new CheckBox("showAllPredictions"));

        form.add(new LambdaAjaxButton<>("save", (_target, _form) -> 
                aAnnotationPage.actionRefreshDocument(_target)));
        
        add(form);

		chartContainer = new Label("chart-container" );
		chartContainer.setOutputMarkupId(true);
		add(chartContainer);
		
		add(new AbstractAjaxTimerBehavior(Duration.milliseconds(15000)) {
			private static final long serialVersionUID = -3782208159226605584L;
			
			@Override
			protected void onTimer(AjaxRequestTarget aTarget) {

				//we want to plot RecommenderEvaluationResultEvent for the learning curve
				String eventType = "RecommenderEvaluationResultEvent";
				List<LoggedEvent> loggedEvents = eventRepo.listLoggedEvents(aModel.getObject().getProject(),
						aModel.getObject().getUser().getUsername(),eventType);

				// iterate over the logged events to extract the scores only.
				double[] listDetailJson = new double[loggedEvents.size()];

				int index = 0;
				for (LoggedEvent loggedEvent : loggedEvents) {
					String detailJson = loggedEvent.getDetails();

					try {
						Details detail = JSONUtil.fromJsonString(Details.class, detailJson);

						try {
							double scoreDouble = Double.valueOf(detail.score);
							listDetailJson[index] = scoreDouble;
							index++;
							 
						} catch (NumberFormatException e) {
							log.debug(
									"Skipping logged Event due to invalid score. Skipping record with logged event id:"
											+ loggedEvent.getId());
							aTarget.addChildren(getPage(), IFeedback.class);
							continue;
						}

					} catch (IOException e) {
						log.error(e.toString(), e);

						error("Invalid logged Event detail. Skipping record with logged event id: " + loggedEvent.getId());
						aTarget.addChildren(getPage(), IFeedback.class);
					}
				}

				String data = "";
				for (Double integer : listDetailJson) {
					data+= ",";
					data += integer;
				}

				String javascript = "var chart=c3.generate({bindto:'#"+ chartContainer.getMarkupId() +"',data:{columns:[[\"data1\""+data +"]]}});;";
				
				System.out.println(javascript);
				aTarget.prependJavaScript(javascript);
			}
		});
	}
     

	private String getJson() {
		// TODO Auto-generated method stub
		return "";
	}

	

	@Override
	public void renderHead(IHeaderResponse aResponse) {
		super.renderHead(aResponse);

		//import Css
		aResponse.render(JavaScriptHeaderItem.forReference(C3JsReference.get()));
		aResponse.render(JavaScriptHeaderItem.forReference(D3JsReference.get()));

		//import Css
		aResponse.render(CssHeaderItem.forReference(C3CssReference.get()));
	}
}

