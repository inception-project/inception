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
package de.tudarmstadt.ukp.inception.ui.curation.editor;

import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorBase;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorFactoryImplBase;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DocumentEditorManager;

/**
 * Produces the split-pane curation view. Only the split-curation page uses it - it pins this
 * factory explicitly rather than resolving one from the project preferences.
 */
public class SplitCurationEditorFactory
    extends AnnotationEditorFactoryImplBase
{
    public static final String ID = "splitCurationEditor";

    @Override
    public String getDisplayName()
    {
        return "Split-pane curation";
    }

    @Override
    public int accepts(Project aProject, String aFormat)
    {
        return NOT_SUITABLE;
    }

    @Override
    public boolean isUserSelectable()
    {
        return false;
    }

    @Override
    public AnnotationEditorBase create(String aId, IModel<AnnotatorState> aModel,
            DocumentEditorManager aManager, AnnotationActionHandler aActionHandler,
            CasProvider aCasProvider)
    {
        return new SplitCurationEditor(aId, aModel, aManager, aActionHandler, aCasProvider);
    }

    @Override
    public void initState(AnnotatorState aState)
    {
        // Do nothing: the paging strategy belongs to the inner text editor, which has
        // already established it by the time this runs.
    }
}
