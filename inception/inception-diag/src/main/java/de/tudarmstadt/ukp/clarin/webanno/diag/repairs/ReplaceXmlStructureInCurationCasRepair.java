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
package de.tudarmstadt.ukp.clarin.webanno.diag.repairs;

import static de.tudarmstadt.ukp.inception.io.xml.dkprocore.XmlNodeUtils.removeXmlDocumentStructure;
import static de.tudarmstadt.ukp.inception.io.xml.dkprocore.XmlNodeUtils.transferXmlDocumentStructure;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CURATION_USER;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

import java.io.IOException;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.dkpro.core.api.xml.type.XmlDocument;

import de.tudarmstadt.ukp.clarin.webanno.diag.repairs.Repair.Safe;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

/**
 * @deprecated We no longer store the document structure in the annotator/curator CASes, only in the
 *             INITIAL_CAS.
 */
@Deprecated
@Safe(false)
public class ReplaceXmlStructureInCurationCasRepair
    implements Repair
{
    private final DocumentService documentService;

    public ReplaceXmlStructureInCurationCasRepair(DocumentService aDocumentService)
    {
        documentService = aDocumentService;
    }

    @Override
    public void repair(SourceDocument aDocument, String aDataOwner, CAS aCas,
            List<LogMessage> aMessages)
    {
        if (!CURATION_USER.equals(aDataOwner)) {
            // We want to repair only the curation CAS
            return;
        }

        try {
            var initialCas = documentService.createOrReadInitialCas(aDocument);

            if (initialCas.select(XmlDocument.class).isEmpty()) {
                // Initial CAS also does not contain a document structure, so we are good
                return;
            }

            var deleted = removeXmlDocumentStructure(aCas);
            var added = transferXmlDocumentStructure(aCas, initialCas);

            var operation = "replaced";
            if (deleted == 0) {
                operation = "added";
            }

            aMessages.add(LogMessage.info(this,
                    "XML document structure has been %s using the structure from the initial CAS (nodes: %d removed, %d added)",
                    operation, deleted, added));
        }
        catch (IOException e) {
            aMessages.add(LogMessage.error(this, "Unable to obtain initial CAS: %s",
                    getRootCauseMessage(e)));
        }
    }
}
