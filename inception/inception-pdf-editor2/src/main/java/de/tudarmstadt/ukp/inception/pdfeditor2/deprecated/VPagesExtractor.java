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
package de.tudarmstadt.ukp.inception.pdfeditor2.deprecated;

import static de.tudarmstadt.ukp.inception.pdfeditor2.pdfbox.GlyphPositionUtils.calculateFontBounds;
import static de.tudarmstadt.ukp.inception.pdfeditor2.pdfbox.GlyphPositionUtils.makeFlipAT;
import static de.tudarmstadt.ukp.inception.pdfeditor2.pdfbox.GlyphPositionUtils.makeRotateAT;
import static de.tudarmstadt.ukp.inception.pdfeditor2.pdfbox.GlyphPositionUtils.makeTransAT;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.TextPosition;

import de.tudarmstadt.ukp.inception.pdfeditor2.visual.model.VGlyph;
import de.tudarmstadt.ukp.inception.pdfeditor2.visual.model.VPage;

@Deprecated
public class VPagesExtractor
    extends LegacyPDFAndPDFGraphicsStreamEngine
{
    private final PDDocument document;
    private final List<VPage> pages = new ArrayList<>();

    private AffineTransform flipAT;
    private AffineTransform rotateAT;
    private AffineTransform transAT;

    private int pageIndex;
    private List<VGlyph> glyphPositions;

    public VPagesExtractor(PDDocument aDocument)
    {
        document = aDocument;
    }

    @Override
    public void processPage(PDPage aPage) throws IOException
    {
        glyphPositions = new ArrayList<>();
        flipAT = makeFlipAT(aPage);
        rotateAT = makeRotateAT(aPage);
        transAT = makeTransAT(aPage);

        // We must fetch the index here at the start, otherwise the index may be off, e.g. if there
        // are empty pages in the PDF
        pageIndex = document.getPages().indexOf(aPage);

        super.processPage(aPage);

        pages.add(new VPage(pageIndex, -1, -1, null, glyphPositions.toArray(VGlyph[]::new)));
    }

    @Override
    protected void processTextPosition(TextPosition text/* , Matrix aTextRenderingMatrix */)
        throws IOException

    {
        // font coordinates
        Shape fontShape = calculateFontBounds(text, flipAT, rotateAT);
        Rectangle2D.Double f = (Rectangle2D.Double) fontShape.getBounds2D();

        // glyph coordinates
        /*
        @formatter:off
        Shape glyphShape = calculateGlyphBounds(aTextRenderingMatrix, text.getFont(),
                text.getCharacterCodes()[0]);
        glyphShape = flipAT.createTransformedShape(glyphShape);
        glyphShape = rotateAT.createTransformedShape(glyphShape);
        glyphShape = transAT.createTransformedShape(glyphShape);
        Rectangle2D.Double g = (Rectangle2D.Double) glyphShape.getBounds2D();
        @formatter:on
         */

        VGlyph p = new VGlyph(glyphPositions.size(), pageIndex, text.getUnicode(), f/* , g */);
        glyphPositions.add(p);
    }

    public List<VPage> getPages()
    {
        return pages;
    }
}
