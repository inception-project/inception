/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab and AB Language Technology
 * Technische Universität Darmstadt, Universität Hamburg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
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

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.FSUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public final class OnClickActionParser
{
    private final static Logger LOG = LoggerFactory.getLogger(OnClickActionParser.class);

    private OnClickActionParser()
    {
        // No instances
    }
    
    /**
     * @return String with substituted variables
     */
    public static Map<String, String> parse(AnnotationLayer aLayer,
            List<AnnotationFeature> aFeatures, Project aProject, SourceDocument aDocument,
            AnnotationFS aAnnotation)
    {
        Map<String, String> valuesMap = new HashMap<>();

        // add some defaults
        valuesMap.put("PID", String.valueOf(aProject.getId()));
        valuesMap.put("PNAME", aProject.getName());
        valuesMap.put("DOCID", String.valueOf(aDocument.getId()));
        valuesMap.put("DOCNAME", aDocument.getName());
        valuesMap.put("LAYERNAME", aLayer.getUiName());

        // add fields from the annotation layer features and use the values from before
        aFeatures.stream().forEach(feat -> {
            Object val = FSUtil.getFeature(aAnnotation, feat.getName(), Object.class);
            if (val != null) {
                valuesMap.put(feat.getUiName(), String.valueOf(val));
            }
        });

        return valuesMap;
    }

    /**
     * as JSON object
     * 
     * @return map as JSON object string
     */
    public static String asJSONObject(final Map<String, String> aValueMap)
    {
        if (aValueMap == null) {
            return "{ }";
        }
        try {
            return new ObjectMapper().writeValueAsString(aValueMap);
        }
        catch (JsonProcessingException e) {
            LOG.warn("Could not encode map to json object: '{}'.",
                    StringUtils.abbreviate(aValueMap.toString(), 100), e);
            return String.format("{ \"%s\": \"%s\" }", e.getClass().getSimpleName(),
                    e.getMessage());
        }
    }

    /**
     * Escapes values in the map
     */
    public static void escapeJavascript(final Map<String, String> aUnescapedValues)
    {
        aUnescapedValues.entrySet()
                .forEach(e -> e.setValue(StringEscapeUtils.escapeJava(e.getValue())));
    }
}
