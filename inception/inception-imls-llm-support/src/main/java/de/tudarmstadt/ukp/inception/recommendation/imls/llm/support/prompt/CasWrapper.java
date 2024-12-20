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
package de.tudarmstadt.ukp.inception.recommendation.imls.llm.support.prompt;

import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.jcas.tcas.Annotation;

public class CasWrapper
{
    private final CAS cas;

    public CasWrapper(CAS aCas)
    {
        cas = aCas;
    }

    public List<AnnotationWrapper> select(String aTypeName)
    {
        var type = getType(aTypeName);

        return cas.<Annotation> select(type).map(AnnotationWrapper::new).toList();
    }

    private Type getType(String aName)
    {
        var type = cas.getTypeSystem().getType(aName);
        if (type != null) {
            return type;
        }

        for (var t : cas.getTypeSystem()) {
            if (t.getShortName().equals(aName)) {
                return t;
            }
        }

        return null;
    }
}
