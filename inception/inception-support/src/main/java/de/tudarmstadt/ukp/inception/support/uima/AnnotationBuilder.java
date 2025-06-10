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
package de.tudarmstadt.ukp.inception.support.uima;

import static org.apache.uima.fit.util.CasUtil.getAnnotationType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

public class AnnotationBuilder<T extends AnnotationFS>
    extends FeatureStructureBuilder<T>
{
    private record Range(int begin, int end) {}

    private final Set<Range> ranges = new LinkedHashSet<>();

    public AnnotationBuilder(CAS aCas, Type aType)
    {
        super(aCas, aType);
    }

    public AnnotationBuilder<T> at(AnnotationFS aAnnotation)
    {
        ranges.add(new Range(aAnnotation.getBegin(), aAnnotation.getEnd()));
        return this;
    }

    public AnnotationBuilder<T> at(int aBegin, int aEnd)
    {
        ranges.add(new Range(aBegin, aEnd));
        return this;
    }

    public List<T> buildAllAndAddToIndexes()
    {
        var annotations = buildAllWithoutAddingToIndexes();
        annotations.forEach(getCas()::addFsToIndexes);
        return annotations;
    }

    public List<T> buildAllWithoutAddingToIndexes()
    {
        var annotations = new ArrayList<T>();

        for (var range : ranges) {
            var ann = super.buildWithoutAddingToIndexes();
            ann.setBegin(range.begin);
            ann.setEnd(range.end);
            annotations.add(ann);
        }

        return annotations;
    }

    @Override
    public T buildAndAddToIndexes()
    {
        if (ranges.size() > 1) {
            throw new IllegalStateException(
                    "Use buildAllAndAddToIndexes() when multiple ranges have been specified.");
        }

        ranges.forEach(range -> {
            withFeature(CAS.FEATURE_BASE_NAME_BEGIN, range.begin);
            withFeature(CAS.FEATURE_BASE_NAME_END, range.end);
        });

        return super.buildAndAddToIndexes();
    }

    @Override
    public T buildWithoutAddingToIndexes()
    {
        if (ranges.size() > 1) {
            throw new IllegalStateException(
                    "Use buildAllWithoutAddingToIndexes() when multiple ranges have been specified.");
        }

        ranges.forEach(range -> {
            withFeature(CAS.FEATURE_BASE_NAME_BEGIN, range.begin);
            withFeature(CAS.FEATURE_BASE_NAME_END, range.end);
        });

        return super.buildWithoutAddingToIndexes();
    }

    public AnnotationBuilder<T> on(String aText)
    {
        return onMatch(Pattern.quote(aText));
    }

    public AnnotationBuilder<T> onAll(String aText)
    {
        return onAllMatches(Pattern.quote(aText));
    }

    public AnnotationBuilder<T> onMatch(String aPattern)
    {
        Matcher m = Pattern.compile(aPattern).matcher(getCas().getDocumentText());
        if (m.find()) {
            at(m.start(), m.end());
        }
        return this;
    }

    public AnnotationBuilder<T> onAllMatches(String aPattern)
    {
        Matcher m = Pattern.compile(aPattern).matcher(getCas().getDocumentText());
        while (m.find()) {
            at(m.start(), m.end());
        }
        return this;
    }

    public static <X extends Annotation> AnnotationBuilder<X> buildAnnotation(CAS aCas, Type aType)
    {
        return new AnnotationBuilder<X>(aCas, aType);
    }

    public static <X extends Annotation> AnnotationBuilder<X> buildAnnotation(JCas aCas,
            Class<X> aType)
    {
        return new AnnotationBuilder<X>(aCas.getCas(), aCas.getCasType(aType));
    }

    public static <X extends Annotation> AnnotationBuilder<X> buildAnnotation(CAS aCas,
            Class<X> aType)
    {
        return new AnnotationBuilder<X>(aCas, aCas.getCasType(aType));
    }

    public static AnnotationBuilder<AnnotationFS> buildAnnotation(CAS aCas)
    {
        return buildAnnotation(aCas, CAS.TYPE_NAME_ANNOTATION);
    }

    public static AnnotationBuilder<AnnotationFS> buildAnnotation(JCas aJCas, String aType)
    {
        return buildAnnotation(aJCas.getCas(), aType);
    }

    public static AnnotationBuilder<AnnotationFS> buildAnnotation(CAS aCas, String aType)
    {
        return new AnnotationBuilder<AnnotationFS>(aCas, getAnnotationType(aCas, aType));
    }
}
