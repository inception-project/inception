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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar;

import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.ActionBarExtension;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.docnav.DocumentNavigator;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.open.OpenDocumentDialog;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ApplicationPageBase;

@Order(ActionBarExtension.ORDER_DOCUMENT_NAVIGATOR)
@Component
public class AnnotationDocumentNavigatorActionBarExtension
    implements ActionBarExtension
{
    @Override
    public String getRole()
    {
        return ROLE_NAVIGATOR;
    }

    @Override
    public boolean accepts(AnnotationPageBase aPage)
    {
        return aPage instanceof AnnotationPage;
    }

    @Override
    public Panel createActionBarItem(String aId, AnnotationPageBase aPage)
    {
        return new DocumentNavigator(aId, aPage);
    }

    @Override
    public void onInitialize(AnnotationPageBase aPage)
    {
        // Open the dialog if no document has been selected.
        aPage.add(new AutoOpenDialogBehavior());

        // We put the dialog into the page footer since this is presently the only place where we
        // can dynamically add stuff to the page. We cannot add simply to the action bar (i.e.
        // DocumentNavigator) because the action bar only shows *after* a document has been
        // selected. In order to allow the dialog to be rendered *before* a document has been
        // selected (i.e. when the action bar is still not on screen), we need to attach it to the
        // page. The same for the AutoOpenDialogBehavior we add below.
        aPage.addToFooter(createOpenDocumentsDialog(ApplicationPageBase.CID_FOOTER_ITEM, aPage));
    }

    @Override
    public void onRemove(AnnotationPageBase aPage)
    {
        aPage.getBehaviors(AutoOpenDialogBehavior.class).forEach(aPage::remove);
        aPage.getFooterItems().getObject().stream() //
                .filter(OpenDocumentDialog.class::isInstance) //
                .toList() // avoid concurrent modification problems
                .forEach(aPage::removeFromFooter);
    }

    private OpenDocumentDialog createOpenDocumentsDialog(String aId, AnnotationPageBase aPage)
    {
        var page = (AnnotationPage) aPage;
        return new OpenDocumentDialog(aId, aPage.getModel(), page::listAccessibleDocuments);
    }
}
