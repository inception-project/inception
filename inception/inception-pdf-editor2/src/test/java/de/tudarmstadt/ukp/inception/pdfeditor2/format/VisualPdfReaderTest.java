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

import static org.apache.commons.lang3.ArrayUtils.toPrimitive;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.io.File;
import java.io.StringWriter;

import org.apache.pdfbox.Loader;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.CasFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.dkpro.core.api.pdf.type.PdfChunk;
import org.dkpro.core.api.pdf.type.PdfPage;
import org.dkpro.core.api.xml.type.XmlDocument;
import org.dkpro.core.api.xml.type.XmlElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.OS;

import de.tudarmstadt.ukp.inception.pdfeditor2.visual.VisualPDFTextStripper;
import de.tudarmstadt.ukp.inception.pdfeditor2.visual.model.VChunk;
import de.tudarmstadt.ukp.inception.pdfeditor2.visual.model.VGlyph;
import de.tudarmstadt.ukp.inception.pdfeditor2.visual.model.VModel;

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
        var reader = createReader( //
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

    @Test
    void thatHtmlStructureIsGenerated() throws Exception
    {
        var reader = createReader( //
                VisualPdfReader.class, //
                VisualPdfReader.PARAM_SORT_BY_POSITION, true, //
                VisualPdfReader.PARAM_GENERATE_HTML_STRUCTURE, true, //
                VisualPdfReader.PARAM_SOURCE_LOCATION, testFilesBase + "eu-001.pdf");
        reader.getNext(cas);

        assertThat(cas.select(XmlDocument.class).asList()).hasSize(1);
        assertThat(cas.select(XmlElement.class).asList()).hasSize(44);
    }

    @Test
    void thatRtlCoordinatesMakeSenseSorting1() throws Exception
    {
        VModel expected;
        var textBuffer = new StringWriter();
        try (var doc = Loader.loadPDF(new File(testFilesBase + "hello3.pdf"))) {
            var extractor = new VisualPDFTextStripper();
            extractor.setSortByPosition(true);
            extractor.writeText(doc, textBuffer);
            expected = extractor.getVisualModel();
        }

        var jCas = JCasFactory.createJCas();
        jCas.setDocumentText(textBuffer.toString());
        VisualPdfReader.visualModelToCas(expected, jCas);

        // Extracted text on Windows seems to differ, maybe due to installed system fonts
        var offset = OS.WINDOWS.isCurrentOs() ? 2 : 0;

        assertThat(jCas.select(PdfChunk.class).asList()) //
                .extracting(PdfChunk::getBegin, PdfChunk::getEnd, PdfChunk::getCoveredText)
                .containsExactly( //
                        tuple(2 + offset, 8 + offset, "Hello "), //
                        tuple(8 + offset, 12 + offset, "محمد"), //
                        tuple(12 + offset, 20 + offset, " World. "));

        assertThat(jCas.select(PdfChunk.class).asList()) //
                .extracting(PdfChunk::getX, c -> c.getG()._getTheArray()) //
                .containsExactly( //
                        tuple(90.0f, new float[] { //
                                90.0f, 104.439545f, 113.32465f, 118.89308f, 124.46151f,
                                134.4655f }),
                        tuple(139.4505f, new float[] { //
                                164.62729f, 154.04388f, 146.17776f, 139.4505f }),
                        tuple(172.5174f, new float[] { //
                                172.5174f, 177.49641f, 196.35753f, 206.42746f, 213.0808f,
                                218.63524f, 228.62524f, 233.62024f }));

        var actual = VisualPdfReader.visualModelFromCas(jCas.getCas(),
                jCas.select(PdfPage.class).asList());

        assertThat(actual.getPages().get(0).getChunks()) //
                .extracting(VChunk::getBegin, VChunk::getText) //
                .containsExactly( //
                        tuple(2 + offset, "Hello "), //
                        tuple(8 + offset, "محمد"), //
                        tuple(12 + offset, " World. "));

        assertThat(actual.getPages().get(0).getChunks()) //
                .extracting( //
                        VChunk::getX, //
                        c -> c.getGlyphs().stream().map(VGlyph::getUnicode).toArray(String[]::new),
                        c -> toPrimitive(
                                c.getGlyphs().stream().map(VGlyph::getFontX).toArray(Float[]::new)),
                        c -> toPrimitive(c.getGlyphs().stream().map(VGlyph::getExtent)
                                .toArray(Float[]::new))) //
                .containsExactly( //
                        tuple( //
                                90.0f, //
                                new String[] { "H", "e", "l", "l", "o", " " }, //
                                new float[] { 90.0f, 104.439545f, 113.32465f, 118.89308f,
                                        124.46151f, 134.4655f },
                                new float[] { 14.439545f, 8.885109f, 5.568428f, 5.568428f,
                                        10.00399f, 4.994995f }),
                        tuple( //
                                139.4505f, //
                                new String[] { "م", "ح", "م", "د" }, //
                                new float[] { 164.62729f, 154.04388f, 146.17776f, 139.4505f },
                                new float[] { 7.872116f, 10.583405f, 7.8661194f, 6.7272644f }),
                        tuple( //
                                172.5174f, //
                                new String[] { " ", "W", "o", "r", "l", "d", ".", " " }, //
                                new float[] { 172.5174f, 177.49641f, 196.35753f, 206.42746f,
                                        213.0808f, 218.63524f, 228.62524f, 233.62024f },
                                new float[] { 4.979019f, 18.861115f, 10.069931f, 6.6533356f,
                                        5.5544434f, 9.9900055f, 4.994995f, 4.994995f }));
    }

    @Test
    void thatRtlCoordinatesMakeSenseNoSorting1() throws Exception
    {
        VModel expected;
        var textBuffer = new StringWriter();
        try (var doc = Loader.loadPDF(new File(testFilesBase + "hello3.pdf"))) {
            var extractor = new VisualPDFTextStripper();
            extractor.setSortByPosition(false);
            extractor.writeText(doc, textBuffer);
            expected = extractor.getVisualModel();
        }

        assertThat(expected.getPages().get(0).getChunks()) //
                .extracting( //
                        VChunk::getX, //
                        c -> c.getGlyphs().stream().map(VGlyph::getUnicode).toArray(String[]::new),
                        c -> toPrimitive(
                                c.getGlyphs().stream().map(VGlyph::getFontX).toArray(Float[]::new)),
                        c -> toPrimitive(c.getGlyphs().stream().map(VGlyph::getExtent)
                                .toArray(Float[]::new))) //
                .containsExactly( //
                        tuple( //
                                90.0f, //
                                new String[] { "H", "e", "l", "l", "o", " " }, //
                                new float[] { 90.0f, 104.439545f, 113.32465f, 118.89308f,
                                        124.46151f, 134.4655f }, //
                                new float[] {
                                        14.42556f, 8.87112f, 5.55444f, 5.55444f, 9.99f, 4.995f }),
                        tuple( //
                                139.4505f, //
                                new String[] { "م", "ح", "م", "د" }, //
                                new float[] { 164.62729f, 154.04388f, 146.17776f, 139.4505f },
                                new float[] { 7.8721204f, 10.5894f, 7.8721204f, 6.73326f }),
                        tuple( //
                                172.5174f, //
                                new String[] { " ", "W", "o", "r", "l", "d", ".", " " }, //
                                new float[] { 172.5174f, 177.49641f, 196.35753f, 206.42746f,
                                        213.0808f, 218.63524f, 228.62524f, 233.62024f },
                                new float[] { 4.995f, 18.86112f, 9.99f, 6.6533403f, 5.55444f, 9.99f,
                                        4.995f, 4.995f }));
        var jCas = JCasFactory.createJCas();
        jCas.setDocumentText(textBuffer.toString());
        VisualPdfReader.visualModelToCas(expected, jCas);

        // Extracted text on Windows seems to differ, maybe due to installed system fonts
        var offset = OS.WINDOWS.isCurrentOs() ? 2 : 0;

        assertThat(jCas.select(PdfChunk.class).asList()) //
                .extracting(PdfChunk::getBegin, PdfChunk::getEnd, PdfChunk::getCoveredText)
                .containsExactly( //
                        tuple(2 + offset, 8 + offset, "Hello "), //
                        tuple(8 + offset, 12 + offset, "محمد"), //
                        tuple(12 + offset, 20 + offset, " World. "));

        assertThat(jCas.select(PdfChunk.class).asList()) //
                .extracting(PdfChunk::getX, c -> c.getG()._getTheArray()) //
                .containsExactly( //
                        tuple(90.0f, new float[] { //
                                90.0f, 104.439545f, 113.32465f, 118.89308f, 124.46151f,
                                134.4655f }),
                        tuple(139.4505f, new float[] { //
                                164.62729f, 154.04388f, 146.17776f, 139.4505f }),
                        tuple(172.5174f, new float[] { //
                                172.5174f, 177.49641f, 196.35753f, 206.42746f, 213.0808f,
                                218.63524f, 228.62524f, 233.62024f }));

        VModel actual = VisualPdfReader.visualModelFromCas(jCas.getCas(),
                jCas.select(PdfPage.class).asList());

        assertThat(actual.getPages().get(0).getChunks()) //
                .extracting(VChunk::getBegin, VChunk::getText) //
                .containsExactly( //
                        tuple(2 + offset, "Hello "), //
                        tuple(8 + offset, "محمد"), //
                        tuple(12 + offset, " World. "));

        assertThat(actual.getPages().get(0).getChunks()) //
                .extracting( //
                        VChunk::getX, //
                        c -> c.getGlyphs().stream().map(VGlyph::getUnicode).toArray(String[]::new),
                        c -> toPrimitive(
                                c.getGlyphs().stream().map(VGlyph::getFontX).toArray(Float[]::new)),
                        c -> toPrimitive(c.getGlyphs().stream().map(VGlyph::getExtent)
                                .toArray(Float[]::new))) //
                .containsExactly( //
                        tuple( //
                                90.0f, //
                                new String[] { "H", "e", "l", "l", "o", " " }, //
                                new float[] { 90.0f, 104.439545f, 113.32465f, 118.89308f,
                                        124.46151f, 134.4655f }, //
                                new float[] { 14.439545f, 8.885109f, 5.568428f, 5.568428f,
                                        10.00399f, 4.994995f }),
                        tuple( //
                                139.4505f, //
                                new String[] { "م", "ح", "م", "د" }, //
                                new float[] { 164.62729f, 154.04388f, 146.17776f, 139.4505f },
                                new float[] { 7.872116f, 10.583405f, 7.8661194f, 6.7272644f }),
                        tuple( //
                                172.5174f, //
                                new String[] { " ", "W", "o", "r", "l", "d", ".", " " }, //
                                new float[] { 172.5174f, 177.49641f, 196.35753f, 206.42746f,
                                        213.0808f, 218.63524f, 228.62524f, 233.62024f },
                                new float[] { 4.979019f, 18.861115f, 10.069931f, 6.6533356f,
                                        5.5544434f, 9.9900055f, 4.994995f, 4.994995f }));
    }

    @Test
    void thatRtlCoordinatesMakeSenseSorting2() throws Exception
    {
        VModel expected;
        var textBuffer = new StringWriter();
        try (var doc = Loader.loadPDF(new File(testFilesBase + "FC60_Times.pdf"))) {
            var extractor = new VisualPDFTextStripper();
            extractor.setSortByPosition(true);
            extractor.writeText(doc, textBuffer);
            expected = extractor.getVisualModel();
        }

        // Extracted text on Windows seems to differ, maybe due to installed system fonts
        var offset = OS.WINDOWS.isCurrentOs() ? new int[] { 2, 2, 3 } : new int[] { 0, 0, 0 };

        // var expectedText = "\n" //
        // + "\n" //
        // + " آَُّتاب\n" //
        // + " \n" //
        // + "\n" //
        // + "\n" //
        // + "";
        // assertThat(textBuffer.toString()) //
        // .isEqualTo(expectedText);

        var jCas = JCasFactory.createJCas();
        jCas.setDocumentText(textBuffer.toString());
        VisualPdfReader.visualModelToCas(expected, jCas);

        assertThat(jCas.select(PdfChunk.class).asList()) //
                .extracting(PdfChunk::getBegin, PdfChunk::getEnd, PdfChunk::getCoveredText)
                .containsExactly( //
                        tuple(2 + offset[0], 7 + offset[0], " آَُّ"), //
                        tuple(7 + offset[1], 10 + offset[1], "تاب"), //
                        tuple(11 + offset[2], 12 + offset[2], " "));

        assertThat(jCas.select(PdfChunk.class).asList()) //
                .extracting(PdfChunk::getX, c -> c.getG()._getTheArray()) //
                .containsExactly( //
                        tuple(//
                                114.486824f, //
                                new float[] { 131.10103f, 120.79997f, 114.486824f }),
                        tuple(//
                                90.00067f, //
                                new float[] { 114.47643f, 108.54f, 90.00067f }),
                        tuple( //
                                90.0f, //
                                new float[] { 90.0f }));

        VModel actual = VisualPdfReader.visualModelFromCas(jCas.getCas(),
                jCas.select(PdfPage.class).asList());

        assertThat(actual.getPages().get(0).getChunks()) //
                .extracting(VChunk::getBegin, VChunk::getText) //
                .containsExactly( //
                        tuple(2 + offset[0], " آَُّ"), //
                        tuple(7 + offset[1], "تاب"), //
                        tuple(11 + offset[2], " "));

        assertThat(actual.getPages().get(0).getChunks()) //
                .extracting( //
                        VChunk::getX, //
                        c -> c.getGlyphs().stream().map(VGlyph::getUnicode).toArray(String[]::new),
                        c -> toPrimitive(
                                c.getGlyphs().stream().map(VGlyph::getFontX).toArray(Float[]::new)),
                        c -> toPrimitive(c.getGlyphs().stream().map(VGlyph::getExtent)
                                .toArray(Float[]::new))) //
                .containsExactly( //
                        tuple(//
                                114.486824f, //
                                new String[] { " ", "آُ", "َّ" }, //
                                new float[] { 131.10103f, 120.79997f, 114.486824f }, //
                                new float[] { 6.494995f, 10.301056f, 6.3131485f }),
                        tuple(//
                                90.00067f, //
                                new String[] { "ت", "ا", "ب" }, //
                                new float[] { 114.47643f, 108.54f, 90.00067f }, //
                                new float[] { 6.339119f, 5.936432f, 18.53933f }),
                        tuple(//
                                90.0f, //
                                new String[] { " " }, //
                                new float[] { 90.0f }, //
                                new float[] { 7.199997f }));
    }

    @Test
    void thatRtlCoordinatesMakeSenseNoSorting2() throws Exception
    {
        VModel expected;
        var textBuffer = new StringWriter();
        try (var doc = Loader.loadPDF(new File(testFilesBase + "FC60_Times.pdf"))) {
            var extractor = new VisualPDFTextStripper();
            extractor.setSortByPosition(false);
            extractor.writeText(doc, textBuffer);
            expected = extractor.getVisualModel();
        }

        // var expectedText = "\n" //
        // + "\n" //
        // + "بآُتَّا \n" //
        // + " \n" //
        // + "\n" //
        // + "\n" //
        // + "";
        // assertThat(textBuffer.toString()) //
        // .isEqualTo(expectedText);

        var jCas = JCasFactory.createJCas();
        jCas.setDocumentText(textBuffer.toString());
        VisualPdfReader.visualModelToCas(expected, jCas);

        // Extracted text on Windows seems to differ, maybe due to installed system fonts
        var offset = OS.WINDOWS.isCurrentOs() ? new int[] { 2, 2, 2, 2, 3 }
                : new int[] { 0, 0, 0, 0, 0 };

        assertThat(jCas.select(PdfChunk.class).asList()) //
                .extracting(PdfChunk::getBegin, PdfChunk::getEnd, PdfChunk::getCoveredText)
                .containsExactly( //
                        tuple(2 + offset[0], 3 + offset[0], "ب"), //
                        tuple(3 + offset[1], 6 + offset[1], "آُت"), //
                        tuple(6 + offset[2], 9 + offset[2], "َّا"), //
                        tuple(10 + offset[3], 11 + offset[3], " "), //
                        tuple(12 + offset[4], 13 + offset[4], " "));

        assertThat(jCas.select(PdfChunk.class).asList()) //
                .extracting(PdfChunk::getX, c -> c.getG()._getTheArray()) //
                .containsExactly( //
                        tuple( //
                                90.00067f, //
                                new float[] { 90.00067f }),
                        tuple(//
                                114.47643f, //
                                new float[] { 120.79997f, 114.47643f }),
                        tuple(//
                                108.54f, //
                                new float[] { 114.486824f, 108.54f }),
                        tuple(//
                                131.10103f, //
                                new float[] { 131.10103f }),
                        tuple(//
                                90.0f, //
                                new float[] { 90.0f }));

        var actual = VisualPdfReader.visualModelFromCas(jCas.getCas(),
                jCas.select(PdfPage.class).asList());

        assertThat(actual.getPages().get(0).getChunks()) //
                .extracting(VChunk::getBegin, VChunk::getText) //
                .containsExactly( //
                        tuple(2 + offset[0], "ب"), //
                        tuple(3 + offset[1], "آُت"), //
                        tuple(6 + offset[2], "َّا"), //
                        tuple(10 + offset[3], " "), //
                        tuple(12 + offset[4], " "));

        assertThat(actual.getPages().get(0).getChunks()) //
                .extracting( //
                        VChunk::getX, //
                        c -> c.getGlyphs().stream().map(VGlyph::getUnicode).toArray(String[]::new),
                        c -> toPrimitive(
                                c.getGlyphs().stream().map(VGlyph::getFontX).toArray(Float[]::new)),
                        c -> toPrimitive(c.getGlyphs().stream().map(VGlyph::getExtent)
                                .toArray(Float[]::new))) //
                .containsExactly( //
                        tuple(//
                                90.00067f, //
                                new String[] { "ب" }, new float[] { 90.00067f },
                                new float[] { 18.523743f }),
                        tuple( //
                                114.47643f, //
                                new String[] { "آُ", "ت" }, //
                                new float[] { 120.79997f, 114.47643f }, //
                                new float[] { 10.2361145f, 6.3235397f }),
                        tuple( //
                                108.54f, //
                                new String[] { "َّ", "ا" }, //
                                new float[] { 114.486824f, 108.54f }, //
                                new float[] { 5.481781f, 5.946823f }),
                        tuple( //
                                131.10103f, //
                                new String[] { " " }, //
                                new float[] { 131.10103f }, //
                                new float[] { 6.494995f }),
                        tuple(//
                                90.0f, //
                                new String[] { " " }, //
                                new float[] { 90.0f }, //
                                new float[] { 7.199997f }));
    }
}
