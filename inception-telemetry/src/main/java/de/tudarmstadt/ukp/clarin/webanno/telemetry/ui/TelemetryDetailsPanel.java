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
package de.tudarmstadt.ukp.clarin.webanno.telemetry.ui;

import java.util.List;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.telemetry.TelemetryDetail;

public class TelemetryDetailsPanel
    extends Panel
{
    private static final long serialVersionUID = 5309790721013698159L;

    public TelemetryDetailsPanel(String aId, IModel<List<TelemetryDetail>> aModel)
    {
        super(aId, aModel);

        ListView<TelemetryDetail> details = new ListView<TelemetryDetail>("details")
        {
            private static final long serialVersionUID = 5156853968330655499L;

            @Override
            protected void populateItem(ListItem<TelemetryDetail> aItem)
            {
                aItem.add(new Label("key", aItem.getModelObject().getKey()));
                aItem.add(new Label("value", aItem.getModelObject().getValue()));
                aItem.add(new Label("description", aItem.getModelObject().getDescription()));
            }
        };

        details.setModel(aModel);

        add(details);
    }
}
