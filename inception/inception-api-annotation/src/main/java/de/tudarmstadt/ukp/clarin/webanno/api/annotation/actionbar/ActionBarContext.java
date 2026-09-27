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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.actionbar;

import org.apache.commons.lang3.Validate;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.page.AnnotationPageBase;
import de.tudarmstadt.ukp.inception.rendering.editorstate.DocumentEditor;

public record ActionBarContext(AnnotationPageBase page, DocumentEditor editor) {
    public ActionBarContext
    {
        Validate.notNull(editor, "Editor context must be specified");
    }

    /**
     * @return whether there is an editor showing a document to act on.
     */
    public boolean hasDocument()
    {
        var state = editor.getAnnotatorState();

        return state != null && state.getDocument() != null;
    }
}
