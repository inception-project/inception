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
package de.tudarmstadt.ukp.inception.io.jsoncas;

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.SHARED_READ_ONLY_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.AUTO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.inception.io.pdf.visual.PdfVModelUtils.containsPdfDocumentStructure;
import static de.tudarmstadt.ukp.inception.io.pdf.visual.PdfVModelUtils.transferPdfDocumentStructure;
import static de.tudarmstadt.ukp.inception.io.xml.dkprocore.XmlNodeUtils.containsXmlDocumentStructure;
import static de.tudarmstadt.ukp.inception.io.xml.dkprocore.XmlNodeUtils.transferXmlDocumentStructure;
import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.createCasCopy;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;

import java.io.File;
import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;

import de.tudarmstadt.ukp.clarin.webanno.api.format.FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;

public class UimaJsonCasStructureFormatSupport
    implements FormatSupport
{
    public static final String ID = "jsoncas-struct";
    public static final String NAME = "UIMA CAS JSON 0.4.0 (XML/PDF structure)";

    private final DocumentService documentService;

    public UimaJsonCasStructureFormatSupport(DocumentService aDocumentService)
    {
        documentService = aDocumentService;
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public boolean isWritable()
    {
        return true;
    }

    @Override
    public boolean isProneToInconsistencies()
    {
        return true;
    }

    @Override
    public File write(SourceDocument aDocument, CAS aCas, File aTargetFolder,
            boolean aStripExtension)
        throws ResourceInitializationException, AnalysisEngineProcessException, IOException
    {
        var cas = aCas;

        if (!containsXmlDocumentStructure(aCas) || !containsPdfDocumentStructure(aCas)) {
            try {
                cas = createCasCopy(aCas);
                var initialCas = documentService.createOrReadInitialCas(aDocument, AUTO_CAS_UPGRADE,
                        SHARED_READ_ONLY_ACCESS);
                transferPdfDocumentStructure(cas, initialCas);
                transferXmlDocumentStructure(cas, initialCas);
            }
            catch (ResourceInitializationException | AnalysisEngineProcessException e) {
                throw e;
            }
            catch (UIMAException e) {
                throw new ResourceInitializationException(e);
            }
        }

        return FormatSupport.super.write(aDocument, cas, aTargetFolder, aStripExtension);
    }

    @Override
    public AnalysisEngineDescription getWriterDescription(Project aProject,
            TypeSystemDescription aTSD, CAS aCAS)
        throws ResourceInitializationException
    {
        return createEngineDescription(UimaJsonCasWriter.class, aTSD);
    }
}
