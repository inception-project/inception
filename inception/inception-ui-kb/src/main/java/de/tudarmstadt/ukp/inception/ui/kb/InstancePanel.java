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
package de.tudarmstadt.ukp.inception.ui.kb;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import de.tudarmstadt.ukp.inception.bootstrap.BootstrapAjaxTabbedPanel;
import de.tudarmstadt.ukp.inception.kb.graph.KBInstance;
import de.tudarmstadt.ukp.inception.kb.graph.KBObject;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

public class InstancePanel
    extends Panel
{
    private static final long serialVersionUID = -1413622323011843523L;

    private IModel<KnowledgeBase> kbModel;
    private IModel<KBObject> selectedInstanceHandle;
    private IModel<KBObject> selectedConceptHandle;

    public InstancePanel(String aId, IModel<KnowledgeBase> aKbModel,
            IModel<KBObject> aSelectedConceptHandle, IModel<KBObject> aSelectedInstanceHandle,
            IModel<KBInstance> aSelectedInstanceModel)
    {
        super(aId, aSelectedInstanceModel);
        setOutputMarkupId(true);
        kbModel = aKbModel;
        selectedInstanceHandle = aSelectedInstanceHandle;
        selectedConceptHandle = aSelectedConceptHandle;

        addOrReplace(new InstanceInfoPanel("instanceinfo", aKbModel, selectedInstanceHandle,
                aSelectedInstanceModel));

        add(new BootstrapAjaxTabbedPanel<ITab>("tabPanel", makeTabs()));
    }

    private List<ITab> makeTabs()
    {
        List<ITab> tabs = new ArrayList<>();

        tabs.add(new AbstractTab(Model.of("Mentions"))
        {
            private static final long serialVersionUID = 6703144434578403272L;

            @Override
            public Panel getPanel(String panelId)
            {
                if (selectedInstanceHandle.getObject() != null) {
                    return new AnnotatedListIdentifiers(panelId, kbModel, selectedConceptHandle,
                            selectedInstanceHandle, true);
                }
                else {
                    return new EmptyPanel(panelId);
                }
            }
        });
        return tabs;
    }
}
