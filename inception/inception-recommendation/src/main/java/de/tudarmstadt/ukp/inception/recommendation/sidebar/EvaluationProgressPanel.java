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

import static org.apache.commons.lang3.StringUtils.repeat;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.event.PredictionsSwitchedEvent;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Progress;

public class EvaluationProgressPanel
    extends GenericPanel<Project>
{
    private static final long serialVersionUID = -8498000053224985486L;

    private @SpringBean UserDao userService;
    private @SpringBean RecommendationService recommendationService;

    public EvaluationProgressPanel(String aId, IModel<Project> aModel)
    {
        super(aId, aModel);

        setOutputMarkupId(true);

        var sessionOwner = userService.getCurrentUser();

        add(new Label("progress", LoadableDetachableModel.of(() -> {
            Progress p = recommendationService.getProgressTowardsNextEvaluation(sessionOwner,
                    getModelObject());
            return repeat("<i class=\"fas fa-circle\"></i>&nbsp;", p.getDone())
                    + repeat("<i class=\"far fa-circle\"></i>&nbsp;", p.getTodo());
        })).setEscapeModelStrings(false)); // SAFE - RENDERING ONLY SPECIFIC ICONS
    }

    @OnEvent
    public void onPredictionsSwitched(PredictionsSwitchedEvent aEvent)
    {
        aEvent.getRequestTarget().ifPresent(target -> target.add(this));
    }
}
