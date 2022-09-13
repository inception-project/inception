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
package de.tudarmstadt.ukp.inception.ui.curation.actionbar.opendocument;

import static wicket.contrib.input.events.EventType.click;

import java.util.List;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.wicketstuff.event.annotation.OnEvent;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.input.InputBehavior;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import wicket.contrib.input.events.key.KeyType;

/**
 * A panel used as Open dialog. It Lists all projects a user is member of for annotation/curation
 * and associated documents
 */
public class CurationOpenDocumentDialogPanel
    extends Panel
{
    private static final long serialVersionUID = 1299869948010875439L;

    private static final String CID_TABLE = "table";
    private static final String CID_CLOSE_DIALOG = "closeDialog";

    private @SpringBean ProjectService projectService;
    private @SpringBean DocumentService documentService;
    private @SpringBean UserDao userRepository;

    private final CurationDocumentTable table;

    private final IModel<AnnotatorState> state;
    private final IModel<List<SourceDocument>> documentList;

    public CurationOpenDocumentDialogPanel(String aId, IModel<AnnotatorState> aState,
            IModel<List<SourceDocument>> aDocumentList)
    {
        super(aId);

        state = aState;
        documentList = aDocumentList;

        queue(new LambdaAjaxLink(CID_CLOSE_DIALOG, this::actionCancel)
                .add(new InputBehavior(new KeyType[] { KeyType.Escape }, click)));

        table = new CurationDocumentTable(CID_TABLE, documentList);
        queue(table);
    }

    @OnEvent
    public void onCurationDocumentOpenDocumentEvent(CurationDocumentOpenDocumentEvent aEvent)
    {
        state.getObject().setDocument(aEvent.getSourceDocument(), documentList.getObject());

        ((AnnotationPageBase) getPage()).actionLoadDocument(aEvent.getTarget());

        findParent(ModalDialog.class).close(aEvent.getTarget());
    }

    private void actionCancel(AjaxRequestTarget aTarget)
    {
        // If the dialog is aborted without choosing a document, return to a sensible location.
        if (state.getObject().getProject() == null || state.getObject().getDocument() == null) {
            try {
                ProjectPageBase ppb = findParent(ProjectPageBase.class);
                if (ppb != null) {
                    ((ProjectPageBase) ppb).backToProjectPage();
                }
            }
            catch (RestartResponseException e) {
                throw e;
            }
            catch (Exception e) {
                setResponsePage(getApplication().getHomePage());
            }
        }

        findParent(ModalDialog.class).close(aTarget);
    }
}
