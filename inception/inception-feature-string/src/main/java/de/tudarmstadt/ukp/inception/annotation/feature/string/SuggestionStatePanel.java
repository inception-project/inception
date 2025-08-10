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
package de.tudarmstadt.ukp.inception.annotation.feature.string;

import static java.lang.String.format;

import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.inception.rendering.editorstate.SuggestionState;

public class SuggestionStatePanel
    extends GenericPanel<List<SuggestionState>>
{
    private static final long serialVersionUID = -1639295549080031870L;

    public SuggestionStatePanel(String aId, IModel<List<SuggestionState>> aModel)
    {
        super(aId, aModel);

        add(new ListView<SuggestionState>("suggestion", aModel)
        {
            private static final long serialVersionUID = 8210108803328906169L;

            @Override
            protected void populateItem(ListItem<SuggestionState> aItem)
            {
                aItem.add(new Label("value",
                        aItem.getModel().map(SuggestionState::value).orElse("no label")));

                var score = new Label("score", aItem.getModel().map(SuggestionState::score) //
                        .map(s -> format("%.2f", s)));
                score.add(AttributeModifier.replace("title",
                        aItem.getModel().map(SuggestionState::recommender)));
                aItem.add(score);
            }
        });
    }
}
