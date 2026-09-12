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
package de.tudarmstadt.ukp.clarin.webanno.ui.curation.actionbar;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.ActionBarContext;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.ActionBarExtension;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.page.LegacyCurationPage;
import de.tudarmstadt.ukp.inception.ui.curation.actionbar.opendocument.CurationOpenDocumentDialog;
import de.tudarmstadt.ukp.inception.ui.curation.page.CurationPage;
import de.tudarmstadt.ukp.inception.ui.curation.page.SplitCurationPage;

@Order(ActionBarExtension.ORDER_DOCUMENT_NAVIGATOR)
public class CurationDocumentNavigatorActionBarExtension
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
        return PRIORITY_GLOBAL;
    }

    @Override
    public boolean accepts(ActionBarContext aContext)
    {
        return (aContext.page() instanceof LegacyCurationPage
                || aContext.page() instanceof CurationPage
                || aContext.page() instanceof SplitCurationPage);
    }

    @Override
    public Panel createActionBarItem(String aId, ActionBarContext aContext)
    {
        return new CurationDocumentNavigator(aId, aContext.page());
    }

    @Override
    public void onInitialize(ActionBarContext aContext)
    {
        if (!(aContext.page() instanceof LegacyCurationPage)) {
            return;
        }

        // Open the dialog if no document has been selected.
        aContext.page().add(new CurationAutoOpenDialogBehavior());

        aContext.page().addToFooter(createOpenDocumentsDialog("item", aContext.page()));
    }

    private CurationOpenDocumentDialog createOpenDocumentsDialog(String aId,
            AnnotationPageBase aPage)
    {
        return new CurationOpenDocumentDialog(aId, aPage.getModel(),
                LoadableDetachableModel.of(aPage::getListOfDocs));
    }
}
