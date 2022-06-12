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
package de.tudarmstadt.ukp.inception.pdfeditor2.visual;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FilenameFilter;
import java.io.StringWriter;
import java.util.Collection;

import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import de.tudarmstadt.ukp.inception.pdfeditor2.visual.model.VGlyph;
import de.tudarmstadt.ukp.inception.pdfeditor2.visual.model.VLine;
import de.tudarmstadt.ukp.inception.pdfeditor2.visual.model.VModel;
import de.tudarmstadt.ukp.inception.pdfeditor2.visual.model.VPage;

class VisualPDFTextStripperTest
{
    public static Iterable<File> pdfFiles()
    {
        return asList(new File("src/test/resources/pdfbox-testfiles/")
                .listFiles((FilenameFilter) new SuffixFileFilter(asList(".pdf"))));
    }

    @ParameterizedTest(name = "{index}: reading PDF file {0}")
    @MethodSource("pdfFiles")
    void thatPdfCanBeParsed(File aFile) throws Exception
    {
        VModel vModel;
        var target = new StringWriter();
        try (PDDocument doc = PDDocument.load(aFile)) {
            var extractor = new VisualPDFTextStripper();
            extractor.writeText(doc, target);
            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                var page = doc.getPage(i);
                System.out.printf("Processing page %d (%d) of %d %n", i + 1,
                        doc.getPages().indexOf(page) + 1, doc.getNumberOfPages());
                extractor.processPage(page);
            }
            vModel = extractor.getVisualModel();
        }

        // System.out.println(target.toString().length());
        // System.out.println(target.toString());

        assertValidGlyphCoordindates(vModel);
        assertValidGlyphOffsets(vModel.getPages());
    }

    private void assertValidGlyphCoordindates(VModel vModel)
    {
        for (VPage vPage : vModel.getPages()) {
            for (VLine vLine : vPage.getLines()) {
                for (VGlyph vGlyph : vLine.getGlyphs()) {
                    float d = vLine.getDir();
                    float x = (d == 0 || d == 180) ? vGlyph.getBase() : vLine.getBase();
                    float y = (d == 0 || d == 180) ? vLine.getBase() : vGlyph.getBase();
                    float w = (d == 0 || d == 180) ? vGlyph.getExtent() : vLine.getExtent();
                    float h = (d == 0 || d == 180) ? vLine.getExtent() : vGlyph.getExtent();

                    // Font (screen) coordinates should be within the line boundaries
                    assertThat(vGlyph.getFontX()).isBetween(x, x + w);
                    assertThat(vGlyph.getFontWidth()).isLessThanOrEqualTo(w);
                    assertThat(vGlyph.getFontY()).isBetween(y, y + h);
                    assertThat(vGlyph.getFontHeight()).isLessThanOrEqualTo(h);

                    System.out.printf("%.0f %f %f -- (%f %f %f %f) -- %s%n", vLine.getDir(),
                            vLine.getBase(), vLine.getExtent(), x, y, w, h, vGlyph);
                }
            }
        }
    }

    private void assertValidGlyphOffsets(Collection<VPage> aPages)
    {
        for (VPage page : aPages) {
            for (VLine line : page.getLines()) {
                assertValidGlyphOffsets(line);
            }
        }
    }

    private void assertValidGlyphOffsets(VLine aLine)
    {
        int cumulativePositionLength = aLine.getGlyphs().stream()
                .mapToInt(t -> t.getUnicode().length()) //
                .sum();

        int textLength = aLine.getText().length();
        if (textLength != cumulativePositionLength) {
            System.out.printf("%d (%d) vs %d (%d)%n", textLength,
                    aLine.getText().codePoints().count(), cumulativePositionLength,
                    aLine.getGlyphs().size());
            System.out.println(" Text [" + aLine.getText() + "]");
            StringBuilder sb = new StringBuilder();
            for (VGlyph g : aLine.getGlyphs()) {
                sb.append(g.getUnicode());
            }
            String posText = sb.toString();
            System.out.println(" Pos [" + posText + "]");
            System.out.println(" Diff [" + StringUtils.difference(posText, aLine.getText()) + "]");

            assertThat(textLength) //
                    .as("Text positions account for a string length of [" + cumulativePositionLength
                            + "] but string has length [" + textLength + "]") //
                    .isEqualTo(cumulativePositionLength);
        }
    }
}
