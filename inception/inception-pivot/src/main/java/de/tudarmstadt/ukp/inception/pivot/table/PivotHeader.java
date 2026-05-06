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
package de.tudarmstadt.ukp.inception.pivot.table;

import java.io.Serializable;
import java.util.Map.Entry;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;

public class PivotHeader
    extends GenericPanel<CompoundKey>
{
    private static final long serialVersionUID = -2870242952648859004L;

    public PivotHeader(String aId, IModel<CompoundKey> aModel)
    {
        super(aId, aModel);

        // var toggle = new LambdaAjaxLink("toggle", t -> {
        // System.out.println("Toggle multi/single view");
        // })
        // {
        // private static final long serialVersionUID = 1L;
        //
        // @Override
        // protected void updateAjaxAttributes(AjaxRequestAttributes attributes)
        // {
        // super.updateAjaxAttributes(attributes);
        //
        // attributes.getAjaxCallListeners().add(new AjaxCallListener()
        // {
        // private static final long serialVersionUID = 1L;
        //
        // @Override
        // public CharSequence getPrecondition(Component component)
        // {
        // // Prevent event bubbling
        // return "Wicket.Event.stop(event); return true;";
        // }
        // });
        // }
        // };
        // toggle.setVisible(false);
        // add(toggle);

        add(new ListView<Entry<String, Serializable>>("key", aModel.map(CompoundKey::entries))
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<Entry<String, Serializable>> aItem)
            {
                var name = new Label("name", aItem.getModel().map(Entry::getKey));
                // name.setVisible(aModel.map(CompoundKey::isMultiValue).getObject());
                aItem.add(name);

                aItem.add(
                        new Label("value", aItem.getModel().map(Entry::getValue).orElse("<none>")));
            }
        });
    }
}
