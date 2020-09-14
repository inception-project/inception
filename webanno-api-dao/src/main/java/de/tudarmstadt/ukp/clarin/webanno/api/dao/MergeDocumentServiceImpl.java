/*
 * Copyright 2020
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

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.lang3.Validate;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.MergeDocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public abstract class MergeDocumentServiceImpl
    implements MergeDocumentService
{
    private @Autowired CasStorageService casStorageService;
    private @Autowired AnnotationSchemaService annotationService;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void writeResultCas(CAS aCas, SourceDocument aDocument, boolean aUpdateTimestamp)
        throws IOException
    {
        casStorageService.writeCas(aDocument, aCas, getResultCasUser());
        if (aUpdateTimestamp) {
            aDocument.setTimestamp(new Timestamp(new Date().getTime()));
            entityManager.merge(aDocument);
        }
    }

    @Override
    public CAS readResultCas(SourceDocument aDocument) throws IOException
    {
        return casStorageService.readCas(aDocument, getResultCasUser());
    }

    @Override
    public void upgradeResultCas(CAS aCas, SourceDocument aDocument)
        throws UIMAException, IOException
    {
        annotationService.upgradeCas(aCas, aDocument, getResultCasUser());
    }

    @Override
    public abstract String getResultCasUser();

    @Override
    public boolean existsResultCas(SourceDocument aSourceDocument) throws IOException
    {
        return casStorageService.existsCas(aSourceDocument, getResultCasUser());
    }

    @Override
    public Optional<Long> getResultCasTimestamp(SourceDocument aDocument) throws IOException
    {
        Validate.notNull(aDocument, "Source document must be specified");
        
        return casStorageService.getCasTimestamp(aDocument, getResultCasUser());
    }
}
