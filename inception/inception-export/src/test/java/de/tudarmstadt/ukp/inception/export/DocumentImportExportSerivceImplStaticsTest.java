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
package de.tudarmstadt.ukp.inception.export;

import static de.tudarmstadt.ukp.inception.export.DocumentImportExportServiceImpl.addOrUpdateDocumentMetadata;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.uima.fit.factory.JCasFactory;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;

public class DocumentImportExportSerivceImplStaticsTest
{
    @Test
    void thatDocumentMetadataIsSetCorrectly() throws Exception
    {
        var project = Project.builder() //
                .withSlug("test-project") //
                .build();
        var sourceDocument = SourceDocument.builder() //
                .withName("Test Document.txt") //
                .withProject(project) //
                .build();

        var slug = sourceDocument.getProject().getSlug();
        var username = "username";

        var jcas = JCasFactory.createJCas();
        DocumentMetaData.create(jcas);

        addOrUpdateDocumentMetadata(jcas.getCas(), sourceDocument, username);

        var dmd = DocumentMetaData.get(jcas.getCas());
        assertThat(dmd.getDocumentTitle()).as("documentTitle").isEqualTo(sourceDocument.getName());
        assertThat(dmd.getDocumentUri()).as("documentUri")
                .isEqualTo(slug + "/" + sourceDocument.getName());
        assertThat(dmd.getDocumentBaseUri()).as("documentBaseUri").isEqualTo(slug);
        assertThat(dmd.getCollectionId()).as("collectionId").isEqualTo(slug);
        assertThat(dmd.getDocumentId()).as("documentId").isEqualTo(username);

    }

}
