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
package de.tudarmstadt.ukp.inception.pdfeditor2.format;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.jcas.JCas;
import org.dkpro.core.api.io.JCasResourceCollectionReader_ImplBase;
import org.dkpro.core.api.pdf.type.PdfGlyph;
import org.dkpro.core.api.pdf.type.PdfLine;
import org.dkpro.core.api.pdf.type.PdfPage;

import de.tudarmstadt.ukp.inception.pdfeditor2.visual.VisualPDFTextStripper;
import de.tudarmstadt.ukp.inception.pdfeditor2.visual.model.VGlyph;
import de.tudarmstadt.ukp.inception.pdfeditor2.visual.model.VLine;
import de.tudarmstadt.ukp.inception.pdfeditor2.visual.model.VModel;
import de.tudarmstadt.ukp.inception.pdfeditor2.visual.model.VPage;

public class VisualPdfReader
    extends JCasResourceCollectionReader_ImplBase
{
    @Override
    public void getNext(JCas aJCas) throws IOException, CollectionException
    {
        Resource resource = nextFile();
        initCas(aJCas, resource, null);

        StringWriter textBuffer = new StringWriter();
        VModel vModel;

        try (InputStream is = resource.getInputStream()) {
            try (PDDocument doc = PDDocument.load(is)) {
                var stripper = new VisualPDFTextStripper();
                stripper.writeText(doc, textBuffer);
                vModel = stripper.getVisualModel();
            }
        }

        aJCas.setDocumentText(textBuffer.toString());

        for (VPage vPage : vModel.getPages()) {
            PdfPage pdfPage = new PdfPage(aJCas, vPage.getBegin(), vPage.getEnd());
            pdfPage.setPageNumber(vPage.getIndex());
            pdfPage.setWidth(vPage.getWidth());
            pdfPage.setHeight(vPage.getHeight());
            pdfPage.addToIndexes();

            for (VLine vLine : vPage.getLines()) {
                PdfLine pdfLine = new PdfLine(aJCas, vLine.getBegin(), vLine.getEnd());
                pdfLine.setD(vLine.getDir());
                pdfLine.setB(vLine.getBase());
                pdfLine.setE(vLine.getExtent());
                pdfLine.addToIndexes();

                for (VGlyph vGlyph : vLine.getGlyphs()) {
                    PdfGlyph pdfGlyph = new PdfGlyph(aJCas, vGlyph.getBegin(), vGlyph.getEnd());
                    pdfGlyph.setB(vGlyph.getBase());
                    pdfGlyph.setE(vGlyph.getExtent());
                    pdfGlyph.addToIndexes();
                }
            }
        }
    }
}
