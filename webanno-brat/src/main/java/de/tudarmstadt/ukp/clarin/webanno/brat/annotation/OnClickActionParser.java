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

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.uima.cas.text.AnnotationFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

/**
 * 
 *
 */
public class OnClickActionParser implements Serializable {
	
	private static final long serialVersionUID = 3008662322929838450L;
	
	private final static Logger LOG = LoggerFactory.getLogger(OnClickActionParser.class); 

	/**
	 * 
	 * @param jstemplate in the form of e.g. "alert('${PID} ${DID} ${DNAME} ${ID}')"
	 * @param project
	 * @param document
	 * @param anno
	 * @return String with substituted variables
	 */
	public static Map<String, String> parse(AnnotationLayer anno_layer, List<AnnotationFeature> anno_layer_features, Project project, SourceDocument document, AnnotationFS anno){
		Map<String, String> valuesMap = new HashMap<>();
		// add some defaults
		valuesMap.put("PID", String.valueOf(project.getId()));
		valuesMap.put("PNAME", project.getName());
		valuesMap.put("DOCID", String.valueOf(document.getId()));
		valuesMap.put("DOCNAME", document.getName());
		valuesMap.put("LAYERNAME", anno_layer.getUiName());

		// collect the methods of the annotation
		Method[] methods = anno.getClass().getDeclaredMethods(); 
		Arrays.stream(methods)
			.filter(m -> m.getParameterCount() < 1 && m.getReturnType() != Void.TYPE) // only select getter methods with no parameters and a return type
			.forEach(m -> {
				if(m.getName().startsWith("get")){
					String value = getStringValue(m, anno);
					if(value != null){
						String subst = m.getName().substring(3); // just cut-off 'get'
						valuesMap.put(subst, value);
					}
					return;
				}
				if(m.getName().startsWith("is")){
					String value = getStringValue(m, anno);
					if(value != null){
						String subst = m.getName(); // don't cut-off 'is'
						valuesMap.put(subst, value);
					}
					return;
				}
				// else nothing to add
			});
		
		// add fields from the annotation layer features and use the values from before
		anno_layer_features.stream().forEach(feat -> {
			String val = valuesMap.get(feat.getName());
			if(val != null)
				valuesMap.put(feat.getUiName(), val);
		});
		
		return valuesMap;
	}
	
	
	/**
	 * as JSON object
	 * @param valueMap
	 * @return map as JSON object string
	 */
	public static String asJSONObject(final Map<String, String> valueMap){
		if(valueMap == null)
			return "{ }";
		try {
			return new ObjectMapper().writeValueAsString(valueMap);
		} catch (JsonProcessingException e) {
			LOG.warn("Could not encode map to json object: '{}'.", StringUtils.abbreviate(valueMap.toString(), 100), e);
			return String.format("{ \"%s\": \"%s\" }", e.getClass().getSimpleName(), e.getMessage());
		}
	}
	

	/**
	 * Escapes values in the map
	 * 
	 * @param unescapedValues
	 */
	public static void escapeJavascript(final Map<String, String> unescapedValues){
		unescapedValues
			.entrySet()
			.forEach(e -> e.setValue(escapeJavascript(e.getValue())));
	}
	
	/**
	 * @param unescaped string
	 * @return javscript escaped string
	 */
	public static String escapeJavascript(String unescaped){
		return StringEscapeUtils.escapeJava(unescaped);
	}
	
	/**
	 * Evaluate method on obj and return its return value as string or null 
	 * @param method
	 * @param obj
	 * @return return value as string or null
	 */
	private static String getStringValue(Method method, Object obj){
		try {
			Object result = method.invoke(obj);
			if(result != null)
				return String.valueOf(result);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			LOG.debug(String.format("Some %s error happened, but probably not an important one: '%s'.", e.getClass(), e.getMessage()), e);
			return null;
		}
		return null;
	}

}
