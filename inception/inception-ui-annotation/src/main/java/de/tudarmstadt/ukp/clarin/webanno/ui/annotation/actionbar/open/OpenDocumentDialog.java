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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.open;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.model.IModel;
import org.danekja.java.util.function.serializable.SerializableBiFunction;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.open.OpenDocumentDialogPanel.DocumentSelectedCallback;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DocumentEditor;
import de.tudarmstadt.ukp.inception.bootstrap.BootstrapModalDialog;

public class OpenDocumentDialog
    extends BootstrapModalDialog
{
    private static final long serialVersionUID = 2767538203924633288L;

    private final SerializableBiFunction<Project, User, List<AnnotationDocument>> docListProvider;
    private final IModel<Project> project;
    private final IModel<AnnotationSet> dataOwner;
    private final DocumentSelectedCallback onDocumentSelected;

    public OpenDocumentDialog(String aId, IModel<Project> aProject,
            IModel<AnnotationSet> aDataOwner,
            SerializableBiFunction<Project, User, List<AnnotationDocument>> aDocListProvider)
    {
        this(aId, aProject, aDataOwner, aDocListProvider, null);
    }

    /**
     * @param aDataOwner
     *            whose annotations are pre-selected when the dialog opens. If {@code null} or
     *            resolving to {@code null} the session owner is used.
     * @param aOnDocumentSelected
     *            callback to be invoked with the chosen document and data owner on selection. If
     *            {@code null}, the document editor manager of the enclosing annotation page is
     *            asked to show it.
     */
    public OpenDocumentDialog(String aId, IModel<Project> aProject,
            IModel<AnnotationSet> aDataOwner,
            SerializableBiFunction<Project, User, List<AnnotationDocument>> aDocListProvider,
            DocumentSelectedCallback aOnDocumentSelected)
    {
        super(aId);
        setOutputMarkupId(true);
        trapFocus();

        docListProvider = aDocListProvider;
        project = aProject;
        dataOwner = aDataOwner;
        onDocumentSelected = aOnDocumentSelected;
    }

    /**
     * Show the dialog letting the workspace determine in which editor it opens.
     *
     * @param aTarget
     *            the AJAX target.
     */
    public void show(AjaxRequestTarget aTarget)
    {
        show(aTarget, null);
    }

    /**
     * Show the dialog on behalf of a specific editor. The document should open in that editor.
     *
     * @param aTarget
     *            the AJAX target.
     * @param aRequestingEditor
     *            the editor that asked, or {@code null} for "no preference".
     */
    public void show(AjaxRequestTarget aTarget, DocumentEditor aRequestingEditor)
    {
        var content = new OpenDocumentDialogPanel(ModalDialog.CONTENT_ID, project, dataOwner,
                docListProvider, onDocumentSelected, aRequestingEditor);
        super.open(content, aTarget);
    }
}
