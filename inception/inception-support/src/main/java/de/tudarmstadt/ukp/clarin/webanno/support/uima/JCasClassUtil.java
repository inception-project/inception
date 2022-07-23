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
package de.tudarmstadt.ukp.clarin.webanno.support.uima;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.cas.TOP;

public class JCasClassUtil
{
    public static final String UIMA_BUILTIN_JCAS_PREFIX = "org.apache.uima.jcas.";

    public static String getUimaTypeName(Class<? extends TOP> aClazz)
    {
        String typeName = aClazz.getName();
        if (typeName.startsWith(UIMA_BUILTIN_JCAS_PREFIX)) {
            typeName = "uima." + typeName.substring(UIMA_BUILTIN_JCAS_PREFIX.length());
        }
        else if (FeatureStructure.class.getName().equals(typeName)) {
            typeName = CAS.TYPE_NAME_TOP;
        }
        else if (AnnotationFS.class.getName().equals(typeName)) {
            typeName = CAS.TYPE_NAME_ANNOTATION;
        }
        return typeName;
    }
}
