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

import static java.lang.Integer.MAX_VALUE;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.ActionBarExtension;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.curation.page.LegacyCurationPage;
import de.tudarmstadt.ukp.inception.ui.curation.actionbar.opendocument.CurationOpenDocumentDialog;
import de.tudarmstadt.ukp.inception.ui.curation.page.CurationPage;

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
        return MAX_VALUE;
    }

    @Override
    public boolean accepts(AnnotationPageBase aPage)
    {
        return aPage instanceof LegacyCurationPage || aPage instanceof CurationPage;
    }

    @Override
    public Panel createActionBarItem(String aId, AnnotationPageBase aPage)
    {
        return new CurationDocumentNavigator(aId, aPage);
    }

    @Override
    public void onInitialize(AnnotationPageBase aPage)
    {
        // Open the dialog if no document has been selected.
        aPage.add(new CurationAutoOpenDialogBehavior());

        // We put the dialog into the page footer since this is presently the only place where we
        // can dynamically add stuff to the page. We cannot add simply to the action bar (i.e.
        // DocumentNavigator) because the action bar only shows *after* a document has been
        // selected. In order to allow the dialog to be rendered *before* a document has been
        // selected (i.e. when the action bar is still not on screen), we need to attach it to the
        // page. The same for the AutoOpenDialogBehavior we add below.
        aPage.addToFooter(createOpenDocumentsDialog("item", aPage));
    }

    private CurationOpenDocumentDialog createOpenDocumentsDialog(String aId,
            AnnotationPageBase aPage)
    {
        return new CurationOpenDocumentDialog(aId, aPage.getModel(),
                LoadableDetachableModel.of(aPage::getListOfDocs));
    }
}
