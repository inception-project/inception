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
package de.tudarmstadt.ukp.inception.ui.curation.sidebar;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.ui.curation.actionbar.opendocument.CurationOpenDocumentDialog;

/**
 * Opens the "Open document" dialog if the page is loaded and no document has been selected yet.
 */
public class CurationSidebarAutoOpenDialogBehavior
    extends AbstractDefaultAjaxBehavior
{
    private static final long serialVersionUID = 5700114110001447912L;

    private @SpringBean CurationSidebarService curationSidebarService;
    private @SpringBean UserDao userRepository;

    /**
     * for the first time, open the <b>open document dialog</b>
     */
    @Override
    public void renderHead(Component aComponent, IHeaderResponse aResponse)
    {
        super.renderHead(aComponent, aResponse);

        aResponse.render(OnLoadHeaderItem.forScript(getCallbackScript()));
    }

    @Override
    protected void respond(AjaxRequestTarget aTarget)
    {
        var page = (AnnotationPageBase) getComponent().getPage();

        // If the page has loaded and there is no document open yet, show the open-document
        // dialog. Also check that the dialog is actually on the page (in the footer) before
        // trying to open it.
        if (page.getModelObject().getDocument() != null) {
            return;
        }

        var project = page.getProject();
        var sessionOwner = userRepository.getCurrentUsername();

        if (curationSidebarService.existsSession(sessionOwner, project.getId())) {
            page.getFooterItems().getObject().stream()
                    .filter(component -> component instanceof CurationOpenDocumentDialog)
                    .map(component -> (CurationOpenDocumentDialog) component).findFirst()
                    .ifPresent(dialog -> dialog.show(aTarget));
        }
    }
}
