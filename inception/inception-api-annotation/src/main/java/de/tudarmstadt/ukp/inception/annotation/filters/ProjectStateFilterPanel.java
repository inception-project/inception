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
package de.tudarmstadt.ukp.inception.annotation.filters;

import static java.util.Arrays.asList;
import static org.apache.wicket.event.Broadcast.BUBBLE;

import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;

import de.tudarmstadt.ukp.clarin.webanno.model.ProjectState;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.wicket.SymbolLabel;

public class ProjectStateFilterPanel
    extends GenericPanel<List<ProjectState>>
{
    private static final long serialVersionUID = 8632088780379498284L;

    private static final String MID_LABEL = "label";

    public ProjectStateFilterPanel(String aId, IModel<List<ProjectState>> aModel)
    {
        this(aId, aModel, ProjectState.values());
    }

    public ProjectStateFilterPanel(String aId, IModel<List<ProjectState>> aModel,
            ProjectState... aStates)
    {
        super(aId, aModel);

        setOutputMarkupId(true);

        var listview = new ListView<>("stateFilter", asList(aStates))
        {
            private static final long serialVersionUID = -2292408105823066466L;

            @Override
            protected void populateItem(ListItem<ProjectState> aItem)
            {
                var state = aItem.getModelObject();
                LambdaAjaxLink link = new LambdaAjaxLink("stateFilterLink",
                        (_target -> actionApplyStateFilter(_target, state)));

                link.add(new SymbolLabel(MID_LABEL, aItem.getModel()));
                link.add(new AttributeAppender("class",
                        () -> aModel.getObject().contains(state) ? "active" : "", " "));
                link.add(AttributeModifier.replace("title",
                        new ResourceModel(state.getClass().getSimpleName() + "." + state.name())));
                aItem.add(link);
            }
        };

        queue(listview);
    }

    private void actionApplyStateFilter(AjaxRequestTarget aTarget, ProjectState aState)
    {
        List<ProjectState> selectedStates = getModel().getObject();
        if (selectedStates.contains(aState)) {
            selectedStates.remove(aState);
        }
        else {
            selectedStates.add(aState);
        }

        aTarget.add(this);

        send(this, BUBBLE, new ProjectStateFilterStateChanged(aTarget, selectedStates));
    }
}
