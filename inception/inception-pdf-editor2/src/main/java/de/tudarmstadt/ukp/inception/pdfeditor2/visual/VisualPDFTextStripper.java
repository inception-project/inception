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

import static de.tudarmstadt.ukp.inception.pdfeditor2.pdfbox.GlyphPositionUtils.calculateFontBounds;
import static de.tudarmstadt.ukp.inception.pdfeditor2.pdfbox.GlyphPositionUtils.makeFlipAT;
import static de.tudarmstadt.ukp.inception.pdfeditor2.pdfbox.GlyphPositionUtils.makeRotateAT;
import static de.tudarmstadt.ukp.inception.pdfeditor2.pdfbox.GlyphPositionUtils.normalizeWord;
import static java.util.stream.Collectors.joining;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.pdfeditor2.pdfbox.GlyphPositionUtils;
import de.tudarmstadt.ukp.inception.pdfeditor2.visual.model.VChunk;
import de.tudarmstadt.ukp.inception.pdfeditor2.visual.model.VGlyph;
import de.tudarmstadt.ukp.inception.pdfeditor2.visual.model.VModel;
import de.tudarmstadt.ukp.inception.pdfeditor2.visual.model.VPage;

public class VisualPDFTextStripper
    extends PDFTextStripper
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final List<VPage> pages = new ArrayList<>();

    private AffineTransform flipAT;
    private AffineTransform rotateAT;

    private int pageIndex;
    private List<VChunk> chunks;

    private Map<TextPosition, Rectangle2D.Double> fontPositionCache;

    public VisualPDFTextStripper() throws IOException
    {
        setAddMoreFormatting(true);
        setSortByPosition(false);
        setShouldSeparateByBeads(true);
    }

    private CharSequence getBuffer()
    {
        return ((StringWriter) output).getBuffer();
    }

    @Override
    public void processPage(PDPage aPage) throws IOException
    {
        log.debug("Processing page {}", getCurrentPageNo());

        fontPositionCache = new HashMap<>();

        chunks = new ArrayList<>();
        flipAT = makeFlipAT(aPage);
        rotateAT = makeRotateAT(aPage);

        // We must fetch the index here at the start, otherwise the index may be off, e.g. if there
        // are empty pages in the PDF
        pageIndex = document.getPages().indexOf(aPage);

        int pageBeginOffset = getBuffer().length();

        super.processPage(aPage);

        int pageEndOffset = getBuffer().length();

        String pageText = getBuffer().subSequence(pageBeginOffset, pageEndOffset).toString();

        PDRectangle visibleArea = getCurrentPage().getCropBox();

        pages.add(new VPage(pageIndex, visibleArea.getWidth(), visibleArea.getHeight(),
                pageBeginOffset, pageEndOffset, pageText, chunks));
    }

    @Override
    protected void writePage() throws IOException
    {
        calculateCharacterPositions();

        removeCharactersOutsideTheFilterBox();

        super.writePage();
    }

    private void calculateCharacterPositions() throws IOException
    {
        for (List<TextPosition> article : charactersByArticle) {
            for (TextPosition tp : article) {
                Shape fontShape = calculateFontBounds(tp, flipAT, rotateAT);
                Rectangle2D.Double f = (Rectangle2D.Double) fontShape.getBounds2D();
                fontPositionCache.put(tp, f);
            }
        }
    }

    private void removeCharactersOutsideTheFilterBox()
    {
        int removed = 0;
        int total = 0;

        PDRectangle visibleArea = getCurrentPage().getCropBox().createRetranslatedRectangle();

        for (List<TextPosition> article : charactersByArticle) {
            Iterator<TextPosition> i = article.iterator();
            while (i.hasNext()) {
                TextPosition tp = i.next();
                Rectangle2D.Double f = fontPositionCache.get(tp);

                total++;

                if (!visibleArea.contains((float) f.x, (float) f.y)
                        && !visibleArea.contains((float) f.x, (float) (f.y + f.height))
                        && !visibleArea.contains((float) (f.x + f.width), (float) f.y)
                        && !visibleArea.contains((float) (f.x + f.width),
                                (float) (f.y + f.width))) {
                    removed++;
                    i.remove();
                }
            }
        }

        log.debug("Removed {} of {} characters because they are outside the visible area ({})",
                removed, total, visibleArea, visibleArea);
    }

    @Override
    protected void writeString(String aText, List<TextPosition> aTextPositions) throws IOException
    {
        assertAlignedTextPositions(aText, aTextPositions);

        // assert aTextPositions.stream().mapToDouble(TextPosition::getDir).distinct()
        // .count() <= 1 : "All glyphs in the string should have the same direction";

        int unicodeLength;
        assert (unicodeLength = aTextPositions.stream().map(TextPosition::getUnicode)
                .map(GlyphPositionUtils::normalizeWord).mapToInt(String::length).sum()) == aText
                        .length() : "Line length [" + aText.length()
                                + "] should match glyph unicode length [" + unicodeLength + "] - ["
                                + aText + "] <-> ["
                                + aTextPositions.stream().map(TextPosition::getUnicode)
                                        .map(GlyphPositionUtils::normalizeWord).collect(joining())
                                + "]";

        var cs = new ProtoVChunk(getBuffer().length(), aText);

        for (var pos : aTextPositions) {
            // Start a new chunk if the direction changes
            var dir = ((pos.getRotation() - pos.getDir()) + 360) % 360;
            if (dir != cs.dir && cs.cursor > 0) {
                chunks.add(cs.endChunk());
                cs.dir = dir;
            }

            Rectangle2D.Double f = fontPositionCache.get(pos);

            // Account for glyphs that were mapped to more than one character by normalization
            // e.g. expanded ligatures
            String normalizedUnicode = normalizeWord(pos.getUnicode());
            assert aText.startsWith(normalizedUnicode, cs.cursor + cs.offset) : "Line text at "
                    + cs.cursor + cs.offset + " should start with [" + normalizedUnicode
                    + "] but was [" + aText.substring(cs.cursor + cs.offset) + "]";

            var glyph = new VGlyph(cs.cursor + cs.begin, pageIndex, normalizedUnicode, cs.dir, f);
            cs.addGlyph(glyph);
        }

        if (cs.cursor > 0) {
            chunks.add(cs.endChunk());
        }

        super.writeString(aText, aTextPositions);
    }

    private class ProtoVChunk
    {
        final String text;
        final int begin;
        int offset = 0;

        List<VGlyph> glyphs = new ArrayList<>();
        int cursor = 0;
        float dir = 0;
        float x0 = Float.MAX_VALUE;
        float y0 = Float.MAX_VALUE;
        float y1 = Float.MIN_VALUE;
        float x1 = Float.MIN_VALUE;

        public ProtoVChunk(int aBegin, String aText)
        {
            begin = aBegin;
            text = aText;
        }

        void addGlyph(VGlyph glyph)
        {
            cursor += glyph.getUnicode().length();

            x1 = Math.max(x1, glyph.getFontX() + glyph.getFontWidth());
            y1 = Math.max(y1, glyph.getFontY() + glyph.getFontHeight());
            x0 = Math.min(x0, glyph.getFontX());
            y0 = Math.min(y0, glyph.getFontY());

            glyphs.add(glyph);
        }

        VChunk endChunk()
        {
            int end = begin + offset + cursor;
            var w = x1 - x0;
            var h = y1 - y0;

            var chunkText = text.substring(offset, offset + cursor);
            VChunk chunk = new VChunk(begin, end, chunkText, dir, x0, y0, w, h, glyphs);

            offset += cursor;
            glyphs = new ArrayList<>();
            cursor = 0;
            dir = 0;
            x0 = Float.MAX_VALUE;
            y0 = Float.MAX_VALUE;
            y1 = Float.MIN_VALUE;
            x1 = Float.MIN_VALUE;

            return chunk;
        }
    }

    private void assertAlignedTextPositions(String aText, List<TextPosition> aTextPositions)
    {
        int cumulativePositionLength = aTextPositions.stream()
                .mapToInt(t -> normalizeWord(t.getUnicode()).length()) //
                .sum();

        if (aText.length() != cumulativePositionLength) {
            System.out.printf("%d (%d) vs %d (%d)%n", aText.length(), aText.codePoints().count(),
                    cumulativePositionLength, aTextPositions.size());
            System.out.println(" Text [" + aText + "]");
            StringBuilder sb = new StringBuilder();
            for (TextPosition p : aTextPositions) {
                sb.append(p.getUnicode());
            }
            String posText = sb.toString();
            System.out.println(" Pos [" + posText + "]");
            System.out.println(" Diff [" + StringUtils.difference(posText, aText) + "]");

            throw new IllegalStateException(
                    "Text positions account for a string length of [" + cumulativePositionLength
                            + "] but string has length [" + aText.length() + "]");
        }
    }

    public VModel getVisualModel()
    {
        return new VModel(pages);
    }
}
