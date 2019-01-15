/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains PDFAnno specific information.
 * Includes information about annotations (spans and relations),
 * the PDFAnno version and the PDFExtract version.
 * It also includes a color map for PDFAnno.
 */
public class PdfAnnoModel
{

    private String pdfannoVersion;

    private String pdfextractVersion;

    private List<Span> spans;

    private List<Integer> unmatchedSpans;

    private List<Relation> relations;

    private ColorMap colorMap;

    public PdfAnnoModel(String aPdfannoVersion, String aPdfextractVersion)
    {
        pdfannoVersion = aPdfannoVersion;
        pdfextractVersion = aPdfextractVersion;
        spans = new ArrayList<>();
        unmatchedSpans = new ArrayList<>();
        relations = new ArrayList<>();
        colorMap = new ColorMap("#808080");
    }

    public String getPdfannoVersion()
    {
        return pdfannoVersion;
    }

    public String getPdfextractVersion()
    {
        return pdfextractVersion;
    }

    public List<Span> getSpans()
    {
        return spans;
    }

    public boolean addSpan(Span aSpan)
    {
        if (aSpan ==  null) {
            return false;
        } else {
            spans.add(aSpan);
            // Span label contains color value as the real label won't be used in PDFAnno
            // the real label is seen in the right panel this is a workaround if two equal
            // labels have different colors also it reduces mapsize.
            colorMap.addSpan(aSpan.getLabel(), aSpan.getLabel());
            return true;
        }
    }

    public List<Relation> getRelations()
    {
        return relations;
    }

    public void addUnmatchedSpan(int aVSpanVId)
    {
        unmatchedSpans.add(aVSpanVId);
    }

    public List<Integer> getUnmatchedSpans()
    {
        return unmatchedSpans;
    }

    public boolean addRelation(Relation aRelation)
    {
        if (aRelation == null) {
            return false;
        } else {
            relations.add(aRelation);
            // Relation label contains color value as the real label won't be used in PDFAnno
            // the real label is seen in the right panel this is a workaround if two equal
            // labels have different colors, also it reduces mapsize.
            colorMap.addRelation(aRelation.getLabel(), aRelation.getLabel());
            return true;
        }
    }

    public ColorMap getColorMap()
    {
        return colorMap;
    }

    public String getAnnoFileContent()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("pdfanno = \"").append(pdfannoVersion).append("\"\n");
        sb.append("pdfextract = \"").append(pdfextractVersion).append("\"\n");
        sb.append("\n");
        spans.forEach(span -> sb.append(span.toAnnoFileString()).append("\n"));
        relations.forEach(relation -> sb.append(relation.toAnnoFileString()).append("\n"));
        return sb.toString();
    }
}
