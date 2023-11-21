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
package de.tudarmstadt.ukp.clarin.webanno.ui.project.documents;

import java.io.Serializable;
import java.util.Objects;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public class SourceDocumentTableRow
    implements Serializable
{
    private static final long serialVersionUID = 7351346533262118753L;

    private final SourceDocument document;

    private boolean selected;

    public SourceDocumentTableRow(SourceDocument aSourceDocument)
    {
        document = aSourceDocument;
    }

    public SourceDocument getDocument()
    {
        return document;
    }

    public void setSelected(boolean aSelected)
    {
        selected = aSelected;
    }

    public boolean isSelected()
    {
        return selected;
    }

    @Override
    public boolean equals(final Object other)
    {
        if (!(other instanceof SourceDocumentTableRow)) {
            return false;
        }
        SourceDocumentTableRow castOther = (SourceDocumentTableRow) other;
        return Objects.equals(document, castOther.document);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(document);
    }
}
