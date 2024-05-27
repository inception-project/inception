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
package de.tudarmstadt.ukp.clarin.webanno.ui.core.users;

import static java.util.Arrays.asList;
import static org.apache.wicket.event.Broadcast.BUBBLE;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.wicket.SymbolLabel;

public class UserStateFilterPanel
    extends Panel
{
    private static final long serialVersionUID = 8632088780379498284L;

    private static final String MID_LABEL = "label";

    public UserStateFilterPanel(String aId, IModel<List<UserState>> aModel)
    {
        this(aId, aModel, UserState.values());
    }

    public UserStateFilterPanel(String aId, IModel<List<UserState>> aModel, UserState... aStates)
    {
        super(aId, aModel);

        var listview = new ListView<>("stateFilter", asList(aStates))
        {
            private static final long serialVersionUID = -2292408105823066466L;

            @Override
            protected void populateItem(ListItem<UserState> aItem)
            {
                LambdaAjaxLink link = new LambdaAjaxLink("stateFilterLink",
                        (_target -> actionApplyStateFilter(_target, aItem.getModelObject())));

                link.add(new SymbolLabel(MID_LABEL, aItem.getModel()));
                link.add(new AttributeAppender("class",
                        () -> aModel.getObject().contains(aItem.getModelObject()) ? "active" : "",
                        " "));
                aItem.add(link);
            }
        };

        queue(listview);
    }

    @SuppressWarnings("unchecked")
    public IModel<List<UserState>> getModel()
    {
        return (IModel<List<UserState>>) getDefaultModel();
    }

    private void actionApplyStateFilter(AjaxRequestTarget aTarget, UserState aState)
    {
        List<UserState> selectedStates = getModel().getObject();
        if (selectedStates.contains(aState)) {
            selectedStates.remove(aState);
        }
        else {
            selectedStates.add(aState);
        }

        send(this, BUBBLE, new UserStateFilterChangedEvent(aTarget, selectedStates));
    }
}
