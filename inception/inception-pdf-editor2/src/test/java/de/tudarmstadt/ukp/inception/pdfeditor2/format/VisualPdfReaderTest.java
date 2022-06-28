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
package de.tudarmstadt.ukp.inception.pdfeditor2.format;

import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CasFactory;
import org.dkpro.core.api.pdf.type.PdfChunk;
import org.dkpro.core.api.pdf.type.PdfPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VisualPdfReaderTest
{
    final String testFilesBase = "src/test/resources/pdfbox-testfiles/";

    CAS cas;

    @BeforeEach
    void setup() throws Exception
    {
        cas = CasFactory.createCas();
    }

    @Test
    void thatCoordinatesAreStoredInCas() throws Exception
    {
        CollectionReader reader = createReader( //
                VisualPdfReader.class, //
                VisualPdfReader.PARAM_SORT_BY_POSITION, true, //
                VisualPdfReader.PARAM_SOURCE_LOCATION, testFilesBase + "eu-001.pdf");
        reader.getNext(cas);
        assertThat(cas.select(PdfChunk.class).asList()).hasSize(163);
        assertThat(cas.select(PdfPage.class).asList()).hasSize(3);

        for (var pdfLine : cas.select(PdfChunk.class)) {
            int totalCharWidth = 0;
            for (int w : pdfLine.getC()._getTheArray()) {
                totalCharWidth += w;
            }
            assertThat(pdfLine.getBegin() + totalCharWidth).isEqualTo(pdfLine.getEnd());
        }
    }
}
