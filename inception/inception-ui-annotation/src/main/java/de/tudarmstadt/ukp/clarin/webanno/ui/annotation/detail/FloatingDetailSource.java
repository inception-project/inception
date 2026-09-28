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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail;

import java.io.Serializable;
import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;

/**
 * An annotation that the {@link AnnotationDetailEditorPanel} shows read-only while it is not
 * selected in any editor, e.g. an annotation from another user's CAS. The panel shows it without
 * changing the active editor and without touching the active editor's selection.
 * <p>
 * The source has a state of its own - holding the project, the document, the data owner and the
 * selection - and reads the CAS on demand. It does not hold on to the CAS.
 */
class FloatingDetailSource
    implements Serializable
{
    private static final long serialVersionUID = 4474416036370734108L;

    private final AnnotatorState state;
    private final ReadOnlyAnnotationActionHandler actionHandler;
    private final List<String> ownerNames;

    /**
     * @param aState
     *            the state holding the project, the document, the data owner and the selection
     * @param aCasProvider
     *            provides the data owner's CAS
     * @param aOwnerNames
     *            the UI names of the users the annotation is shown as belonging to - the data owner
     *            first
     */
    FloatingDetailSource(AnnotatorState aState, CasProvider aCasProvider, List<String> aOwnerNames)
    {
        state = aState;
        actionHandler = new ReadOnlyAnnotationActionHandler(aCasProvider);
        ownerNames = aOwnerNames;
    }

    AnnotatorState getState()
    {
        return state;
    }

    AnnotationActionHandler getActionHandler()
    {
        return actionHandler;
    }

    List<String> getOwnerNames()
    {
        return ownerNames;
    }
}
