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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.linesOf;

import java.io.File;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PdfAnnoModelTest
{

    private PdfAnnoModel pdfAnnoModel;

    @BeforeEach
    public void setup()
    {
        pdfAnnoModel = new PdfAnnoModel("0.5.0", "0.3.2");
        pdfAnnoModel.addSpan(new Span("1", 1, "somelabel", "#FF00FF", "sometext", 0, 7));
        pdfAnnoModel.addSpan(new Span("2", 1, "someotherlabel", "#00AA00", "atest", 8, 12));
        pdfAnnoModel.addRelation(new Relation("3", "1", "2", "label", "#CCCCCC"));
    }

    @Test
    public void testgetAnnoFileContent() throws Exception
    {
        String annoFileString = pdfAnnoModel.getAnnoFileContent();
        assertThat(linesOf(new File("src/test/resources/annoFile.anno"), "UTF-8"))
                .isEqualTo(Arrays.asList(annoFileString.split("\n")));
    }
}
