/*
# * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.api.dao;

import static de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst.CORRECTION_USER;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.lang3.Validate;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.jcas.JCas;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.CorrectionDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

@Component(CorrectionDocumentService.SERVICE_NAME)
public class CorrectionDocumentServiceImpl
    implements CorrectionDocumentService
{
    private @Autowired CasStorageService casStorageService;
    private @Autowired AnnotationSchemaService annotationService;

    @PersistenceContext
    private EntityManager entityManager;

    public CorrectionDocumentServiceImpl()
    {
        // Nothing to do
    }

    @Override
    public boolean existsCorrectionCas(SourceDocument aSourceDocument)
        throws IOException
    {
        try {
            readCorrectionCas(aSourceDocument);
            return true;
        }
        catch (FileNotFoundException e) {
            return false;
        }
    }

    @Override
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_USER')")
    public void writeCorrectionCas(JCas aJcas, SourceDocument aDocument)
        throws IOException
    {
        casStorageService.writeCas(aDocument, aJcas, CORRECTION_USER);
    }

    @Override
    public JCas readCorrectionCas(SourceDocument aDocument)
        throws IOException
    {
        return casStorageService.readCas(aDocument, CORRECTION_USER);
    }

    @Override
    public void upgradeCorrectionCas(CAS aCas, SourceDocument aDocument)
        throws UIMAException, IOException
    {
        annotationService.upgradeCas(aCas, aDocument, CORRECTION_USER);
    }
    
    @Override
    public Optional<Long> getCorrectionCasTimestamp(SourceDocument aDocument) throws IOException
    {
        Validate.notNull(aDocument, "Source document must be specified");
        
        return casStorageService.getCasTimestamp(aDocument, CORRECTION_USER);
    }
}
