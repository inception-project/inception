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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.fit.factory.CasFactory;
import org.apache.uima.jcas.tcas.DocumentAnnotation;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;

public class CasPersistenceUtilsTest
{
    public @Rule TemporaryFolder testFolder = new TemporaryFolder();
    
    {
        System.setProperty(CASImpl.ALWAYS_HOLD_ONTO_FSS, "true");
    }
    
    @Test
    public void thatDocumentAnnotationIsNotDuplicatedDuringLoad() throws Exception
    {
        CAS cas = CasFactory.createCas();
        
        cas.setDocumentLanguage("en");
        
        DocumentMetaData dmd = DocumentMetaData.create(cas);
        dmd.setLanguage("en");
        
        File file = testFolder.newFile();
        
        CasPersistenceUtils.writeSerializedCas(cas, file);
        
        CAS cas2 = CasCreationUtils.createCas((TypeSystemDescription) null, null, null);
        
        CasPersistenceUtils.readSerializedCas(cas2, file);
        
        assertThat(cas2.select(DocumentAnnotation.class).asList())
                .extracting(fs -> fs.getType().getName())
                .containsExactly(DocumentMetaData.class.getName());
    }

//    @Test
//    public void importExportService() throws Exception
//    {
//        TypeSystemDescription tsd = createTypeSystemDescriptionFromPath(
//                "src/test/resources/xmi/test-types.xml");
//        
//        File inFile = new File("src/test/resources/xmi/rdxruC2B.xmi");
//        
//        
//        ImportExportServiceImpl imExSrv = new ImportExportServiceImpl(null,
//                asList(new XmiFormatSupport()), null, null);
//        imExSrv.init();
//        imExSrv.importCasFromFile(inFile, null, XmiFormatSupport.ID, tsd);
//    }
    
}
