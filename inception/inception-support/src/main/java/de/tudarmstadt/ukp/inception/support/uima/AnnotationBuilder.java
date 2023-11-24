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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.tcas.Annotation;

public class AnnotationBuilder<T extends AnnotationFS>
    extends FeatureStructureBuilder<T>
{

    public AnnotationBuilder(CAS aCas, Type aType)
    {
        super(aCas, aType);
    }

    public AnnotationBuilder<T> at(AnnotationFS aAnnotation)
    {
        withFeature(CAS.FEATURE_BASE_NAME_BEGIN, aAnnotation.getBegin());
        withFeature(CAS.FEATURE_BASE_NAME_END, aAnnotation.getEnd());
        return this;
    }

    public AnnotationBuilder<T> at(int aBegin, int aEnd)
    {
        withFeature(CAS.FEATURE_BASE_NAME_BEGIN, aBegin);
        withFeature(CAS.FEATURE_BASE_NAME_END, aEnd);
        return this;
    }

    public AnnotationBuilder<T> on(String aPattern)
    {
        Matcher m = Pattern.compile(aPattern).matcher(getCas().getDocumentText());
        if (m.matches()) {
            at(m.start(), m.end());
        }
        return this;
    }

    public static <X extends Annotation> AnnotationBuilder<X> buildAnnotation(CAS aCas, Type aType)
    {
        return new AnnotationBuilder<X>(aCas, aType);
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

    public static AnnotationBuilder<AnnotationFS> buildAnnotation(CAS aCas, String aType)
    {
        return new AnnotationBuilder<AnnotationFS>(aCas, CasUtil.getAnnotationType(aCas, aType));
    }
}
