package paperai.pdfextract;

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
