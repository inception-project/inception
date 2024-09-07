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

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static wicket.contrib.input.events.EventType.click;
import static wicket.contrib.input.events.key.KeyType.Page_down;
import static wicket.contrib.input.events.key.KeyType.Page_up;
import static wicket.contrib.input.events.key.KeyType.Shift;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar.export.ExportDocumentDialog;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.documents.api.DocumentAccess;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.wicket.input.InputBehavior;
import de.tudarmstadt.ukp.inception.ui.curation.actionbar.opendocument.CurationOpenDocumentDialog;
import de.tudarmstadt.ukp.inception.ui.curation.page.CurationPage;
import wicket.contrib.input.events.key.KeyType;

/**
 * @deprecated Can be removed when the sidebar curation mode on the annotation page goes away. On
 *             the new {@link CurationPage}, this is not required anymore because the {link
 *             CurationDocumentNavigatorActionBarExtension} is used.
 * @forRemoval 35.0
 */
@Deprecated(forRemoval = true)
public class CurationSidebarDocumentNavigator
    extends Panel
{
    private static final long serialVersionUID = 7061696472939390003L;

    private @SpringBean ProjectService projectService;
    private @SpringBean DocumentAccess documentAccess;
    private @SpringBean UserDao userService;

    private AnnotationPageBase page;

    private final ExportDocumentDialog exportDialog;

    public CurationSidebarDocumentNavigator(String aId, AnnotationPageBase aPage)
    {
        super(aId);

        page = aPage;

        queue(new LambdaAjaxLink("showPreviousDocument", t -> actionShowPreviousDocument(t))
                .add(new InputBehavior(new KeyType[] { Shift, Page_up }, click)));

        queue(new LambdaAjaxLink("showNextDocument", t -> actionShowNextDocument(t))
                .add(new InputBehavior(new KeyType[] { Shift, Page_down }, click)));

        queue(new LambdaAjaxLink("showOpenDocumentDialog", this::actionShowOpenDocumentDialog));

        queue(exportDialog = new ExportDocumentDialog("exportDialog", page.getModel()));
        queue(new LambdaAjaxLink("showExportDialog", exportDialog::show) //
                .add(visibleWhen(this::isExportable)));
    }

    private boolean isExportable()
    {
        return documentAccess.canExportAnnotationDocument(userService.getCurrentUser(),
                page.getModelObject().getProject());
    }

    /**
     * Show the previous document, if exist
     * 
     * @param aTarget
     *            the AJAX request target
     */
    public void actionShowPreviousDocument(AjaxRequestTarget aTarget)
    {
        var documentChanged = page.getModelObject().moveToPreviousDocument(page.getListOfDocs());
        if (!documentChanged) {
            info("There is no previous document");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }
        page.actionLoadDocument(aTarget);
    }

    /**
     * Show the next document if exist
     * 
     * @param aTarget
     *            the AJAX request target
     */
    public void actionShowNextDocument(AjaxRequestTarget aTarget)
    {
        var documentChanged = page.getModelObject().moveToNextDocument(page.getListOfDocs());
        if (!documentChanged) {
            info("There is no next document");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }
        page.actionLoadDocument(aTarget);
    }

    public void actionShowOpenDocumentDialog(AjaxRequestTarget aTarget)
    {
        page.getModelObject().getSelection().clear();
        page.getFooterItems().getObject().stream()
                .filter(component -> component instanceof CurationOpenDocumentDialog)
                .map(component -> (CurationOpenDocumentDialog) component) //
                .findFirst() //
                .ifPresent(dialog -> dialog.show(aTarget));
    }
}
