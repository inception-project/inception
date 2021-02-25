/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab and AB Language Technology
 * Technische Universität Darmstadt, Universität Hamburg
 *
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
package de.tudarmstadt.ukp.clarin.webanno.brat.annotation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.text.AnnotationFS;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public final class OnClickActionParser
{
    private OnClickActionParser()
    {
        // No instances
    }

    /**
     * @return String with substituted variables
     */
    public static Map<String, Object> parse(AnnotationLayer aLayer,
            List<AnnotationFeature> aFeatures, SourceDocument aDocument, AnnotationFS aAnnotation)
    {
        Map<String, Object> valuesMap = new HashMap<>();

        // add some defaults
        valuesMap.put("PID", aLayer.getProject().getId());
        valuesMap.put("PNAME", aLayer.getProject().getName());
        valuesMap.put("DOCID", aDocument.getId());
        valuesMap.put("DOCNAME", aDocument.getName());
        valuesMap.put("LAYERNAME", aLayer.getName());

        // add fields from the annotation layer features and use the values from before
        aFeatures.stream().forEach(feat -> {
            if (WebAnnoCasUtil.isPrimitiveFeature(aAnnotation, feat.getName())) {
                Object val = WebAnnoCasUtil.getFeature(aAnnotation, feat.getName());
                if (val != null) {
                    valuesMap.put(feat.getUiName(), String.valueOf(val));
                }
            }
        });

        return valuesMap;
    }
}
