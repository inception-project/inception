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
import java.util.LinkedList;
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

//	public static Map<String, Set<FeatureStructure>> doDiff(List<Type> aEntryType,
//			Map<String, CAS> aCasMap) throws Exception {
//		Iterator<CAS> casIterator = aCasMap.values().iterator();
//		if (!casIterator.hasNext()) {
//			return new HashMap<String, Set<FeatureStructure>>();
//		}
//		CAS cas = casIterator.next();
//		// TODO get begin and end like that?
//		int begin = 0;
//		int end = cas.getJCas().getDocumentText().length();
//		return doDiff(aEntryType, aCasMap, begin, end);
//	}
	

//	public static Map<String, Set<FeatureStructure>> doDiff(List<Type> aEntryTypes,
//			Map<String, CAS> aCasMap, int aBegin, int aEnd) throws Exception {
//		Map<String, Set<FeatureStructure>> diffs = new HashMap<String, Set<FeatureStructure>>();
//		
//		for (Type entryType : aEntryTypes) {
//			Map<String, Set<FeatureStructure>> diff = doDiff(entryType, aCasMap, aBegin, aEnd);
//			for (String username : diff.keySet()) {
//				if(!diffs.containsKey(username)) {
//					diffs.put(username, new HashSet<FeatureStructure>());
//				}
//				diffs.get(username).addAll(diff.get(username));
//			}
//		}
//		return diffs;
//	}
	public static List<AnnotationOption> doDiff(List<Type> aEntryTypes,
			Map<String, JCas> aCasMap, int aBegin, int aEnd) throws Exception {
		Map<Integer, Map<Integer, Set<AnnotationFS>>> annotationFSsByBeginEnd = new HashMap<Integer, Map<Integer, Set<AnnotationFS>>>();
		List<AnnotationOption> annotationOptions = new LinkedList<AnnotationOption>();
		Map<FeatureStructure, String> usernameByFeatureStructure = new HashMap<FeatureStructure, String>();

		Set<String> usernames = new HashSet<String>();

		for (Type aEntryType : aEntryTypes) {
			for (String username : aCasMap.keySet()) {
				usernames.add(username);
				CAS cas = aCasMap.get(username).getCas();
				
				// statt cas.getAnnotationIndex(aEntryType) auch
				// cas.getIndexRepository().getAllIndexedFS(aType)
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
			
			Map<FeatureStructure, AnnotationSelection> annotationSelectionByFeatureStructure = new HashMap<FeatureStructure, AnnotationSelection>();
			for (Map<Integer, Set<AnnotationFS>> annotationFSsByEnd : annotationFSsByBeginEnd
					.values()) {
				for (Set<AnnotationFS> annotationFSs : annotationFSsByEnd.values()) {
					// TODO check if still necessary
					/*
					if(annotationFSs.size() < aCasMap.keySet().size()) {
						for (AnnotationFS annotationFS : annotationFSs) {
							String username = usernameByFeatureStructure.get(annotationFS);
							diffs.get(username).add(annotationFS);
						}
						// Annotation is missing, thus add 
					}
					*/
					//Map<String, AnnotationSelection> annotationSelectionByUsername = new HashMap<String, AnnotationSelection>();
					Map<String, AnnotationOption> annotationOptionPerType = new HashMap<String, AnnotationOption>();
					for (FeatureStructure fsNew : annotationFSs) {
						String usernameFSNew = usernameByFeatureStructure
								.get(fsNew);
						// diffFS1 contains all feature structures of fs1, which do not occur in other cases
						Set<FeatureStructure> diffFSNew = traverseFS(fsNew);
						Map<FeatureStructure, AnnotationOption> annotationOptionByCompareResultFS1 = new HashMap<FeatureStructure, AnnotationOption>();
						
						Map<FeatureStructure, AnnotationSelection> annotationSelectionByFeatureStructureNew = new HashMap<FeatureStructure, AnnotationSelection>(annotationSelectionByFeatureStructure);
						for (FeatureStructure fsOld : annotationSelectionByFeatureStructure.keySet()) {
							String usernameFSOld = usernameByFeatureStructure
									.get(fsOld);
							// TODO add annotationFS to existing annotationSelectionByAnnotationFS
							if (fsNew != fsOld) {
								CompareResult compareResult = compareFeatureFS(
										fsNew, fsOld, diffFSNew);
								for (FeatureStructure compareResultFSNew : compareResult.getAgreements().keySet()) {
									FeatureStructure compareResultFSOld = compareResult.getAgreements().get(compareResultFSNew);
									int addressNew = aCasMap.get(usernameFSNew).getLowLevelCas().ll_getFSRef(compareResultFSNew);
									AnnotationSelection annotationSelection = annotationSelectionByFeatureStructure.get(compareResultFSOld);
									annotationSelection.getAddressByUsername().put(usernameFSNew, addressNew);
									annotationSelectionByFeatureStructureNew.put(compareResultFSNew, annotationSelection);
									// Only one fs needed?
									//annotationSelectionByFeatureStructure.put(compareResultFSNew, annotationSelection);
									
								}
								/* TODO wird das benötigt?
								for (FeatureStructure compareResultFS1 : compareResult.getDiffs().keySet()) {
									FeatureStructure compareResultFS2 = compareResult.getDiffs().get(compareResultFS1);
									AnnotationSelection annotationSelection = annotationSelectionByFeatureStructure.get(compareResultFS2);
									AnnotationOption annotationOption = annotationSelection.getAnnotationOption();
									// TODO Wird nicht verwendet. Warum?
									annotationOptionByCompareResultFS1.put(compareResultFS1, annotationOption);
								}
								 */
								/*
								for (FeatureStructure newDiff : compareResult.getDiffs()
										.get(fs1)) {
									// TODO new AnnatationSelection for FS1
									diffs.get(usernameFS1).add(newDiff);
								}
								for (FeatureStructure newAgreement : compareResult.getAgreements()
										.get(fs1)) {
									// TODO merge AnnotationSelection to FS2
								}
								*/
							}
						}
						annotationSelectionByFeatureStructure = annotationSelectionByFeatureStructureNew;
						
						// add featureStructures, that have not been found in existing annotationSelections
						for (FeatureStructure subFS1 : diffFSNew) {
							AnnotationSelection annotationSelection = new AnnotationSelection();
							int addressSubFS1 = aCasMap.get(usernameFSNew).getLowLevelCas().ll_getFSRef(subFS1);
							annotationSelection.getAddressByUsername().put(usernameFSNew, addressSubFS1);
							annotationSelectionByFeatureStructure.put(subFS1, annotationSelection);
							String type = subFS1.getType().toString();
							if(!annotationOptionPerType.containsKey(type)) {
								annotationOptionPerType.put(type, new AnnotationOption());
							}
							AnnotationOption annotationOption = annotationOptionPerType.get(type);
							// link annotationOption and annotationSelection
							annotationSelection.setAnnotationOption(annotationOption);
							annotationOption.getAnnotationSelections().add(annotationSelection);
						}
					}
					annotationOptions.addAll(annotationOptionPerType.values());
				}
			}
		}

		return annotationOptions;
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

	private static CompareResult compareFeatureFS(
			FeatureStructure fs1, FeatureStructure fs2, Set<FeatureStructure> diffFS1) throws Exception {
		//Map<FeatureStructure, Set<FeatureStructure>> diffs = new HashMap<FeatureStructure, Set<FeatureStructure>>();
		CompareResult compareResult = new CompareResult();
//		compareResult.getAgreements().put(fs1, new HashSet<FeatureStructure>());
//		compareResult.getAgreements().put(fs2, new HashSet<FeatureStructure>());
//		compareResult.getDiffs().put(fs1, new HashSet<FeatureStructure>());
//		compareResult.getDiffs().put(fs2, new HashSet<FeatureStructure>());
		
		// check if types are equal
		Type type = fs1.getType();
		if (!fs2.getType().toString().equals(type.toString())) {
			// if types differ add feature structure to diff
			compareResult.getDiffs().put(fs1, fs2);
			return compareResult;
		}

		boolean agreeOnSubfeatures = true;
		for (Feature feature : type.getFeatures()) {
			// features are present in both feature structures, fs1 and fs2
			// compare primitive values
			if (feature.getRange().isPrimitive()) {
				// System.out.println(feature.getDomain().getName());
				// System.out.println(feature.getRange().getName());

				// check int Values
				if (feature.getRange().getName().equals("uima.cas.Integer")) {
					if (!(fs1.getIntValue(feature) == fs2.getIntValue(feature))) {
						//disagree
						agreeOnSubfeatures = false;
						//compareResult.getDiffs().put(fs1, fs2);
					} else {
						// agree
						//compareResult.getAgreements().put(fs1, fs2);
						//diffFS1.remove(fs1);
					}
				} else if (feature.getRange().getName()
						.equals("uima.cas.String")) {
					String stringValue1 = fs1.getStringValue(feature);
					String stringValue2 = fs1.getStringValue(feature);
					if (stringValue1 == null && stringValue2 == null) {
						// TODO agree
						// Do nothing, null == null
					} else if (stringValue1 == null
							|| stringValue2 == null
							|| !fs1.getStringValue(feature).equals(
									fs2.getStringValue(feature))) {
						// stringValue1 differs from stringValue2
						
						// disagree
						agreeOnSubfeatures = false;
						//compareResult.getDiffs().put(fs1, fs2);
					} else {
						// agree
						//compareResult.getAgreements().put(fs1, fs2);
						//diffFS1.remove(fs1);
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
					CompareResult compareResultSubfeatures = compareFeatureFS(
							featureValue1, featureValue2, diffFS1);
					compareResult.getDiffs().putAll(compareResultSubfeatures.getDiffs());
					compareResult.getAgreements().putAll(compareResultSubfeatures.getAgreements());
					if(!compareResult.getDiffs().isEmpty()) {
						agreeOnSubfeatures = false;
					}
				}
				/* not necessary anymore
				else if (featureValue1 == null && featureValue2 != null) {
					// TODO if feature present in only one branch add to separate field in CompareResult
					Set<FeatureStructure> allFV2 = traverseFS(featureValue2);
					compareResult.getFs2only().addAll(allFV2);
//					compareResult.getDiffs().get(fs2).add(featureValue2);
				} else if (featureValue2 == null && featureValue1 != null) {
					// TODO if feature present in only one branch add to separate field in CompareResult
					Set<FeatureStructure> allFV1 = traverseFS(featureValue1);
					compareResult.getFs2only().addAll(allFV1);
//					compareResult.getDiffs().get(fs1).add(featureValue1);
				}
				*/
			}
		}
		if(agreeOnSubfeatures) {
			compareResult.getAgreements().put(fs1, fs2);
			diffFS1.remove(fs1);
		} else {
			compareResult.getDiffs().put(fs1, fs2);
		}
		
		// TODO if no diffs, agree (here or elsewhere)?
		if(compareResult.getDiffs().isEmpty()) {
			compareResult.getAgreements().put(fs1, fs2);
			diffFS1.remove(fs1);
		}

		return compareResult;
	}
	
}
