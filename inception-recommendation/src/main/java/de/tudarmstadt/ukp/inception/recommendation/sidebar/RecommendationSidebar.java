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
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.tudarmstadt.ukp.clarin.webanno.api.JCasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModelAdapter;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.inception.log.model.LoggedEvent;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Preferences;
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
		add(chartContainer);

		add(new AbstractAjaxTimerBehavior(Duration.milliseconds(5000)) {
			private static final long serialVersionUID = -3782208159226605584L;

			@Override
			protected void onTimer(AjaxRequestTarget target) {

				List<LoggedEvent> loggedEvents = recommendationService.listLoggedEvents(aModel.getObject().getProject(),
						aModel.getObject().getUser());

				// iterate over the logged events to extract the scores only.
				List<Double> listDetailJson = new ArrayList<Double>();

				for (LoggedEvent loggedEvent : loggedEvents) {
					String detailJson = loggedEvent.getDetails();

					ObjectMapper mapper = new ObjectMapper();
					Double scoreDouble;
					try {
						LoggedEventDetail detail = mapper.readValue(detailJson, LoggedEventDetail.class);

						try {
							scoreDouble = Double.valueOf(detail.getScore());
							listDetailJson.add(scoreDouble);
						} catch (NumberFormatException e) {
							log.debug("Skipping logged Event due to invalid score, Detail:{}", detail);
							continue;
						}

					} catch (IOException e) {
						log.error(e.toString(), e);
					}
				}

				String data = "";
				for (Double integer : listDetailJson) {
					data+= ",";
					data += integer;
				}

				String javascript = "var chart=c3.generate({bindto:\"#chart-container-js\",data:{columns:[[\"data1\""+data +"]]}});;";
				target.prependJavaScript(javascript);
			}
		});
	}

	private String getJson() {
		// TODO Auto-generated method stub
		return "";
	}

	@Override
	protected void onAfterRender() {
		super.onAfterRender();
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

//TODO: Move it to a separate file?
@JsonIgnoreProperties(ignoreUnknown = true)
class LoggedEventDetail {

	String score;
	String tool;

	public String getScore() {
		return score;
	}

	public void setScore(String score) {
		this.score = score;
	}

	public String getTool() {
		return tool;
	}

	public void setTool(String tool) {
		this.tool = tool;
	}
}
