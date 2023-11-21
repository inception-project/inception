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
package de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.Tsv3XSerializer;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public abstract class TsvUnit
{
    private TsvDocument doc;
    private TsvSentence sentence;
    private int position;
    private final Token uimaToken;
    private Map<Type, List<AnnotationFS>> uimaAnnotations = new LinkedHashMap<>();

    public TsvUnit(TsvDocument aDoc, TsvSentence aSentence, Token aUimaToken, int aPosition)
    {
        doc = aDoc;
        sentence = aSentence;
        uimaToken = aUimaToken;
        position = aPosition;
    }

    public int getBegin()
    {
        return uimaToken.getBegin();
    }

    public int getEnd()
    {
        return uimaToken.getEnd();
    }

    /**
     * @param aCol
     *            the column
     * @return all annotations for the current column. Mind that there can be multiple columns per
     *         annotation. Also mind that in order to the get value for the column, it needs to be
     *         still retrieved from the corresponding feature of the returned annotations.
     */
    public List<AnnotationFS> getAnnotationsForColumn(TsvColumn aCol)
    {
        return uimaAnnotations.getOrDefault(aCol.uimaType, Collections.emptyList());
    }

    public TsvSentence getSentence()
    {
        return sentence;
    }

    public TsvDocument getDocument()
    {
        return doc;
    }

    /**
     * Add an UIMA annotation which overlaps with the current token in any way. It could also
     * completely contain or exactly match the token boundaries.
     * 
     * @param aFS
     *            an UIMA annotation.
     */
    public void addUimaAnnotation(AnnotationFS aFS)
    {
        addUimaAnnotation(aFS, false);
    }

    /**
     * Add an UIMA annotation which overlaps with the current token in any way. It could also
     * completely contain or exactly match the token boundaries.
     * 
     * @param aFS
     *            an UIMA annotation.
     * @param aAddDisambiguationIfStacked
     *            whether to add a disambiguation ID when annotations are stacked
     */
    public void addUimaAnnotation(AnnotationFS aFS, boolean aAddDisambiguationIfStacked)
    {
        Type effectiveType = getDocument().getSchema().getEffectiveType(aFS);
        uimaAnnotations.putIfAbsent(effectiveType, new ArrayList<>());

        List<AnnotationFS> annotations = uimaAnnotations.get(effectiveType);

        // If we already have annotations of this type, then we need to add disambiguation IDs.
        boolean alreadyHaveAnnotationsOfSameType = !annotations.isEmpty();

        // Add to the list only if necessary, i.e. only on the first column in which we encounter
        // this annotation. If it has multiple features, there may also be subsequent columns
        // for the same annotation and we do not want to add it again and again.
        // The position of the annotation in the per-type list corresponds to its stacking ID.
        boolean hasBeenAdded = false;
        if (!annotations.contains(aFS)) {
            annotations.add(aFS);
            hasBeenAdded = true;
        }

        // Add disambiguation IDs if annotations are stacked
        if (aAddDisambiguationIfStacked && hasBeenAdded && alreadyHaveAnnotationsOfSameType) {
            annotations.forEach(doc::addDisambiguationId);
        }
    }

    /**
     * @param aUimaType
     *            an UIMA annotation type.
     * @param aStackingIndex
     *            the stacking index if there are multiple annotations of the same type on the
     *            current unit.
     * @return the annotation
     */
    public AnnotationFS getUimaAnnotation(Type aUimaType, int aStackingIndex)
    {
        List<AnnotationFS> annotations = uimaAnnotations.get(aUimaType);
        if (annotations != null && annotations.size() > aStackingIndex) {
            return annotations.get(aStackingIndex);
        }
        else {
            return null;
        }
    }

    public List<AnnotationFS> getUimaAnnotations(Type aUimaType)
    {
        return uimaAnnotations.get(aUimaType);
    }

    public List<Type> getUimaTypes()
    {
        return uimaAnnotations.keySet().stream().collect(Collectors.toList());
    }

    public Token getUimaToken()
    {
        return uimaToken;
    }

    public int getPosition()
    {
        return position;
    }

    public String getId()
    {
        return String.format("%d-%d", sentence.getPosition(), position);
    }

    @Override
    public String toString()
    {
        StringWriter buf = new StringWriter();
        try (PrintWriter out = new PrintWriter(buf)) {
            new Tsv3XSerializer().write(out, this,
                    doc.getSchema().getHeaderColumns(doc.getActiveColumns()));
        }
        return buf.toString();
    }
}
