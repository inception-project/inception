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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.ws;

import java.io.Serializable;
import java.util.Optional;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

/**
 * Describes which document/data owner/location a document should be opened at.
 *
 * @param document
 *            the document to show (mandatory).
 * @param dataOwner
 *            whose annotations to show (mandatory).
 * @param focus
 *            the unit to move to (optional) - leave empty to not move.
 */
public record EditorRequest(SourceDocument document, AnnotationSet dataOwner,
        Optional<Integer> focus)
    implements Serializable
{
    private static final long serialVersionUID = 1L;

    /**
     * @return whether this request names the same document and data owner as the given one. The
     *         focus is deliberately <b>not</b> part of this: it is what distinguishes "move within
     *         the document" from "load a different document".
     */
    public boolean matches(SourceDocument aDocument, AnnotationSet aDataOwner)
    {
        return document.equals(aDocument) && dataOwner.equals(aDataOwner);
    }
}
