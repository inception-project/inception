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
package de.tudarmstadt.ukp.clarin.webanno.diag.checks;

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.SHARED_READ_ONLY_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.AUTO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CURATION_USER;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

import java.io.IOException;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.dkpro.core.api.xml.type.XmlDocument;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

/**
 * @deprecated We no longer store the document structure in the annotator/curator CASes, only in the
 *             INITIAL_CAS.
 */
@Deprecated
public class XmlStructurePresentInCurationCasCheck
    implements Check
{
    private final DocumentService documentService;

    public XmlStructurePresentInCurationCasCheck(DocumentService aDocumentService)
    {
        documentService = aDocumentService;
    }

    @Override
    public boolean check(SourceDocument aDocument, String aDataOwner, CAS aCas,
            List<LogMessage> aMessages)
    {
        if (!CURATION_USER.equals(aDataOwner)) {
            // We want to check only the curation CAS
            return true;
        }

        if (!aCas.select(XmlDocument.class).isEmpty()) {
            // Document structure already exists
            return true;
        }

        try (var session = CasStorageSession.openNested()) {
            var initialCas = documentService.createOrReadInitialCas(aDocument, AUTO_CAS_UPGRADE,
                    SHARED_READ_ONLY_ACCESS);

            if (initialCas.select(XmlDocument.class).isEmpty()) {
                // Initial CAS also does not contain a document structure, so we are good
                return true;
            }

            // If we get here, the curation CAS does not contain a document structure
            // but the initial CAS did, so the structure is missing from the curation CAS.
            aMessages.add(LogMessage.error(this,
                    "XML document structure that is present in the initial CAS has not been "
                            + "copied over to the curation CAS"));
            return false;
        }
        catch (IOException e) {
            aMessages.add(LogMessage.error(this, "Unable to obtain initial CAS: %s",
                    getRootCauseMessage(e)));
            return false;
        }
    }
}
