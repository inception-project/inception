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
package de.tudarmstadt.ukp.inception.pdfeditor2.pdfbox;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.text.Normalizer;

import org.apache.fontbox.util.BoundingBox;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType3Font;
import org.apache.pdfbox.text.TextPosition;

public class GlyphPositionUtils
{
    // Source:
    // https://github.com/apache/pdfbox/blob/10d1e91af4eb9a06af7e95460533bf3ebc1b1280/examples/src/main/java/org/apache/pdfbox/examples/util/DrawPrintTextLocations.java#L220
    public static AffineTransform makeFlipAT(PDPage pdPage)
    {
        // flip y-axis
        AffineTransform flipAT = new AffineTransform();
        flipAT.translate(0, pdPage.getBBox().getHeight());
        flipAT.scale(1, -1);

        return flipAT;
    }

    // Source:
    // https://github.com/apache/pdfbox/blob/10d1e91af4eb9a06af7e95460533bf3ebc1b1280/examples/src/main/java/org/apache/pdfbox/examples/util/DrawPrintTextLocations.java#L225
    public static AffineTransform makeRotateAT(PDPage pdPage)
    {
        // page may be rotated
        AffineTransform rotateAT = new AffineTransform();
        int rotation = pdPage.getRotation();
        if (rotation != 0) {
            PDRectangle mediaBox = pdPage.getMediaBox();
            switch (rotation) {
            case 90:
                rotateAT.translate(mediaBox.getHeight(), 0);
                break;
            case 270:
                rotateAT.translate(0, mediaBox.getWidth());
                break;
            case 180:
                rotateAT.translate(mediaBox.getWidth(), mediaBox.getHeight());
                break;
            default:
                break;
            }
            rotateAT.rotate(Math.toRadians(rotation));
        }
        return rotateAT;
    }

    // Source:
    // https://github.com/apache/pdfbox/blob/10d1e91af4eb9a06af7e95460533bf3ebc1b1280/examples/src/main/java/org/apache/pdfbox/examples/util/DrawPrintTextLocations.java#L248
    public static AffineTransform makeTransAT(PDPage pdPage)
    {
        PDRectangle cropBox = pdPage.getCropBox();
        return AffineTransform.getTranslateInstance(-cropBox.getLowerLeftX(),
                cropBox.getLowerLeftY());
    }

    // Source:
    // https://github.com/apache/pdfbox/blob/10d1e91af4eb9a06af7e95460533bf3ebc1b1280/examples/src/main/java/org/apache/pdfbox/examples/util/DrawPrintTextLocations.java#L290
    public static Shape calculateFontBounds(TextPosition text, AffineTransform flipAT,
            AffineTransform rotateAT)
        throws IOException
    {

        // for (TextPosition text : textPositions)
        // {
        // System.out.println("String[" + text.getXDirAdj() + ","
        // + text.getYDirAdj() + " fs=" + text.getFontSize() + " xscale="
        // + text.getXScale() + " height=" + text.getHeightDir() + " space="
        // + text.getWidthOfSpace() + " width="
        // + text.getWidthDirAdj() + "]" + text.getUnicode());

        // glyph space -> user space
        // note: text.getTextMatrix() is *not* the Text Matrix, it's the Text Rendering Matrix
        AffineTransform at = text.getTextMatrix().createAffineTransform();

        // in red:
        // show rectangles with the "height" (not a real height, but used for text extraction
        // heuristics, it is 1/2 of the bounding box height and starts at y=0)
        Rectangle2D.Float rect = new Rectangle2D.Float(0, 0,
                text.getWidthDirAdj() / text.getTextMatrix().getScalingFactorX(),
                text.getHeightDir() / text.getTextMatrix().getScalingFactorY());
        Shape s = at.createTransformedShape(rect);
        s = flipAT.createTransformedShape(s);
        s = rotateAT.createTransformedShape(s);
        // g2d.setColor(Color.red);
        // g2d.draw(s);

        // in blue:
        // show rectangle with the real vertical bounds, based on the font bounding box y values
        // usually, the height is identical to what you see when marking text in Adobe Reader
        PDFont font = text.getFont();
        BoundingBox bbox = font.getBoundingBox();

        // advance width, bbox height (glyph space)
        // todo: should iterate all chars
        float xadvance = font.getWidth(text.getCharacterCodes()[0]);
        rect = new Rectangle2D.Float(0, bbox.getLowerLeftY(), xadvance, bbox.getHeight());

        if (font instanceof PDType3Font) {
            // bbox and font matrix are unscaled
            at.concatenate(font.getFontMatrix().createAffineTransform());
        }
        else {
            // bbox and font matrix are already scaled to 1000
            at.scale(1 / 1000f, 1 / 1000f);
        }
        s = at.createTransformedShape(rect);
        s = flipAT.createTransformedShape(s);
        s = rotateAT.createTransformedShape(s);

        // g2d.setColor(Color.blue);
        // g2d.draw(s);

        return s;
        // }
    }

    // source:
    // https://github.com/apache/pdfbox/blob/10d1e91af4eb9a06af7e95460533bf3ebc1b1280/pdfbox/src/main/java/org/apache/pdfbox/text/PDFTextStripper.java#L1911
    /**
     * Normalize certain Unicode characters. For example, convert the single "fi" ligature to "f"
     * and "i". Also normalises Arabic and Hebrew presentation forms.
     *
     * @param word
     *            Word to normalize
     * @return Normalized word
     */
    public static String normalizeWord(String word)
    {
        StringBuilder builder = null;
        int p = 0;
        int q = 0;
        int strLength = word.length();
        for (; q < strLength; q++) {
            // We only normalize if the codepoint is in a given range.
            // Otherwise, NFKC converts too many things that would cause
            // confusion. For example, it converts the micro symbol in
            // extended Latin to the value in the Greek script. We normalize
            // the Unicode Alphabetic and Arabic A&B Presentation forms.
            char c = word.charAt(q);
            if (0xFB00 <= c && c <= 0xFDFF || 0xFE70 <= c && c <= 0xFEFF) {
                if (builder == null) {
                    builder = new StringBuilder(strLength * 2);
                }
                builder.append(word, p, q);
                // Some fonts map U+FDF2 differently than the Unicode spec.
                // They add an extra U+0627 character to compensate.
                // This removes the extra character for those fonts.
                if (c == 0xFDF2 && q > 0
                        && (word.charAt(q - 1) == 0x0627 || word.charAt(q - 1) == 0xFE8D)) {
                    builder.append("\u0644\u0644\u0647");
                }
                else {
                    // Trim because some decompositions have an extra space, such as U+FC5E
                    builder.append(Normalizer
                            .normalize(word.substring(q, q + 1), Normalizer.Form.NFKC).trim());
                }
                p = q + 1;
            }
        }

        if (builder == null) {
            return word;
            // return handleDirection(word);
        }
        else {
            builder.append(word, p, q);
            return builder.toString();
            // return handleDirection(builder.toString());
        }
    }
}
