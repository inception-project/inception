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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import org.junit.Before;
import org.junit.Test;

import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.PdfExtractFile;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.PdfExtractLine;

public class PdfExtractFileTest
{
    private PdfExtractFile pdfExtractFile;

    @Before
    public void setup() throws Exception
    {
        String pdftxt = new Scanner(new File("src/test/resources/pdfextract.txt"))
            .useDelimiter("\\Z").next();
        pdfExtractFile = new PdfExtractFile(pdftxt, new HashMap<>());
    }

    @Test
    public void testGetStringContent()
    {
        assertThat("2abc[4]d").isEqualTo(pdfExtractFile.getStringContent());
    }

    @Test
    public void testGetStringPdfExtractLines()
    {
        List<PdfExtractLine> pdfExtractLines = new ArrayList<>();
        pdfExtractLines.add(
            new PdfExtractLine(1, 7, "c", "550.406 789.56177 8.3499165 16.080894"));
        pdfExtractLines.add(
            new PdfExtractLine(1, 11, "[", "551.406 790.56177 9.3499165 17.080894"));
        pdfExtractLines.add(
            new PdfExtractLine(1, 12, "4", "552.406 791.56177 10.3499165 18.080894"));
        assertThat(pdfExtractLines).isEqualTo(pdfExtractFile.getStringPdfExtractLines(3, 5));
    }
}
