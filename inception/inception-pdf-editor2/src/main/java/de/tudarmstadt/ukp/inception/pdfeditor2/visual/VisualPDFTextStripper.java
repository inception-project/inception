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

import de.tudarmstadt.ukp.inception.pdfeditor2.visual.model.VGlyph;
import de.tudarmstadt.ukp.inception.pdfeditor2.visual.model.VLine;
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
    private List<VLine> lines;

    private Map<TextPosition, Rectangle2D.Double> fontPositionCache;

    public VisualPDFTextStripper() throws IOException
    {
        setAddMoreFormatting(true);
        setSortByPosition(true);
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

        lines = new ArrayList<>();
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
                pageBeginOffset, pageEndOffset, pageText, lines));
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

        List<VGlyph> glyphs = new ArrayList<>();

        float lineBase = Float.MAX_VALUE;
        float lineHeight = Float.MIN_VALUE;
        int begin = getBuffer().length();
        int cursor = begin;

        assert aTextPositions.stream().mapToDouble(TextPosition::getDir).distinct()
                .count() <= 1 : "All glyphs in the string should have the same direction";

        float dir = 0;
        for (var pos : aTextPositions) {
            Rectangle2D.Double f = fontPositionCache.get(pos);

            // Account for glyphs that were mapped to more than one character by normalization
            // e.g. expanded ligatures
            String normalizedUnicode = normalizeWord(pos.getUnicode());
            VGlyph glyph = new VGlyph(cursor, pageIndex, normalizedUnicode, pos.getDir(), f);
            glyphs.add(glyph);
            cursor += normalizedUnicode.length();

            dir = pos.getDir();
            if (dir == 90 || dir == 270) {
                lineBase = Math.min(lineBase, glyph.getFontX());
                lineHeight = Math.max(lineHeight, glyph.getFontWidth());
            }
            else {
                lineBase = Math.min(lineBase, glyph.getFontY());
                lineHeight = Math.max(lineHeight, glyph.getFontHeight());
            }
        }

        super.writeString(aText, aTextPositions);

        int end = getBuffer().length();

        lines.add(new VLine(begin, end, aText, dir, lineBase, lineHeight, glyphs));
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
