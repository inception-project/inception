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
package de.tudarmstadt.ukp.inception.ui.refdoc;

import org.apache.wicket.Component;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.ActionBarContext;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.ActionBarExtension;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.multiedit.DocumentEditorPanel;

/**
 * Previous/next document buttons for the reference document sidebar.
 */
@Order(ActionBarExtension.ORDER_DOCUMENT_NAVIGATOR)
public class ReferenceDocumentNavigatorActionBarExtension
    implements ActionBarExtension
{

    @Override
    public String getRole()
    {
        return ROLE_NAVIGATOR;
    }

    @Override
    public int getPriority()
    {
        return PRIORITY_LOCAL;
    }

    @Override
    public boolean accepts(ActionBarContext aContext)
    {
        return aContext.editorContext() instanceof ReferenceDocumentEditor;
    }

    @Override
    public Component createActionBarItem(String aId, ActionBarContext aContext)
    {
        return new ReferenceDocumentNavigator(aId, (DocumentEditorPanel) aContext.editorContext());
    }
}
