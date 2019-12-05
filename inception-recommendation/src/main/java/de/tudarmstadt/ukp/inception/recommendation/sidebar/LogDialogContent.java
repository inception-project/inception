/*
 * Copyright 2019
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

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessageGroup;

public class LogDialogContent
    extends Panel
{
    private static final long serialVersionUID = -5003560112554715634L;
    
    private final ModalWindow modalWindow;
    
    public LogDialogContent(String aId, final ModalWindow aModalWindow,
            IModel<List<LogMessageGroup>> aModel)
    {
        super(aId, aModel);
        
        setOutputMarkupId(true);

        modalWindow = aModalWindow;

        add(createMessageSetsView(getModel()));
        add(new LambdaAjaxLink("close", this::actionCancel));
    }
    
    public IModel<List<LogMessageGroup>> getModel()
    {
        return (IModel<List<LogMessageGroup>>) getDefaultModel();
    }

    private ListView<LogMessageGroup> createMessageSetsView(IModel<List<LogMessageGroup>> aModel)
    {
        return new ListView<LogMessageGroup>("messageSets", aModel)
        {
            private static final long serialVersionUID = 6196874659414792428L;

            @Override
            protected void populateItem(ListItem<LogMessageGroup> aItem)
            {
                IModel<LogMessageGroup> set = aItem.getModel();
                aItem.add(new Label("name", PropertyModel.of(set, "name")));
                aItem.add(createMessagesView(set));
            }
        };
    }
    
    private ListView<LogMessage> createMessagesView(IModel<LogMessageGroup> aModel)
    {
        return new ListView<LogMessage>("messages", PropertyModel.of(aModel, "messages"))
        {
            private static final long serialVersionUID = 5961113080333988246L;

            @Override
            protected void populateItem(ListItem<LogMessage> aItem)
            {
                IModel<LogMessage> msg = aItem.getModel();
                aItem.add(new Label("level", PropertyModel.of(msg, "level")));
                aItem.add(new Label("source", PropertyModel.of(msg, "source")));
                aItem.add(new Label("message", PropertyModel.of(msg, "message")));
            }
        };
    }
    private void actionCancel(AjaxRequestTarget aTarget)
    {
        modalWindow.close(aTarget);
    }
}
