/*
 * Copyright 2017
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

public class AnnoFile
{

    private String pdfannoVersion;

    private String pdfextractVersion;

    private List<Span> spans;

    private List<Relation> relations;

    private ColorMap colorMap;

    public AnnoFile(String pdfannoVersion, String pdfextractVersion)
    {
        this.pdfannoVersion = pdfannoVersion;
        this.pdfextractVersion = pdfextractVersion;
        this.spans = new ArrayList<>();
        this.relations = new ArrayList<>();
        this.colorMap = new ColorMap("#808080");
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

    public boolean addSpan(Span span)
    {
        if (span ==  null) {
            return false;
        } else {
            spans.add(span);
            // Span label contains color value as the real label won't be used in PDFAnno
            // the real label is seen in the right panel this is a workaround if two equal
            // labels have different colors also it reduces mapsize.
            colorMap.addSpan(span.getLabel(), span.getLabel());
            return true;
        }
    }

    public List<Relation> getRelations()
    {
        return relations;
    }

    public boolean addRelation(Relation relation)
    {
        if (relation == null) {
            return false;
        } else {
            relations.add(relation);
            // Relation label contains color value as the real label won't be used in PDFAnno
            // the real label is seen in the right panel this is a workaround if two equal
            // labels have different colors, also it reduces mapsize.
            colorMap.addRelation(relation.getLabel(), relation.getLabel());
            return true;
        }
    }

    public ColorMap getColorMap()
    {
        return colorMap;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("pdfanno = \"").append(pdfannoVersion).append("\"\n");
        sb.append("pdfextract = \"").append(pdfextractVersion).append("\"\n");
        sb.append("\n");
        spans.forEach(span -> sb.append(span.toString()).append("\n"));
        relations.forEach(relation -> sb.append(relation.toString()).append("\n"));
        return sb.toString();
    }
}
