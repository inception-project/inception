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
import static org.assertj.core.api.Assertions.linesOf;

import java.io.File;
import java.util.Arrays;
import java.util.Scanner;

import org.junit.Before;
import org.junit.Test;

import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.AnnoFile;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.Relation;
import de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model.Span;

public class AnnoFileTest
{

    private AnnoFile annoFile;

    @Before
    public void setup()
    {
        annoFile = new AnnoFile("0.5.0", "0.3.2");
        annoFile.addSpan(new Span(1, 1, "#FF00FF", "sometext", 0, 7));
        annoFile.addSpan(new Span(2, 1, "#00AA00", "atest", 8, 12));
        annoFile.addRelation(new Relation(1, 2, "#CCCCCC"));
    }

    @Test
    public void testColorMap() throws Exception
    {
        String colorMapString = annoFile.getColorMap().toString();
        assertThat(new Scanner(new File("src/test/resources/colormap.json"))
            .useDelimiter("\\Z").next()).isEqualTo(colorMapString);
    }

    @Test
    public void testToString() throws Exception
    {
        String annoFileString = annoFile.toString();
        assertThat(linesOf(new File("src/test/resources/annoFile.anno"),
            "UTF-8")).isEqualTo(Arrays.asList(annoFileString.split("\n")));
    }
}
