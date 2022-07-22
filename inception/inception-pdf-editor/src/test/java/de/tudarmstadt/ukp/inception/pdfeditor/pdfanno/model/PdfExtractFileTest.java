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
package de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model;

import static de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.PdfExtractFile.getSubstitutionTable;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.readAllBytes;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PdfExtractFileTest
{
    private PdfExtractFile pdfExtractFile;

    @BeforeEach
    public void setup() throws Exception
    {
        String pdftxt = new String(readAllBytes(Paths.get("src/test/resources/pdfextract.txt")),
                UTF_8);
        pdfExtractFile = new PdfExtractFile(pdftxt, getSubstitutionTable());
    }

    @Test
    public void testGetSanitizedContent()
    {
        assertThat(pdfExtractFile.getStringContent()).isEqualTo("2abc[4]dﬁeg`A");
        assertThat(pdfExtractFile.getSanitizedContent()).isEqualTo("2abc[4]dfiegÀ");
    }

    @Test
    public void testGetStringPdfExtractLines()
    {
        assertThat(new Offset(4, 4)).isEqualTo(pdfExtractFile.getSanitizedIndex(0));
        assertThat(new Offset(5, 5)).isEqualTo(pdfExtractFile.getSanitizedIndex(1));
        assertThat(new Offset(6, 6)).isEqualTo(pdfExtractFile.getSanitizedIndex(2));
        assertThat(new Offset(7, 7)).isEqualTo(pdfExtractFile.getSanitizedIndex(3));
        assertThat(new Offset(11, 11)).isEqualTo(pdfExtractFile.getSanitizedIndex(4));
        assertThat(new Offset(12, 12)).isEqualTo(pdfExtractFile.getSanitizedIndex(5));
        assertThat(new Offset(13, 13)).isEqualTo(pdfExtractFile.getSanitizedIndex(6));
        assertThat(new Offset(14, 14)).isEqualTo(pdfExtractFile.getSanitizedIndex(7));
        assertThat(new Offset(15, 15)).isEqualTo(pdfExtractFile.getSanitizedIndex(8));
        assertThat(new Offset(15, 15)).isEqualTo(pdfExtractFile.getSanitizedIndex(9));
        assertThat(new Offset(16, 16)).isEqualTo(pdfExtractFile.getSanitizedIndex(10));
        assertThat(new Offset(17, 17)).isEqualTo(pdfExtractFile.getSanitizedIndex(11));
        assertThat(new Offset(18, 19)).isEqualTo(pdfExtractFile.getSanitizedIndex(12));
    }

    @Test
    public void testGetStringIndex()
    {
        assertThat(new Offset(0, 0)).isEqualTo(pdfExtractFile.getStringIndex(1));
        assertThat(new Offset(0, 0)).isEqualTo(pdfExtractFile.getStringIndex(2));
        assertThat(new Offset(0, 0)).isEqualTo(pdfExtractFile.getStringIndex(3));
        assertThat(new Offset(0, 0)).isEqualTo(pdfExtractFile.getStringIndex(4));
        assertThat(new Offset(1, 1)).isEqualTo(pdfExtractFile.getStringIndex(5));
        assertThat(new Offset(2, 2)).isEqualTo(pdfExtractFile.getStringIndex(6));
        assertThat(new Offset(3, 3)).isEqualTo(pdfExtractFile.getStringIndex(7));
        assertThat(new Offset(4, 4)).isEqualTo(pdfExtractFile.getStringIndex(8));
        assertThat(new Offset(4, 4)).isEqualTo(pdfExtractFile.getStringIndex(9));
        assertThat(new Offset(4, 4)).isEqualTo(pdfExtractFile.getStringIndex(10));
        assertThat(new Offset(4, 4)).isEqualTo(pdfExtractFile.getStringIndex(11));
        assertThat(new Offset(5, 5)).isEqualTo(pdfExtractFile.getStringIndex(12));
        assertThat(new Offset(6, 6)).isEqualTo(pdfExtractFile.getStringIndex(13));
        assertThat(new Offset(7, 7)).isEqualTo(pdfExtractFile.getStringIndex(14));
        assertThat(new Offset(8, 9)).isEqualTo(pdfExtractFile.getStringIndex(15));
        assertThat(new Offset(10, 10)).isEqualTo(pdfExtractFile.getStringIndex(16));
        assertThat(new Offset(11, 11)).isEqualTo(pdfExtractFile.getStringIndex(17));
        assertThat(new Offset(12, 12)).isEqualTo(pdfExtractFile.getStringIndex(18));
        assertThat(new Offset(12, 12)).isEqualTo(pdfExtractFile.getStringIndex(19));
    }
}
