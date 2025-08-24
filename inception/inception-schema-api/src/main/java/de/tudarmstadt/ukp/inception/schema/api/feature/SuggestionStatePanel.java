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
package de.tudarmstadt.ukp.inception.schema.api.feature;

import static java.lang.String.format;
import static org.apache.commons.collections4.CollectionUtils.containsAny;

import java.util.Objects;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.SuggestionState;

public class SuggestionStatePanel
    extends GenericPanel<FeatureState>
{
    private static final long serialVersionUID = -1639295549080031870L;

    private @SpringBean FeatureSupportRegistry featureSupportRegistry;
    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userService;

    public SuggestionStatePanel(String aId, IModel<FeatureState> aModel)
    {
        super(aId, aModel);

        var sessionOwner = userService.getCurrentUsername();

        var projectRoles = aModel.map(FeatureState::getFeature) //
                .map(AnnotationFeature::getProject) //
                .map(project -> projectService.listProjectPermissionLevel(sessionOwner, project)
                        .stream().map(ProjectPermission::getLevel).toList());

        var rolesSeeingSuggestionInfo = aModel.map(FeatureState::getFeature) //
                .map(f -> featureSupportRegistry.readTraits(f, () -> null)) //
                .map(t -> (RecommendableFeatureTrait) t) //
                .map(t -> t.getRolesSeeingSuggestionInfo());

        setVisible(projectRoles //
                .map(r1 -> rolesSeeingSuggestionInfo.map(r2 -> containsAny(r1, r2)).getObject())
                .getObject());

        add(new ListView<SuggestionState>("suggestion", aModel.map(FeatureState::getSuggestions))
        {
            private static final long serialVersionUID = 8210108803328906169L;

            @Override
            protected void populateItem(ListItem<SuggestionState> aItem)
            {
                var feature = aModel.getObject().getFeature();
                var fs = featureSupportRegistry.findExtension(feature).get();

                var featureValue = aModel.getObject().getValue();
                var suggestionValue = aItem.getModel().map(SuggestionState::value).getObject();

                var valueVisible = !Objects.equals(featureValue, suggestionValue);

                if (valueVisible) {
                    if (suggestionValue == null) {
                        suggestionValue = "no label";
                    }
                    else {
                        suggestionValue = fs.renderWrappedFeatureValue(suggestionValue);
                    }
                    aItem.add(new Label("value", suggestionValue));
                }
                else {
                    aItem.add(new Label("value").setVisible(false));
                }

                aItem.add(new Label("recommender",
                        aItem.getModel().map(SuggestionState::recommender)));
                var score = new Label("score", aItem.getModel().map(SuggestionState::score) //
                        .map(s -> format("%.2f", s)));
                score.add(AttributeModifier.replace("title",
                        aItem.getModel().map(SuggestionState::recommender)));
                aItem.add(score);
            }
        });
    }
}
