/*******************************************************************************
 * Copyright 2012
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.page.curation;

import static org.uimafit.util.CasUtil.selectCovered;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.CasCopier;
import org.uimafit.util.CasUtil;

public class CasDiff {

	/**
	 * spot differing annotations by comparing cases of the same document.
	 * Notice: if two annotations exist for one cas, that have the same entry
	 * type, begin and end, the result may contain more
	 * 
	 * @param aCasMap
	 *            Map of (username, cas)
	 * @return Map of (username, feature structure addresses)
	 * @throws Exception
	 */

	public static Map<String, Set<FeatureStructure>> doDiff(List<Type> aEntryType,
			Map<String, CAS> aCasMap) throws Exception {
		Iterator<CAS> casIterator = aCasMap.values().iterator();
		if (!casIterator.hasNext()) {
			return new HashMap<String, Set<FeatureStructure>>();
		}
		CAS cas = casIterator.next();
		// TODO get begin and end like that?
		int begin = 0;
		int end = cas.getJCas().getDocumentText().length();
		return doDiff(aEntryType, aCasMap, begin, end);
	}
	

	public static Map<String, Set<FeatureStructure>> doDiff(List<Type> aEntryTypes,
			Map<String, CAS> aCasMap, int aBegin, int aEnd) throws Exception {
		Map<String, Set<FeatureStructure>> diffs = new HashMap<String, Set<FeatureStructure>>();
		
		for (Type entryType : aEntryTypes) {
			Map<String, Set<FeatureStructure>> diff = doDiff(entryType, aCasMap, aBegin, aEnd);
			for (String username : diff.keySet()) {
				if(!diffs.containsKey(username)) {
					diffs.put(username, new HashSet<FeatureStructure>());
				}
				diffs.get(username).addAll(diff.get(username));
			}
		}
		return diffs;
	}
	public static Map<String, Set<FeatureStructure>> doDiff(Type aEntryType,
			Map<String, CAS> aCasMap, int aBegin, int aEnd) throws Exception {
		Map<Integer, Map<Integer, Set<AnnotationFS>>> annotationFSsByBeginEnd = new HashMap<Integer, Map<Integer, Set<AnnotationFS>>>();
		Map<FeatureStructure, String> usernameByFeatureStructure = new HashMap<FeatureStructure, String>();

		Map<String, Set<FeatureStructure>> diffs = new HashMap<String, Set<FeatureStructure>>();

		for (String username : aCasMap.keySet()) {
			diffs.put(username, new HashSet<FeatureStructure>());
			CAS cas = aCasMap.get(username);
			// statt cas.getAnnotationIndex(aEntryType) auch
			// cas.getIndexRepository().getAllIndexedFS(aType)

			;
			for (AnnotationFS annotationFS : selectCovered(cas, aEntryType,
					aBegin, aEnd)) {
				Integer begin = annotationFS.getBegin();
				Integer end = annotationFS.getEnd();

				if (!annotationFSsByBeginEnd.containsKey(begin)) {
					annotationFSsByBeginEnd.put(begin,
							new HashMap<Integer, Set<AnnotationFS>>());
				}
				if (!annotationFSsByBeginEnd.get(begin).containsKey(end)) {
					annotationFSsByBeginEnd.get(begin).put(end,
							new HashSet<AnnotationFS>());
				}
				annotationFSsByBeginEnd.get(begin).get(end).add(annotationFS);
				usernameByFeatureStructure.put(annotationFS, username);
			}
		}

		for (Map<Integer, Set<AnnotationFS>> annotationFSsByEnd : annotationFSsByBeginEnd
				.values()) {
			for (Set<AnnotationFS> annotationFSs : annotationFSsByEnd.values()) {
				if(annotationFSs.size() < aCasMap.keySet().size()) {
					for (AnnotationFS annotationFS : annotationFSs) {
						String username = usernameByFeatureStructure.get(annotationFS);
						diffs.get(username).add(annotationFS);
					}
					// Annotation is missing, thus add 
				}
				for (AnnotationFS annotationFS1 : annotationFSs) {
					for (AnnotationFS annotationFS2 : annotationFSs) {
						if (annotationFS1 != annotationFS2) {
							Map<FeatureStructure, Set<FeatureStructure>> newDiffs = compareFeatureFS(
									annotationFS1, annotationFS2);
							for (FeatureStructure newDiff : newDiffs
									.get(annotationFS1)) {
								String username = usernameByFeatureStructure
										.get(annotationFS1);
								diffs.get(username).add(newDiff);
							}
							for (FeatureStructure newDiff : newDiffs
									.get(annotationFS2)) {
								String username = usernameByFeatureStructure
										.get(annotationFS2);
								diffs.get(username).add(newDiff);
							}
						}
					}
				}
			}
		}

		return diffs;
	}

	private static Set<FeatureStructure> traverseFS(FeatureStructure fs) {
		Set<FeatureStructure> nodePlusChildren = new HashSet<FeatureStructure>();
		nodePlusChildren.add(fs);
		for (Feature feature : fs.getType().getFeatures()) {
			// features are present in both feature structures, fs1 and fs2
			// compare primitive values
			if (!feature.getRange().isPrimitive()) {
				// compare composite types
				// TODO assumtion: if feature is not primitive, it is a
				// composite feature
				FeatureStructure featureValue = fs.getFeatureValue(feature);
				if (featureValue != null) {
					nodePlusChildren.addAll(traverseFS(featureValue));
				}
			}
		}
		return nodePlusChildren;
	}

	private static Map<FeatureStructure, Set<FeatureStructure>> compareFeatureFS(
			FeatureStructure fs1, FeatureStructure fs2) throws Exception {
		Map<FeatureStructure, Set<FeatureStructure>> diffs = new HashMap<FeatureStructure, Set<FeatureStructure>>();
		diffs.put(fs1, new HashSet<FeatureStructure>());
		diffs.put(fs2, new HashSet<FeatureStructure>());
		// check if types are equal
		Type type = fs1.getType();
		if (!fs2.getType().toString().equals(type.toString())) {
			// if types differ add feature structures and all children to diff
			diffs.get(fs1).addAll(traverseFS(fs1));
			diffs.get(fs2).addAll(traverseFS(fs2));
			// TODO check inheritence if necessary (is fs1 a subtype of fs2 or
			// vice versa?)
			return diffs;
		}

		for (Feature feature : type.getFeatures()) {
			// features are present in both feature structures, fs1 and fs2
			// compare primitive values
			if (feature.getRange().isPrimitive()) {
				// System.out.println(feature.getDomain().getName());
				// System.out.println(feature.getRange().getName());

				// check int Values
				if (feature.getRange().getName().equals("uima.cas.Integer")) {
					if (!(fs1.getIntValue(feature) == fs2.getIntValue(feature))) {
						diffs.get(fs1).add(fs1);
						diffs.get(fs2).add(fs2);
					}
				} else if (feature.getRange().getName()
						.equals("uima.cas.String")) {
					String stringValue1 = fs1.getStringValue(feature);
					String stringValue2 = fs1.getStringValue(feature);
					if (stringValue1 == null && stringValue2 == null) {
						// Do nothing, null == null
					} else if (stringValue1 == null
							|| stringValue2 == null
							|| !fs1.getStringValue(feature).equals(
									fs2.getStringValue(feature))) {
						// stringValue1 differs from stringValue2
						diffs.get(fs1).add(fs1);
						diffs.get(fs2).add(fs2);
					}
				} else {
					throw new Exception(feature.getRange().getName()
							+ " not yet checkd!");
				}

				// TODO check other Values
			} else {
				// compare composite types
				// TODO assumtion: if feature is not primitive, it is a
				// composite feature
				FeatureStructure featureValue1 = fs1.getFeatureValue(feature);
				FeatureStructure featureValue2 = fs2.getFeatureValue(feature);
				if (featureValue1 != null && featureValue2 != null) {
					Map<FeatureStructure, Set<FeatureStructure>> compareResult = compareFeatureFS(
							featureValue1, featureValue2);
					diffs.get(fs1).addAll(compareResult.get(featureValue1));
					diffs.get(fs2).addAll(compareResult.get(featureValue2));
				} else if (featureValue1 == null && featureValue2 != null) {
					diffs.get(fs2).add(featureValue2);
				} else if (featureValue2 == null && featureValue1 != null) {
					diffs.get(fs1).add(featureValue1);
				}
			}
		}

		// if one of the children of fs1 and fs2 differ, add fs1 and fs2 to diff
		if (!diffs.get(fs1).isEmpty()) {
			diffs.get(fs1).add(fs1);
		}
		if (!diffs.get(fs2).isEmpty()) {
			diffs.get(fs2).add(fs2);
		}

		return diffs;
	}
}
