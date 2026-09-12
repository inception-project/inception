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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.preferences;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.preferences.AnnotationPreferencesDialog;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageBase2;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;

public class PreferencesSidebarFooterItem
    extends Panel
{
    private static final long serialVersionUID = -7010485821196580123L;

    private final AnnotationPreferencesDialog preferencesDialog;
    private final AnnotationPageBase2 page;

    public PreferencesSidebarFooterItem(String aId, AnnotationPageBase2 aPage)
    {
        super(aId);

        page = aPage;

        add(preferencesDialog = new AnnotationPreferencesDialog("preferencesDialog",
                page.getModel()));
        preferencesDialog.setOnChangeAction(this::actionCompletePreferencesChange);

        add(new LambdaAjaxLink("showPreferencesDialog", this::actionShowPreferencesDialog));
    }

    private void actionCompletePreferencesChange(AjaxRequestTarget aTarget)
    {
        page.actionLoadDocument(aTarget);
    }

    private void actionShowPreferencesDialog(AjaxRequestTarget aTarget)
    {
        preferencesDialog.show(aTarget);
    }
}
