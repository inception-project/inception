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
package de.tudarmstadt.ukp.inception.curation.service;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

/**
 * Thrown when a document cannot be opened for curation at all - because it has not reached a
 * curatable state, or because there is neither an existing curation CAS nor any annotation document
 * from which one could be created.
 */
public class DocumentNotCuratableException
    extends CurationNotPossibleException
{
    private static final long serialVersionUID = -8154425387113920943L;

    public DocumentNotCuratableException(SourceDocument aDocument, String aMessage)
    {
        super(aDocument, aMessage);
    }
}
