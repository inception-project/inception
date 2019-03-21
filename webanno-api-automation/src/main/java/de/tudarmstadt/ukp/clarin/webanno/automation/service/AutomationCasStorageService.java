/*
 * Copyright 2012
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
package de.tudarmstadt.ukp.clarin.webanno.automation.service;

import java.io.File;
import java.io.IOException;

import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.clarin.webanno.model.TrainingDocument;

public interface AutomationCasStorageService
{
    String SERVICE_NAME = "automationCasStorageService";
    
    
    /**
     * Creates an annotation document for the {@link TrainingDocument}
     *
     * @param aDocument
     *            the {@link TrainingDocument}
     * @param aJcas
     *            The annotated CAS object
     */
    void writeCas(TrainingDocument aDocument, CAS aJcas)
            throws IOException;
    
    /**
     * For a given {@link TrainingDocument}, return the annotated CAS object
     *
     * @param aDocument
     *            the {@link TrainingDocument}
     */
    CAS readCas(TrainingDocument aDocument)
            throws IOException;
        
    File getAutomationFolder(TrainingDocument aDocument)
        throws IOException;

    void analyzeAndRepair(TrainingDocument aDocument, CAS aCas);
}
