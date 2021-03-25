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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar;

import java.util.List;

import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;

public class ActionBar
    extends Panel
{
    private static final long serialVersionUID = -5445521297124750502L;

    private @SpringBean ActionBarExtensionPoint actionBarExtensionPoint;

    public ActionBar(String aId)
    {
        super(aId);

        add(new ListView<ActionBarExtension>("items",
                LoadableDetachableModel.of(this::getExtensions))
        {
            private static final long serialVersionUID = -3124915140030491897L;

            @Override
            protected void populateItem(ListItem<ActionBarExtension> aItem)
            {
                aItem.add(aItem.getModelObject().createActionBarItem("item",
                        (AnnotationPageBase) getPage()));
            }
        });
    }

    @Override
    protected void onInitialize()
    {
        super.onInitialize();

        for (ActionBarExtension ext : getExtensions()) {
            ext.onInitialize((AnnotationPageBase) getPage());
        }
    }

    private List<ActionBarExtension> getExtensions()
    {
        return actionBarExtensionPoint.getExtensions((AnnotationPageBase) getPage());
    }
}
