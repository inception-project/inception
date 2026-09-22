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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar;

import java.io.Serializable;
import java.util.Optional;

import org.apache.commons.lang3.Validate;

import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPageBase2;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DocumentEditor;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DocumentEditorManager;

public record SidebarContext(AnnotationPageBase2 page, DocumentEditorManager manager)
    implements Serializable
{
    private static final long serialVersionUID = 4471253809995359914L;

    public SidebarContext
    {
        Validate.notNull(page, "Page must be specified");
        Validate.notNull(manager, "Editor manager must be specified");
    }

    /**
     * @return the active editor, if any. Empty before the first document has been opened.
     */
    public Optional<DocumentEditor> editorContext()
    {
        return manager.getActiveEditor();
    }

    /**
     * @return whether any editor currently holds a document.
     */
    public boolean hasDocument()
    {
        return manager.hasOpenDocument();
    }

    /**
     * @return the data owner to act on: the active editor's, or - before any document has been
     *         opened - the page's default. Never the session owner on a page that pins its data
     *         owner.
     */
    public AnnotationSet getDataOwner()
    {
        return editorContext() //
                .map(editor -> editor.getAnnotatorState().getDataOwner()) //
                .orElseGet(page::getDefaultDataOwner);
    }

    /**
     * @return the project being worked on. Never {@code null}.
     */
    public Project getProject()
    {
        return editorContext() //
                .map(DocumentEditor::getProject) //
                .orElseGet(page::getProject);
    }
}
