/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.pdfeditor2.deprecated;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.fontbox.util.BoundingBox;
import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDCIDFont;
import org.apache.pdfbox.pdmodel.font.PDCIDFontType2;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;
import org.apache.pdfbox.pdmodel.font.PDSimpleFont;
import org.apache.pdfbox.pdmodel.font.PDTrueTypeFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType3Font;
import org.apache.pdfbox.pdmodel.font.encoding.GlyphList;
import org.apache.pdfbox.pdmodel.graphics.state.PDGraphicsState;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapts code from the PDFBox {@code LegacyPDFStreamEngine} but uses it in the context of a
 * {@link PDFGraphicsStreamEngine}.
 */
@Deprecated
public abstract class LegacyPDFAndPDFGraphicsStreamEngine
    extends PDFGraphicsStreamEngineAdapter
{
    private static final Logger LOG = LoggerFactory
            .getLogger(LegacyPDFAndPDFGraphicsStreamEngine.class);

    private static final GlyphList GLYPHLIST;

    private final Map<COSDictionary, Float> fontHeightMap = new WeakHashMap<COSDictionary, Float>();

    private int pageRotation;
    private PDRectangle pageSize;
    private Matrix translateMatrix;

    // Source:
    // https://github.com/apache/pdfbox/blob/10d1e91af4eb9a06af7e95460533bf3ebc1b1280/pdfbox/src/main/java/org/apache/pdfbox/text/LegacyPDFStreamEngine.java#L89
    static {
        // load additional glyph list for Unicode mapping
        String path = "/org/apache/pdfbox/resources/glyphlist/additional.txt";
        InputStream input = GlyphList.class.getResourceAsStream(path);
        try {
            GLYPHLIST = new GlyphList(GlyphList.getAdobeGlyphList(), input);
            input.close();
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    protected LegacyPDFAndPDFGraphicsStreamEngine()
    {
        super(null);
    }

    // source:
    // https://github.com/apache/pdfbox/blob/10d1e91af4eb9a06af7e95460533bf3ebc1b1280/pdfbox/src/main/java/org/apache/pdfbox/text/LegacyPDFStreamEngine.java#L141
    /**
     * This will initialize and process the contents of the stream.
     *
     * @param page
     *            the page to process
     * @throws java.io.IOException
     *             if there is an error accessing the stream.
     */
    @Override
    public void processPage(PDPage page) throws IOException
    {
        this.pageRotation = page.getRotation();
        this.pageSize = page.getCropBox();

        if (pageSize.getLowerLeftX() == 0 && pageSize.getLowerLeftY() == 0) {
            translateMatrix = null;
        }
        else {
            // translation matrix for cropbox
            translateMatrix = Matrix.getTranslateInstance(-pageSize.getLowerLeftX(),
                    -pageSize.getLowerLeftY());
        }
        super.processPage(page);
    }

    // source:
    // https://github.com/apache/pdfbox/blob/10d1e91af4eb9a06af7e95460533bf3ebc1b1280/pdfbox/src/main/java/org/apache/pdfbox/text/LegacyPDFStreamEngine.java#L158
    /**
     * Called when a glyph is to be processed. The heuristic calculations here were originally
     * written by Ben Litchfield for PDFStreamEngine.
     */
    @Override
    protected void showGlyph(Matrix textRenderingMatrix, PDFont font, int code, Vector displacement)
        throws IOException
    {
        //
        // legacy calculations which were previously in PDFStreamEngine
        //
        // DO NOT USE THIS CODE UNLESS YOU ARE WORKING WITH PDFTextStripper.
        // THIS CODE IS DELIBERATELY INCORRECT
        //

        PDGraphicsState state = getGraphicsState();
        Matrix ctm = state.getCurrentTransformationMatrix();
        float fontSize = state.getTextState().getFontSize();
        float horizontalScaling = state.getTextState().getHorizontalScaling() / 100f;
        Matrix textMatrix = getTextMatrix();

        float displacementX = displacement.getX();
        // the sorting algorithm is based on the width of the character. As the displacement
        // for vertical characters doesn't provide any suitable value for it, we have to
        // calculate our own
        if (font.isVertical()) {
            displacementX = font.getWidth(code) / 1000;
            // there may be an additional scaling factor for true type fonts
            TrueTypeFont ttf = null;
            if (font instanceof PDTrueTypeFont) {
                ttf = ((PDTrueTypeFont) font).getTrueTypeFont();
            }
            else if (font instanceof PDType0Font) {
                PDCIDFont cidFont = ((PDType0Font) font).getDescendantFont();
                if (cidFont instanceof PDCIDFontType2) {
                    ttf = ((PDCIDFontType2) cidFont).getTrueTypeFont();
                }
            }
            if (ttf != null && ttf.getUnitsPerEm() != 1000) {
                displacementX *= 1000f / ttf.getUnitsPerEm();
            }
        }

        //
        // legacy calculations which were previously in PDFStreamEngine
        //
        // DO NOT USE THIS CODE UNLESS YOU ARE WORKING WITH PDFTextStripper.
        // THIS CODE IS DELIBERATELY INCORRECT
        //

        // (modified) combined displacement, this is calculated *without* taking the character
        // spacing and word spacing into account, due to legacy code in TextStripper
        float tx = displacementX * fontSize * horizontalScaling;
        float ty = displacement.getY() * fontSize;

        // (modified) combined displacement matrix
        Matrix td = Matrix.getTranslateInstance(tx, ty);

        // (modified) text rendering matrix
        Matrix nextTextRenderingMatrix = td.multiply(textMatrix).multiply(ctm); // text space ->
                                                                                // device space
        float nextX = nextTextRenderingMatrix.getTranslateX();
        float nextY = nextTextRenderingMatrix.getTranslateY();

        // (modified) width and height calculations
        float dxDisplay = nextX - textRenderingMatrix.getTranslateX();
        Float fontHeight = fontHeightMap.get(font.getCOSObject());
        if (fontHeight == null) {
            fontHeight = computeFontHeight(font);
            fontHeightMap.put(font.getCOSObject(), fontHeight);
        }
        float dyDisplay = fontHeight * textRenderingMatrix.getScalingFactorY();

        //
        // start of the original method
        //

        // Note on variable names. There are three different units being used in this code.
        // Character sizes are given in glyph units, text locations are initially given in text
        // units, and we want to save the data in display units. The variable names should end with
        // Text or Disp to represent if the values are in text or disp units (no glyph units are
        // saved).

        float glyphSpaceToTextSpaceFactor = 1 / 1000f;
        if (font instanceof PDType3Font) {
            glyphSpaceToTextSpaceFactor = font.getFontMatrix().getScaleX();
        }

        float spaceWidthText = 0;
        try {
            // to avoid crash as described in PDFBOX-614, see what the space displacement should be
            spaceWidthText = font.getSpaceWidth() * glyphSpaceToTextSpaceFactor;
        }
        catch (Throwable exception) {
            LOG.warn("Unable to calculate spaceWidthText", exception);
        }

        if (spaceWidthText == 0) {
            spaceWidthText = font.getAverageFontWidth() * glyphSpaceToTextSpaceFactor;
            // the average space width appears to be higher than necessary so make it smaller
            spaceWidthText *= .80f;
        }
        if (spaceWidthText == 0) {
            spaceWidthText = 1.0f; // if could not find font, use a generic value
        }

        // the space width has to be transformed into display units
        float spaceWidthDisplay = spaceWidthText * textRenderingMatrix.getScalingFactorX();

        // use our additional glyph list for Unicode mapping
        String unicodeMapping = font.toUnicode(code, GLYPHLIST);

        // when there is no Unicode mapping available, Acrobat simply coerces the character code
        // into Unicode, so we do the same. Subclasses of PDFStreamEngine don't necessarily want
        // this, which is why we leave it until this point in PDFTextStreamEngine.
        if (unicodeMapping == null) {
            if (font instanceof PDSimpleFont) {
                char c = (char) code;
                unicodeMapping = new String(new char[] { c });
            }
            else {
                // Acrobat doesn't seem to coerce composite font's character codes, instead it
                // skips them. See the "allah2.pdf" TestTextStripper file.
                return;
            }
        }

        // adjust for cropbox if needed
        Matrix translatedTextRenderingMatrix;
        if (translateMatrix == null) {
            translatedTextRenderingMatrix = textRenderingMatrix;
        }
        else {
            translatedTextRenderingMatrix = Matrix.concatenate(translateMatrix,
                    textRenderingMatrix);
            nextX -= pageSize.getLowerLeftX();
            nextY -= pageSize.getLowerLeftY();
        }

        processTextPosition(
                new TextPosition(pageRotation, pageSize.getWidth(), pageSize.getHeight(),
                        translatedTextRenderingMatrix, nextX, nextY, Math.abs(dyDisplay), dxDisplay,
                        Math.abs(spaceWidthDisplay), unicodeMapping, new int[] { code }, font,
                        fontSize, (int) (fontSize * textMatrix.getScalingFactorX()))
        // , textRenderingMatrix
        );
    }

    // Source:
    // https://github.com/apache/pdfbox/blob/10d1e91af4eb9a06af7e95460533bf3ebc1b1280/pdfbox/src/main/java/org/apache/pdfbox/text/LegacyPDFStreamEngine.java#L329
    /**
     * Compute the font height. Override this if you want to use own calculations.
     * 
     * @param font
     *            the font.
     * @return the font height.
     * 
     * @throws IOException
     *             if there is an error while getting the font bounding box.
     */
    protected float computeFontHeight(PDFont font) throws IOException
    {
        BoundingBox bbox = font.getBoundingBox();
        if (bbox.getLowerLeftY() < Short.MIN_VALUE) {
            // PDFBOX-2158 and PDFBOX-3130
            // files by Salmat eSolutions / ClibPDF Library
            bbox.setLowerLeftY(-(bbox.getLowerLeftY() + 65536));
        }
        // 1/2 the bbox is used as the height todo: why?
        float glyphHeight = bbox.getHeight() / 2;

        // sometimes the bbox has very high values, but CapHeight is OK
        PDFontDescriptor fontDescriptor = font.getFontDescriptor();
        if (fontDescriptor != null) {
            float capHeight = fontDescriptor.getCapHeight();
            if (Float.compare(capHeight, 0) != 0
                    && (capHeight < glyphHeight || Float.compare(glyphHeight, 0) == 0)) {
                glyphHeight = capHeight;
            }
            // PDFBOX-3464, PDFBOX-4480, PDFBOX-4553:
            // sometimes even CapHeight has very high value, but Ascent and Descent are ok
            float ascent = fontDescriptor.getAscent();
            float descent = fontDescriptor.getDescent();
            if (capHeight > ascent && ascent > 0 && descent < 0
                    && ((ascent - descent) / 2 < glyphHeight
                            || Float.compare(glyphHeight, 0) == 0)) {
                glyphHeight = (ascent - descent) / 2;
            }
        }

        // transformPoint from glyph space -> text space
        float height;
        if (font instanceof PDType3Font) {
            height = font.getFontMatrix().transformPoint(0, glyphHeight).y;
        }
        else {
            height = glyphHeight / 1000;
        }

        return height;
    }

    @Override
    public Point2D getCurrentPoint() throws IOException
    {
        return new Point2D.Float(0.0f, 0.0f);
    }

    protected abstract void processTextPosition(
            TextPosition aTextPosition /* , Matrix aTextRenderingMatrix */)
        throws IOException;

}
