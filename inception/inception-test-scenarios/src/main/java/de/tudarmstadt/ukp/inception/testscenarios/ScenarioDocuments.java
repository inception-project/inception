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
package de.tudarmstadt.ukp.inception.testscenarios;

import java.io.IOException;

import org.apache.uima.UIMAException;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.text.TextFormatSupport;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;

/**
 * Imports the sample documents that ship next to a scenario.
 */
public class ScenarioDocuments
{
    private ScenarioDocuments()
    {
        // No instances
    }

    /**
     * Imports a plain-text sample document that lives in the same package as {@code aOwner}.
     * <p>
     * The document is looked up relative to {@code aOwner} rather than relative to the calling
     * instance, so a scenario that inherits its import logic from a base class still picks up the
     * data file sitting next to the class that declares it.
     *
     * @param aDocumentService
     *            used to store the document.
     * @param aProject
     *            the project to import into.
     * @param aOwner
     *            the class whose package holds the document.
     * @param aDocumentName
     *            the file name of the document, e.g. {@code curation-sample.txt}.
     * @return the imported document.
     * @throws IOException
     *             if the document cannot be found or read.
     */
    public static SourceDocument importDocument(DocumentService aDocumentService, Project aProject,
            Class<?> aOwner, String aDocumentName)
        throws IOException
    {
        var doc = SourceDocument.builder() //
                .withProject(aProject) //
                .withName(aDocumentName) //
                .withFormat(TextFormatSupport.ID) //
                .build();

        try (var is = aOwner.getResourceAsStream(aDocumentName)) {
            if (is == null) {
                throw new IOException("Scenario document [" + aDocumentName
                        + "] not found next to [" + aOwner.getName() + "]");
            }

            aDocumentService.uploadSourceDocument(is, doc);
        }
        catch (UIMAException e) {
            throw new IOException(e);
        }

        return doc;
    }
}
