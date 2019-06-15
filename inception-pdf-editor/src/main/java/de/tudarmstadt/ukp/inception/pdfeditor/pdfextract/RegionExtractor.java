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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

public class RegionExtractor
{

    static int POINTS_IN_INCH = 72;

    PDFRenderer renderer;
    int dpi;

    public RegionExtractor(PDDocument doc, int dpi)
    {
        this.renderer = new PDFRenderer(doc);
        this.dpi = dpi;
    }

    RenderedImage extract(int pageIndex, Float x, Float y, Float w, Float h) throws IOException
    {
        Rectangle2D rect = new Rectangle2D.Float(x, y, w, h);

        double scale = dpi / POINTS_IN_INCH;
        double bitmapWidth = rect.getWidth() * scale;
        double bitmapHeight = rect.getHeight() * scale;
        BufferedImage image = new BufferedImage((int) bitmapWidth, (int) bitmapHeight,
            BufferedImage.TYPE_INT_RGB);

        AffineTransform transform = AffineTransform.getScaleInstance(scale, scale);
        transform.concatenate(AffineTransform.getTranslateInstance(-rect.getX(), -rect.getY()));

        Graphics2D graphics = image.createGraphics();
        graphics.setBackground(Color.WHITE);
        graphics.setTransform(transform);

        renderer.renderPageToGraphics(pageIndex, graphics);
        graphics.dispose();
        return image;
    }
}
