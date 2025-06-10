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
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.translate.UnicodeEscaper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.pdfeditor2.visual.model.VChunk;
import de.tudarmstadt.ukp.inception.pdfeditor2.visual.model.VGlyph;
import de.tudarmstadt.ukp.inception.pdfeditor2.visual.model.VModel;
import de.tudarmstadt.ukp.inception.pdfeditor2.visual.model.VPage;

public class VisualPDFTextStripper
    extends PDFTextStripper
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final List<VPage> pages = new ArrayList<>();

    private AffineTransform flipAT;
    private AffineTransform rotateAT;

    private int pageIndex;
    private List<VChunk> chunks;

    private Map<TextPosition, Rectangle2D.Double> fontPositionCache;

    private PdfEventHandler eventHandler;

    public VisualPDFTextStripper() throws IOException
    {
        setAddMoreFormatting(true);
        setSortByPosition(false);
        setShouldSeparateByBeads(true);
    }

    public void setEventHandler(PdfEventHandler aEventHandler)
    {
        eventHandler = aEventHandler;
    }

    private CharSequence getBuffer()
    {
        return ((StringWriter) output).getBuffer();
    }

    @Override
    public void processPage(PDPage aPage) throws IOException
    {
        LOG.trace("Processing page {}", getCurrentPageNo());

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

        var pageText = getBuffer().subSequence(pageBeginOffset, pageEndOffset).toString();

        var visibleArea = getCurrentPage().getCropBox();

        // When we recover chunks from the CAS, they are sorted by the usual annotation order.
        // We sort here already to have the same order as when we recover from the CAS.
        chunks.sort(comparing(VChunk::getBegin));

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
            for (var tp : article) {
                var fontShape = calculateFontBounds(tp, flipAT, rotateAT);
                Rectangle2D.Double f = (Rectangle2D.Double) fontShape.getBounds2D();
                fontPositionCache.put(tp, f);
            }
        }
    }

    private void removeCharactersOutsideTheFilterBox()
    {
        int removed = 0;
        int total = 0;

        var visibleArea = getCurrentPage().getCropBox().createRetranslatedRectangle();

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

        if (removed > 0) {
            LOG.debug("Removed {} of {} characters because they are outside the visible area ({})",
                    removed, total, visibleArea, visibleArea);
        }
    }

    @Override
    protected void writeString(String aText, List<TextPosition> aTextPositions) throws IOException
    {
        assertAlignedTextPositions(aText, aTextPositions);

        int unicodeLength;
        assert (unicodeLength = aTextPositions.stream() //
                .map(TextPosition::getVisuallyOrderedUnicode)//
                .map(g -> normalizeWord(g, null))//
                .mapToInt(String::length).sum()) == aText.length() : "Line length ["
                        + aText.length() + "] should match glyph unicode length [" + unicodeLength
                        + "] - [" + aText + "] <-> [" + aTextPositions.stream() //
                                .map(TextPosition::getVisuallyOrderedUnicode) //
                                .map(g -> normalizeWord(g, null)) //
                                .collect(joining())
                        + "]";

        var originalWord = aTextPositions.stream() //
                .map(TextPosition::getVisuallyOrderedUnicode) //
                .collect(joining());
        var glyphOrder = new ArrayList<Integer>();
        var text = normalizeWord(originalWord, glyphOrder);

        assert text.equals(aText) : "Text from PDFbox [" + aText
                + "] should match text from TextPositions [" + text + "]";

        if (glyphOrder.isEmpty()) {
            var cs = new ProtoVChunk(getBuffer().length(), aText, 0, false);

            for (var pos : aTextPositions) {
                // Start a new chunk if the direction changes
                var dir = ((pos.getRotation() - pos.getDir()) + 360) % 360;
                if (dir != cs.dir && !cs.isEmpty()) {
                    chunks.add(cs.endChunk(cs.end, dir));
                }

                Rectangle2D.Double f = fontPositionCache.get(pos);

                // Account for glyphs that were mapped to more than one character by normalization
                // e.g. expanded ligatures
                String normalizedUnicode = normalizeWord(pos.getVisuallyOrderedUnicode(), null);

                normalizedUnicode = reconcileGlyphWithText(aText, false, normalizedUnicode, cs.end);

                var glyph = new VGlyph(cs.offset + cs.end, pageIndex, normalizedUnicode, cs.dir, f);
                cs.addGlyph(glyph);
            }

            if (!cs.isEmpty()) {
                chunks.add(cs.endFinalChunk());
            }
        }
        else {
            // Bidi
            var cs = new ProtoVChunk(getBuffer().length(), aText, glyphOrder.get(0),
                    glyphOrder.get(0) != 0);
            int i = 0;
            for (var pos : aTextPositions) {
                int gPos = glyphOrder.get(i);

                var rtl = cs.isEmpty() ? cs.rtl
                        : (gPos > glyphOrder.get(i - 1) + 1 ? !cs.rtl : cs.rtl);
                var dir = cs.isEmpty() ? cs.dir : ((pos.getRotation() - pos.getDir()) + 360) % 360;
                Rectangle2D.Double f = fontPositionCache.get(pos);

                if ((dir != cs.dir) || (rtl != cs.rtl)) {
                    // Start a new chunk if the direction changes or flips RTL/LTR
                    chunks.add(cs.endChunk(gPos, dir, rtl));
                }
                else {
                    // Also start a new chunk if we cannot properly recover the extends from the
                    // sequence of glyph base positions
                    if (!cs.glyphs.isEmpty()) {
                        var protoGlyph = new VGlyph(0, 0, "", 0, f);
                        var lastGlyph = cs.glyphs.get(cs.glyphs.size() - 1);
                        // Negative gap is an overlap - we also do not want huge overlaps
                        var gap = protoGlyph.getBase()
                                - (lastGlyph.getBase() + lastGlyph.getExtent());
                        // We calculate the tolerance based on the font size (line height)
                        var tolerance = protoGlyph.getCoExtent() * 0.025;
                        if (Math.abs(gap) > tolerance) {
                            LOG.debug("Gap/overlap too large [abs({}) > {}] - starting new chunk",
                                    gap, tolerance);
                            chunks.add(cs.endChunk(gPos, dir, rtl));
                        }
                    }
                }

                // Account for glyphs that were mapped to more than one character by normalization
                // e.g. expanded ligatures
                String normalizedUnicode = normalizeWord(pos.getVisuallyOrderedUnicode(), null);
                var begin = cs.rtl ? gPos - (normalizedUnicode.length() - 1) : gPos;

                normalizedUnicode = reconcileGlyphWithText(aText, rtl, normalizedUnicode, begin);

                var glyph = new VGlyph(cs.offset + begin, pageIndex, normalizedUnicode, cs.dir, f);
                cs.addGlyph(glyph);

                i += normalizedUnicode.length();
            }

            if (!cs.isEmpty()) {
                chunks.add(cs.endFinalChunk());
            }
        }

        super.writeString(aText, aTextPositions);
    }

    @Override
    protected void startDocument(PDDocument aDocument) throws IOException
    {
        if (eventHandler != null) {
            try {
                eventHandler.documentStart();
            }
            catch (Exception e) {
                throw handleEventHandlerException(e);
            }
        }

        super.startDocument(aDocument);
    }

    @Override
    protected void endDocument(PDDocument aDocument) throws IOException
    {
        super.endDocument(aDocument);

        if (eventHandler != null) {
            try {
                eventHandler.documentEnd();
            }
            catch (Exception e) {
                throw handleEventHandlerException(e);
            }
        }
    }

    @Override
    protected void writeParagraphStart() throws IOException
    {
        if (eventHandler != null) {
            try {
                eventHandler.beforeStartParagraph();
            }
            catch (Exception e) {
                throw handleEventHandlerException(e);
            }

        }

        super.writeParagraphStart();

        if (eventHandler != null) {
            try {
                eventHandler.afterStartParagraph();
            }
            catch (Exception e) {
                throw handleEventHandlerException(e);
            }
        }
    }

    @Override
    protected void writeParagraphEnd() throws IOException
    {
        if (eventHandler != null) {
            try {
                eventHandler.beforeEndParagraph();
            }
            catch (Exception e) {
                throw handleEventHandlerException(e);
            }
        }

        super.writeParagraphEnd();

        if (eventHandler != null) {
            try {
                eventHandler.afterEndParagraph();
            }
            catch (Exception e) {
                throw handleEventHandlerException(e);
            }
        }
    }

    @Override
    protected void writePageStart() throws IOException
    {
        if (eventHandler != null) {
            try {
                eventHandler.beforeStartPage();
            }
            catch (Exception e) {
                throw handleEventHandlerException(e);
            }
        }

        super.writePageStart();

        if (eventHandler != null) {
            try {
                eventHandler.afterStartPage();
            }
            catch (Exception e) {
                throw handleEventHandlerException(e);
            }
        }
    }

    @Override
    protected void writePageEnd() throws IOException
    {
        if (eventHandler != null) {
            try {
                eventHandler.beforeEndPage();
            }
            catch (Exception e) {
                throw handleEventHandlerException(e);
            }
        }

        super.writePageEnd();

        if (eventHandler != null) {
            try {
                eventHandler.afterEndPage();
            }
            catch (Exception e) {
                throw handleEventHandlerException(e);
            }
        }
    }

    private IOException handleEventHandlerException(Exception aException)
    {
        if (aException instanceof IOException ioException) {
            return ioException;
        }

        return new IOException(aException);
    }

    private String reconcileGlyphWithText(String aText, boolean rtl, String normalizedUnicode,
            int begin)
    {
        String actualSubstring = aText.substring(begin, begin + normalizedUnicode.length());

        if (normalizedUnicode.length() == 1) {
            assert actualSubstring.equals(normalizedUnicode) : "Glyph [" + normalizedUnicode
                    + "] misaligned with text [" + actualSubstring + "]";
        }
        else {
            if (rtl && normalizedUnicode.length() > 1) {
                char[] utf16Codepoints = normalizedUnicode.toCharArray();
                ArrayUtils.reverse(utf16Codepoints);
                var candidate = new String(utf16Codepoints);
                if (candidate.equals(actualSubstring)) {
                    normalizedUnicode = actualSubstring;
                }
            }

            if (!actualSubstring.equals(normalizedUnicode)) {
                var nue = UnicodeEscaper.above(0).translate(normalizedUnicode);
                var ase = UnicodeEscaper.above(0).translate(actualSubstring);

                LOG.warn(
                        "Glyph [{}]({}) misaligned with text [{}]({}) - using actual text for glyph",
                        normalizedUnicode, nue, actualSubstring, ase);

                normalizedUnicode = actualSubstring;
            }
        }
        return normalizedUnicode;
    }

    private class ProtoVChunk
    {
        final String text;
        final int offset;

        List<VGlyph> glyphs = new ArrayList<>();

        int begin = 0;
        int end = 0;

        float dir = 0;
        boolean rtl = false;

        float x0 = Float.MAX_VALUE;
        float y0 = Float.MAX_VALUE;
        float y1 = Float.MIN_VALUE;
        float x1 = Float.MIN_VALUE;

        public ProtoVChunk(int aOffset, String aText, int aBegin, boolean aRtl)
        {
            offset = aOffset;
            text = aText;
            begin = aBegin;
            end = aBegin;
            rtl = aRtl;

            if (rtl) {
                begin++;
                end++;
            }
        }

        void addGlyph(VGlyph glyph)
        {
            begin = Math.min(begin, glyph.getBegin() - offset);
            end = Math.max(end, glyph.getEnd() - offset);

            x1 = Math.max(x1, glyph.getFontX() + glyph.getFontWidth());
            y1 = Math.max(y1, glyph.getFontY() + glyph.getFontHeight());
            x0 = Math.min(x0, glyph.getFontX());
            y0 = Math.min(y0, glyph.getFontY());

            glyphs.add(glyph);
        }

        VChunk endFinalChunk()
        {
            return endChunk(0, 0, false);
        }

        VChunk endChunk(int aBegin, float aDir)
        {
            return endChunk(aBegin, aDir, false);
        }

        VChunk endChunk(int aNewStart, float aNewDir, boolean aNewRtl)
        {
            var w = x1 - x0;
            var h = y1 - y0;

            glyphs.sort(comparing(VGlyph::getBegin));

            var chunkText = text.substring(begin, end);
            VChunk chunk = new VChunk(offset + begin, offset + end, chunkText, dir, x0, y0, w, h,
                    glyphs);

            begin = aNewStart;
            end = aNewStart;

            dir = aNewDir;
            rtl = aNewRtl;
            glyphs = new ArrayList<>();
            x0 = Float.MAX_VALUE;
            y0 = Float.MAX_VALUE;
            y1 = Float.MIN_VALUE;
            x1 = Float.MIN_VALUE;

            if (rtl) {
                begin++;
                end++;
            }

            return chunk;
        }

        boolean isEmpty()
        {
            return begin == end;
        }
    }

    private void assertAlignedTextPositions(String aText, List<TextPosition> aTextPositions)
    {
        int cumulativePositionLength = aTextPositions.stream()
                .mapToInt(t -> normalizeWord(t.getVisuallyOrderedUnicode(), null).length()) //
                .sum();

        if (aText.length() != cumulativePositionLength) {
            System.out.printf("%d (%d) vs %d (%d)%n", aText.length(), aText.codePoints().count(),
                    cumulativePositionLength, aTextPositions.size());
            System.out.println(" Text [" + aText + "]");
            StringBuilder sb = new StringBuilder();
            for (TextPosition p : aTextPositions) {
                sb.append(p.getVisuallyOrderedUnicode());
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
