package paperai.pdfextract;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.imageio.ImageIO;

import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.contentstream.operator.DrawObject;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.contentstream.operator.state.Concatenate;
import org.apache.pdfbox.contentstream.operator.state.Restore;
import org.apache.pdfbox.contentstream.operator.state.Save;
import org.apache.pdfbox.contentstream.operator.state.SetGraphicsStateParameters;
import org.apache.pdfbox.contentstream.operator.state.SetMatrix;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.util.Matrix;

public class ImageExtractor
    extends PDFStreamEngine
{

    public static void main(String[] args) throws IOException
    {
        File inFile = new File(args[0]);
        HashMap<String, String> map = new HashMap<>();
        for (int i = 1; i < args.length; i += 2)
            map.put(args[i], args[i + 1]);
        String o = map.containsKey("-o") ? map.get("-o") : "";
        String outDir = o.isEmpty() ? inFile.getParent() : (new File(o)).getAbsolutePath();
        int dpi = map.containsKey("-dpi") ? Integer.parseInt(map.get("-dpi")) : 300;

        if (inFile.isDirectory()) {
            for (File file : inFile.listFiles()) {
                if (file.isFile() && file.getName().endsWith(".pdf")) {
                    processFile(file, dpi, outDir);
                }
            }
        }
        else
            processFile(inFile, dpi, outDir);
    }

    static void processFile(File inFile, int dpi, String outDir) throws IOException
    {
        PDDocument doc = PDDocument.load(inFile);
        String baseName = inFile.getName().substring(0, inFile.getName().lastIndexOf("."));
        try {
            RegionExtractor regionExt = new RegionExtractor(doc, dpi);
            int count = 1;
            for (int pageIndex = 0; pageIndex < doc.getNumberOfPages(); pageIndex++) {
                for (ImageOperator op : ImageExtractor.extract(doc.getPage(pageIndex))) {
                    RenderedImage image = regionExt.extract(pageIndex, op.x, op.y, op.w, op.h);
                    String outFileName = baseName + "_" + String.valueOf(count) + ".png";
                    ImageIO.write(image, "png", new File(outDir, outFileName));
                    System.out.println(outFileName + " is saved.");
                    count++;
                }
            }
        }
        finally {
            doc.close();
        }
    }

    static List<ImageOperator> extract(PDPage page) throws IOException
    {
        ImageExtractor ext = new ImageExtractor();
        ext.processPage(page);
        return ext.buffer;
    }

    List<ImageOperator> buffer = new ArrayList<>();

    public ImageExtractor()
    {
        addOperator(new Concatenate());
        addOperator(new DrawObject());
        addOperator(new SetGraphicsStateParameters());
        addOperator(new Save());
        addOperator(new Restore());
        addOperator(new SetMatrix());
    }

    @Override protected void processOperator(Operator operator, List<COSBase> operands)
        throws IOException
    {
        String operation = operator.getName();
        if ("Do".equals(operation)) {
            COSName objectName = (COSName) operands.get(0);
            PDXObject xobject = getResources().getXObject(objectName);

            if (xobject instanceof PDImageXObject) {
                PDImageXObject image = (PDImageXObject) xobject;
                Matrix ctmNew = getGraphicsState().getCurrentTransformationMatrix();
                PDRectangle pageRect = this.getCurrentPage().getCropBox();
                float w = ctmNew.getScalingFactorX();
                float h = ctmNew.getScalingFactorY();
                float x = ctmNew.getTranslateX();
                float y = pageRect.getHeight() - ctmNew.getTranslateY() - h;
                buffer.add(new ImageOperator(x, y, w, h));
            }
            else if (xobject instanceof PDFormXObject) {
                PDFormXObject form = (PDFormXObject) xobject;
                showForm(form);
            }
        }
        else {
            super.processOperator(operator, operands);
        }
    }
}
