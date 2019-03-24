/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukpinception.pdfeditor.pdfanno.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Scanner;

import org.junit.Before;
import org.junit.Test;

import de.tudarmstadt.ukp.inception.pdfeditor.PdfAnnotationEditor;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.Offset;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.PdfExtractFile;

public class PdfExtractFileTest
{
    private PdfExtractFile pdfExtractFile;

    @Before
    public void setup() throws Exception
    {
        String pdftxt = new Scanner(new File("src/test/resources/pdfextract.txt"))
            .useDelimiter("\\Z").next();
        pdfExtractFile = new PdfExtractFile(pdftxt, PdfAnnotationEditor.getSubstitutionTable());
    }

    @Test
    public void testGetStringContent()
    {
        assertThat("2abc[4]dﬁeg`A").isEqualTo(pdfExtractFile.getStringContent());
    }

    @Test
    public void testGetSanitizedContent()
    {
        assertThat("2abc[4]dfiegÀ").isEqualTo(pdfExtractFile.getSanitizedContent());
    }

    @Test
    public void testGetStringPdfExtractLines()
    {
        assertThat(new Offset(4, 4)).isEqualTo(pdfExtractFile.getExtractIndex(0));
        assertThat(new Offset(5, 5)).isEqualTo(pdfExtractFile.getExtractIndex(1));
        assertThat(new Offset(6, 6)).isEqualTo(pdfExtractFile.getExtractIndex(2));
        assertThat(new Offset(7, 7)).isEqualTo(pdfExtractFile.getExtractIndex(3));
        assertThat(new Offset(11, 11)).isEqualTo(pdfExtractFile.getExtractIndex(4));
        assertThat(new Offset(12, 12)).isEqualTo(pdfExtractFile.getExtractIndex(5));
        assertThat(new Offset(13, 13)).isEqualTo(pdfExtractFile.getExtractIndex(6));
        assertThat(new Offset(14, 14)).isEqualTo(pdfExtractFile.getExtractIndex(7));
        assertThat(new Offset(15, 15)).isEqualTo(pdfExtractFile.getExtractIndex(8));
        assertThat(new Offset(15, 15)).isEqualTo(pdfExtractFile.getExtractIndex(9));
        assertThat(new Offset(16, 16)).isEqualTo(pdfExtractFile.getExtractIndex(10));
        assertThat(new Offset(17, 17)).isEqualTo(pdfExtractFile.getExtractIndex(11));
        assertThat(new Offset(18, 19)).isEqualTo(pdfExtractFile.getExtractIndex(12));
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
