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
package de.tudarmstadt.ukp.inception.workload.matrix.management.support;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.NEW;

import java.io.Serializable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;

public abstract class DocumentMatrixSortKey
    implements Serializable
{
    private static final long serialVersionUID = -2104352856931165344L;

    public static final DocumentMatrixSortKey DOCUMENT_NAME = new FixedDocumentMatrixSortKey(
            "documentName")
    {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare(DocumentMatrixRow aRow1, DocumentMatrixRow aRow2)
        {
            return aRow1.getSourceDocument().getName()
                    .compareTo(aRow2.getSourceDocument().getName());
        }
    };

    public static final DocumentMatrixSortKey DOCUMENT_STATE = new FixedDocumentMatrixSortKey(
            "documentState")
    {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare(DocumentMatrixRow aRow1, DocumentMatrixRow aRow2)
        {
            return aRow1.getState().getName().compareTo(aRow2.getState().getName());
        }
    };

    public static final DocumentMatrixSortKey CURATION_STATE = new FixedDocumentMatrixSortKey(
            "curationState")
    {
        private static final long serialVersionUID = 1L;

        @Override
        public int compare(DocumentMatrixRow aRow1, DocumentMatrixRow aRow2)
        {
            return aRow1.getCurationState().getName().compareTo(aRow2.getCurationState().getName());
        }
    };

    public abstract int compare(DocumentMatrixRow aRow1, DocumentMatrixRow aRow2);

    public static AnnotatorDocumentMatrixSortKey annotatorSortKey(String aUsername)
    {
        return new AnnotatorDocumentMatrixSortKey(aUsername);
    }

    static public abstract class FixedDocumentMatrixSortKey
        extends DocumentMatrixSortKey
    {
        private static final long serialVersionUID = 3514426420365814343L;
        private final String name;

        public FixedDocumentMatrixSortKey(String aName)
        {
            name = aName;
        }

        @Override
        public boolean equals(final Object other)
        {
            if (!(other instanceof FixedDocumentMatrixSortKey)) {
                return false;
            }
            FixedDocumentMatrixSortKey castOther = (FixedDocumentMatrixSortKey) other;
            return new EqualsBuilder().append(name, castOther.name).isEquals();
        }

        @Override
        public int hashCode()
        {
            return new HashCodeBuilder().append(name).toHashCode();
        }
    }

    static public class AnnotatorDocumentMatrixSortKey
        extends DocumentMatrixSortKey
    {
        private static final long serialVersionUID = -8232983429696733363L;

        private final String name;

        public AnnotatorDocumentMatrixSortKey(String aName)
        {
            name = aName;
        }

        @Override
        public int compare(DocumentMatrixRow aRow1, DocumentMatrixRow aRow2)
        {
            AnnotationDocument annDoc1 = aRow1.getAnnotationDocument(name);
            AnnotationDocument annDoc2 = aRow2.getAnnotationDocument(name);

            AnnotationDocumentState state1 = annDoc1 != null ? annDoc1.getState() : NEW;
            AnnotationDocumentState state2 = annDoc2 != null ? annDoc2.getState() : NEW;

            return state1.getName().compareTo(state2.getName());
        }

        @Override
        public boolean equals(final Object other)
        {
            if (!(other instanceof AnnotatorDocumentMatrixSortKey)) {
                return false;
            }
            AnnotatorDocumentMatrixSortKey castOther = (AnnotatorDocumentMatrixSortKey) other;
            return new EqualsBuilder().append(name, castOther.name).isEquals();
        }

        @Override
        public int hashCode()
        {
            return new HashCodeBuilder().append(name).toHashCode();
        }
    }
}
