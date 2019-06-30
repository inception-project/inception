/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Hiroyuki Shindo
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package de.tudarmstadt.ukp.inception.pdfeditor.pdfextract;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.fontbox.util.BoundingBox;
import org.apache.pdfbox.contentstream.PDFGraphicsStreamEngine;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDCIDFont;
import org.apache.pdfbox.pdmodel.font.PDCIDFontType2;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;
import org.apache.pdfbox.pdmodel.font.PDSimpleFont;
import org.apache.pdfbox.pdmodel.font.PDTrueTypeFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType3CharProc;
import org.apache.pdfbox.pdmodel.font.PDType3Font;
import org.apache.pdfbox.pdmodel.font.PDVectorFont;
import org.apache.pdfbox.pdmodel.font.encoding.GlyphList;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.pdmodel.graphics.state.PDGraphicsState;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector;

public class PDFExtractor
    extends PDFGraphicsStreamEngine
{

    public static void main(String[] args)
    {
        File path = args.length == 0 ? new File(".") : new File(args[0]);
        if (path.isDirectory()) {
            for (File file : path.listFiles()) {
                if (file.isFile() && file.getName().endsWith(".pdf")) {
                    processFile(file);
                }
            }
        }
        else {
            if (path.isFile()) {
                processFile(path);
            }
        }
    }

    public static String processFileToString(File file, boolean writeGlyphCoords) throws IOException
    {
        try (StringWriter w = new StringWriter(); PDDocument doc = PDDocument.load(file)) {

            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                boolean isLastPage = i == doc.getNumberOfPages() - 1;
                PDFExtractor ext = new PDFExtractor(doc.getPage(i), i + 1, w, isLastPage);
                ext.setWriteGlyphCoords(writeGlyphCoords);
                ext.processPage(doc.getPage(i));
                ext.write();
            }
            return w.toString();
        }
    }

    private static void processFile(File file)
    {
        String outPath = String.format("%s.0-3-1.txt.gz", file);
        try (PDDocument doc = PDDocument.load(file);
            GZIPOutputStream gzip = new GZIPOutputStream(new FileOutputStream(outPath));
            Writer w = new BufferedWriter(new OutputStreamWriter(gzip, "UTF-8"));
        ) {
            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                boolean isLastPage = i == doc.getNumberOfPages() - 1;
                PDFExtractor ext = new PDFExtractor(doc.getPage(i), i + 1, w, isLastPage);
                ext.processPage(doc.getPage(i));
                ext.write();
            }
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private boolean writeGlyphCoords;
    private Writer output;
    private int pageIndex;
    private int pageRotation;
    private PDRectangle pageSize;
    private Matrix translateMatrix;
    private final GlyphList glyphList;
    private List<Object> buffer = new ArrayList<>();
    private boolean isLastPage;

    private AffineTransform flipAT;
    private AffineTransform rotateAT;
    private AffineTransform transAT;

    public PDFExtractor(PDPage page, int pageIndex, Writer output, boolean aIsLastPage)
        throws IOException
    {
        super(page);
        this.pageIndex = pageIndex;
        this.output = output;
        this.writeGlyphCoords = true;
        this.isLastPage = aIsLastPage;

        String path = "org/apache/pdfbox/resources/glyphlist/additional.txt";
        InputStream input = GlyphList.class.getClassLoader().getResourceAsStream(path);
        this.glyphList = new GlyphList(GlyphList.getAdobeGlyphList(), input);

        this.pageRotation = page.getRotation();
        this.pageSize = page.getCropBox();
        if (this.pageSize.getLowerLeftX() == 0.0F && this.pageSize.getLowerLeftY() == 0.0F) {
            this.translateMatrix = null;
        }
        else {
            this.translateMatrix = Matrix.getTranslateInstance(-this.pageSize.getLowerLeftX(),
                -this.pageSize.getLowerLeftY());
        }

        // taken from DrawPrintTextLocations for setting flipAT, rotateAT and transAT
        PDRectangle cropBox = page.getCropBox();
        // flip y-axis
        flipAT = new AffineTransform();
        flipAT.translate(0, page.getBBox().getHeight());
        flipAT.scale(1, -1);

        // page may be rotated
        rotateAT = new AffineTransform();
        int rotation = page.getRotation();
        if (rotation != 0) {
            PDRectangle mediaBox = page.getMediaBox();
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
        // cropbox
        transAT = AffineTransform
            .getTranslateInstance(-cropBox.getLowerLeftX(), cropBox.getLowerLeftY());
    }

    float getPageHeight()
    {
        return getPage().getCropBox().getHeight();
    }

    private void addDraw(String op, float... values)
    {
        buffer.add(new DrawOperator(op, values));
    }

    private void write() throws IOException
    {
        // Add a last dummy line, required for zero-width span annotations
        if (isLastPage) {
            ListIterator li = buffer.listIterator(buffer.size());
            while (li.hasPrevious()) {
                int index = li.previousIndex();
                Object obj = buffer.get(index);
                if (obj instanceof TextOperator) {
                    TextOperator t = (TextOperator) obj;
                    buffer.add(index + 1, new TextOperator(" ",
                        t.fx + t.fw, t.fy, 5, t.fh, t.gx + t.gw, t.gy, 5, t.gh));
                    break;
                }
            }
        }

        for (Object obj : buffer) {
            if (obj instanceof TextOperator) {
                TextOperator t = (TextOperator) obj;
                output.write(String
                    .format("%s\t%s\t%s %s %s %s", pageIndex, t.unicode, t.fx, t.fy, t.fw, t.fh));
                if (isWriteGlyphCoords()) {
                    output.write(String.format("\t%s %s %s %s", t.gx, t.gy, t.gw, t.gh));
                }
            }
            else if (obj instanceof DrawOperator) {
                DrawOperator d = (DrawOperator) obj;
                output.write(String.format("%s\t[%s]", pageIndex, d.type));
                if (d.values.length > 0) {
                    output.write(String.format("\t%s", d.values[0]));
                }
                for (int k = 1; k < d.values.length; k++) {
                    output.write(String.format(" %s", d.values[k]));
                }
            }
            output.write("\n");
        }
    }

    @Override public void drawImage(PDImage pdImage)
    {
    }

    @Override public void appendRectangle(Point2D p0, Point2D p1, Point2D p2, Point2D p3)
    {
        float h = getPageHeight();
        addDraw("RECTANGLE", (float) p0.getX(), h - (float) p0.getY(), (float) p1.getX(),
            h - (float) p1.getY(), (float) p2.getX(), h - (float) p2.getY(), (float) p3.getX(),
            h - (float) p3.getY());
    }

    @Override public void clip(int i)
    {
    }

    @Override public void moveTo(float x, float y)
    {
        addDraw("MOVE_TO", x, getPageHeight() - y);
    }

    @Override public void lineTo(float x, float y)
    {
        addDraw("LINE_TO", x, getPageHeight() - y);
    }

    @Override public void curveTo(float x1, float y1, float x2, float y2, float x3, float y3)
    {
        float h = getPageHeight();
        addDraw("CURVE_TO", x1, h - y1, x2, h - y2, x3, h - y3);
    }

    @Override public Point2D getCurrentPoint()
    {
        return new Point2D.Float(0.0f, 0.0f);
    }

    @Override public void closePath()
    {
    }

    @Override public void endPath()
    {
    }

    @Override public void strokePath()
    {
        addDraw("STROKE_PATH");
    }

    @Override public void fillPath(int i)
    {
        addDraw("FILL_PATH");
    }

    @Override public void fillAndStrokePath(int i)
    {
        addDraw("FILL_STROKE_PATH");
    }

    @Override public void shadingFill(COSName cosName)
    {
    }

    @Override public void showFontGlyph(Matrix textRenderingMatrix, PDFont font, int code,
        String unicode, Vector displacement) throws IOException
    {
        // taken from LegacyPDFStreamEngine.showGlyph
        PDGraphicsState state = this.getGraphicsState();
        Matrix ctm = state.getCurrentTransformationMatrix();
        float fontSize = state.getTextState().getFontSize();
        float horizontalScaling = state.getTextState().getHorizontalScaling() / 100.0F;
        Matrix textMatrix = this.getTextMatrix();
        BoundingBox bbox = font.getBoundingBox();
        if (bbox.getLowerLeftY() < -32768.0F) {
            bbox.setLowerLeftY(-(bbox.getLowerLeftY() + 65536.0F));
        }

        float glyphHeight = bbox.getHeight() / 2.0F;
        PDFontDescriptor fontDescriptor = font.getFontDescriptor();
        float height;
        if (fontDescriptor != null) {
            height = fontDescriptor.getCapHeight();
            if (height != 0.0F && (height < glyphHeight || glyphHeight == 0.0F)) {
                glyphHeight = height;
            }
        }

        height = glyphHeight / 1000.0F;

        float displacementX = displacement.getX();
        if (font.isVertical()) {
            displacementX = font.getWidth(code) / 1000.0F;
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
                displacementX *= 1000.0F / (float) ttf.getUnitsPerEm();
            }
        }

        float tx = displacementX * fontSize * horizontalScaling;
        float ty = displacement.getY() * fontSize;
        Matrix td = Matrix.getTranslateInstance(tx, ty);
        Matrix nextTextRenderingMatrix = td.multiply(textMatrix).multiply(ctm);
        float nextX = nextTextRenderingMatrix.getTranslateX();
        float nextY = nextTextRenderingMatrix.getTranslateY();
        float dxDisplay = nextX - textRenderingMatrix.getTranslateX();
        float dyDisplay = height * textRenderingMatrix.getScalingFactorY();
        float glyphSpaceToTextSpaceFactor = 0.001F;
        if (font instanceof PDType3Font) {
            glyphSpaceToTextSpaceFactor = font.getFontMatrix().getScaleX();
        }

        float spaceWidthText = 0.0F;
        try {
            spaceWidthText = font.getSpaceWidth() * glyphSpaceToTextSpaceFactor;
        }
        catch (Throwable e) {
            //LOG.warn(e, e);
        }

        if (spaceWidthText == 0.0F) {
            spaceWidthText = font.getAverageFontWidth() * glyphSpaceToTextSpaceFactor;
            spaceWidthText *= 0.8F;
        }
        if (spaceWidthText == 0.0F) {
            spaceWidthText = 1.0F;
        }

        float spaceWidthDisplay = spaceWidthText * textRenderingMatrix.getScalingFactorX();
        unicode = font.toUnicode(code, this.glyphList);
        if (unicode == null) {
            unicode = "NO_UNICODE";
        }
        else if (StringUtils.isBlank(unicode)) {
            return;
        }

        Matrix translatedTextRenderingMatrix;
        if (this.translateMatrix == null) {
            translatedTextRenderingMatrix = textRenderingMatrix;
        }
        else {
            translatedTextRenderingMatrix = Matrix
                .concatenate(this.translateMatrix, textRenderingMatrix);
            nextX -= this.pageSize.getLowerLeftX();
            nextY -= this.pageSize.getLowerLeftY();
        }

        TextPosition text = new TextPosition(pageRotation, pageSize.getWidth(),
            pageSize.getHeight(), translatedTextRenderingMatrix, nextX, nextY, Math.abs(dyDisplay),
            dxDisplay, Math.abs(spaceWidthDisplay), unicode, new int[] { code }, font, fontSize,
            (int) (fontSize * textMatrix.getScalingFactorX()));

        Shape fontShape = calculateFontBounds(text);
        Rectangle2D.Double f = (Rectangle2D.Double) fontShape.getBounds2D(); // font coordinates
        Shape glyphShape = calculateGlyphBounds(textRenderingMatrix, font, code);
        Rectangle2D.Double g = (Rectangle2D.Double) glyphShape.getBounds2D(); // glyph coordinates
        TextOperator t = new TextOperator(unicode, (float) f.x, (float) f.y, (float) f.width,
            (float) f.height, (float) g.x, (float) g.y, (float) g.width, (float) g.height);
        buffer.add(t);
    }

    // taken from writeString in DrawPrintTextLocations
    private Shape calculateFontBounds(TextPosition text) throws IOException
    {
        // glyph space -> user space
        // note: text.getTextMatrix() is *not* the Text Matrix, it's the Text Rendering Matrix
        AffineTransform at = text.getTextMatrix().createAffineTransform();

        // show rectangle with the real vertical bounds, based on the font bounding box y values
        // usually, the height is identical to what you see when marking text in Adobe Reader
        PDFont font = text.getFont();
        BoundingBox bbox = font.getBoundingBox();

        // advance width, bbox height (glyph space)
        float xadvance = font
            .getWidth(text.getCharacterCodes()[0]); // todo: should iterate all chars
        Rectangle2D.Float rect = new Rectangle2D.Float(0, bbox.getLowerLeftY(), xadvance,
            bbox.getHeight());

        if (font instanceof PDType3Font) {
            // bbox and font matrix are unscaled
            at.concatenate(font.getFontMatrix().createAffineTransform());
        }
        else {
            // bbox and font matrix are already scaled to 1000
            at.scale(1 / 1000f, 1 / 1000f);
        }
        Shape s = at.createTransformedShape(rect);
        s = flipAT.createTransformedShape(s);
        s = rotateAT.createTransformedShape(s);
        return s;
    }

    // taken from DrawPrintTextLocations.java
    // this calculates the real (except for type 3 fonts) individual glyph bounds
    private Shape calculateGlyphBounds(Matrix textRenderingMatrix, PDFont font, int code)
        throws IOException
    {
        GeneralPath path = null;
        AffineTransform at = textRenderingMatrix.createAffineTransform();
        at.concatenate(font.getFontMatrix().createAffineTransform());
        if (font instanceof PDType3Font) {
            // It is difficult to calculate the real individual glyph bounds for type 3 fonts
            // because these are not vector fonts, the content stream could contain almost anything
            // that is found in page content streams.
            PDType3Font t3Font = (PDType3Font) font;
            PDType3CharProc charProc = t3Font.getCharProc(code);
            if (charProc != null) {
                BoundingBox fontBBox = t3Font.getBoundingBox();
                PDRectangle glyphBBox = charProc.getGlyphBBox();
                if (glyphBBox != null) {
                    // PDFBOX-3850: glyph bbox could be larger than the font bbox
                    glyphBBox.setLowerLeftX(
                        Math.max(fontBBox.getLowerLeftX(), glyphBBox.getLowerLeftX()));
                    glyphBBox.setLowerLeftY(
                        Math.max(fontBBox.getLowerLeftY(), glyphBBox.getLowerLeftY()));
                    glyphBBox.setUpperRightX(
                        Math.min(fontBBox.getUpperRightX(), glyphBBox.getUpperRightX()));
                    glyphBBox.setUpperRightY(
                        Math.min(fontBBox.getUpperRightY(), glyphBBox.getUpperRightY()));
                    path = glyphBBox.toGeneralPath();
                }
            }
        }
        else if (font instanceof PDVectorFont) {
            PDVectorFont vectorFont = (PDVectorFont) font;
            path = vectorFont.getPath(code);

            if (font instanceof PDTrueTypeFont) {
                PDTrueTypeFont ttFont = (PDTrueTypeFont) font;
                int unitsPerEm = ttFont.getTrueTypeFont().getHeader().getUnitsPerEm();
                at.scale(1000d / unitsPerEm, 1000d / unitsPerEm);
            }
            if (font instanceof PDType0Font) {
                PDType0Font t0font = (PDType0Font) font;
                if (t0font.getDescendantFont() instanceof PDCIDFontType2) {
                    int unitsPerEm = ((PDCIDFontType2) t0font.getDescendantFont()).getTrueTypeFont()
                        .getHeader().getUnitsPerEm();
                    at.scale(1000d / unitsPerEm, 1000d / unitsPerEm);
                }
            }
        }
        else if (font instanceof PDSimpleFont) {
            PDSimpleFont simpleFont = (PDSimpleFont) font;

            // these two lines do not always work, e.g. for the TT fonts in file 032431.pdf
            // which is why PDVectorFont is tried first.
            String name = simpleFont.getEncoding().getName(code);
            path = simpleFont.getPath(name);
        }
        else {
            // shouldn't happen, please open issue in JIRA
            System.out.println("Unknown font class: " + font.getClass());
        }
        if (path == null) {
            return null;
        }
        Shape s = at.createTransformedShape(path.getBounds2D());
        s = flipAT.createTransformedShape(s);
        s = rotateAT.createTransformedShape(s);
        s = transAT.createTransformedShape(s);
        return s;
    }

    private boolean isWriteGlyphCoords()
    {
        return writeGlyphCoords;
    }

    private void setWriteGlyphCoords(boolean writeGlyphCoords)
    {
        this.writeGlyphCoords = writeGlyphCoords;
    }
}
