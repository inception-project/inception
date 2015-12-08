/*******************************************************************************
 * Copyright 2015
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.curation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.CasDiff2.ConfigurationSet;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.CasDiff2.DiffResult;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.CasDiff2.Position;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.component.CurationPanel;

/**
 * Do a merge CAS out of multiple user annotations
 *
 */
public class MergeCas {
	/**
	 * Using {@code DiffResult}, determine the annotations to be deleted from
	 * the randomly generated MergeCase. The initial Merge CAs is stored under a
	 * name {@code CurationPanel#CURATION_USER}.
	 * <p>
	 * Any similar annotations stacked in a {@code CasDiff2.Position} will be
	 * assumed a difference
	 * <p>
	 * Any two annotation with different value will be assumed a difference
	 * <p>
	 * Any non stacked empty/null annotations are assumed agreement
	 * <p>
	 * Any non stacked annotations with similar values for each of the features
	 * are assumed agreement
	 * <p>
	 * Any two link mode / slotable annotations which agree on the base features
	 * are assumed agreement
	 * 
	 * @param aDiff
	 *            the {@code CasDiff2.DiffResult}
	 * @param aJCases
	 *            a map of{@code JCas}s for each users and the random merge
	 * @return the actual merge {@code JCas}
	 */
	public static JCas geMergeCas(DiffResult aDiff, Map<String, JCas> aJCases) {

		Set<FeatureStructure> slotFeaturesToReset = new HashSet<>();
		Set<FeatureStructure> annotationsToDelete = new HashSet<>();

		Set<String> users = new HashSet<>();
		for (Position position : aDiff.getPositions()) {

			Map<String, List<FeatureStructure>> annosPerUser = new HashMap<>();

			ConfigurationSet cfgs = aDiff.getConfigurtionSet(position);
			users = cfgs.getCasGroupIds();

			if (cfgs.getConfigurations(CurationPanel.CURATION_USER).size() == 0) { // incomplete
																					// annotations
				continue;
			}
			FeatureStructure mergeAnno = cfgs.getConfigurations(CurationPanel.CURATION_USER).get(0)
					.getFs(CurationPanel.CURATION_USER, aJCases);

			// Get Annotations per user in this position
			getAllAnnosOnPosition(aJCases, annosPerUser, users, mergeAnno);

			for (FeatureStructure mergeFs : annosPerUser.get(CurationPanel.CURATION_USER)) {
				// incomplete annotations
				if (aJCases.size() != annosPerUser.size()) {
					annotationsToDelete.add(mergeFs);
				}
				// agreed and not stacked
				else if (isAgree(mergeFs, annosPerUser)) {

					Type t = mergeFs.getType();
					Feature sourceFeat = t.getFeatureByBaseName(WebAnnoConst.FEAT_REL_SOURCE);
					Feature targetFeat = t.getFeatureByBaseName(WebAnnoConst.FEAT_REL_TARGET);

					// Is this a relation?
					if (sourceFeat != null && targetFeat != null) {

						FeatureStructure source = mergeFs.getFeatureValue(sourceFeat);
						FeatureStructure target = mergeFs.getFeatureValue(targetFeat);

						// all span anno on this source positions
						Map<String, List<FeatureStructure>> sourceAnnosPerUser = new HashMap<>();
						// all span anno on this target positions
						Map<String, List<FeatureStructure>> targetAnnosPerUser = new HashMap<>();

						getAllAnnosOnPosition(aJCases, sourceAnnosPerUser, users, source);
						getAllAnnosOnPosition(aJCases, targetAnnosPerUser, users, target);

						if (isAgree(source, sourceAnnosPerUser) && isAgree(target, targetAnnosPerUser)) {
							slotFeaturesToReset.add(mergeFs);
						} else {
							annotationsToDelete.add(mergeFs);
						}
					} else {
						slotFeaturesToReset.add(mergeFs);
					}
				}
				// disagree or stacked annotations
				else {
					annotationsToDelete.add(mergeFs);
				}

				// remove dangling rels
				// setDanglingRelToDel(aJCases.get(CurationPanel.CURATION_USER),
				// mergeFs, annotationsToDelete);
			}

		}

		// remove annotations that do not agree or are a stacked ones
		for (FeatureStructure fs : annotationsToDelete) {
			if (!slotFeaturesToReset.contains(fs)) {
				aJCases.get(CurationPanel.CURATION_USER).removeFsFromIndexes(fs);
			}
		}
		// if slot bearing annotation, clean
		for (FeatureStructure baseFs : slotFeaturesToReset) {
			for (Feature roleFeature : baseFs.getType().getFeatures()) {
				if (isLinkMode(baseFs, roleFeature)) {
					// FeatureStructure roleFs = baseFs.getFeatureValue(f);
					ArrayFS roleFss = (ArrayFS) BratAjaxCasUtil.getFeatureFS(baseFs, roleFeature.getShortName());
					if (roleFss == null) {
						continue;
					}
					Map<String, ArrayFS> roleAnnosPerUser = new HashMap<>();

					setAllRoleAnnosOnPosition(aJCases, roleAnnosPerUser, users, baseFs, roleFeature);
					List<FeatureStructure> linkFSes = new LinkedList<FeatureStructure>(
							Arrays.asList(roleFss.toArray()));
					for (FeatureStructure roleFs : roleFss.toArray()) {

						if (isRoleAgree(roleFs, roleAnnosPerUser)) {
							for (Feature targetFeature : roleFs.getType().getFeatures()) {
								if (isBasicFeature(targetFeature)) {
									continue;
								}
								if (!targetFeature.getShortName().equals("target")) {
									continue;
								}
								FeatureStructure targetFs = roleFs.getFeatureValue(targetFeature);
								if (targetFs == null) {
									continue;
								}
								Map<String, List<FeatureStructure>> targetAnnosPerUser = new HashMap<>();
								getAllAnnosOnPosition(aJCases, targetAnnosPerUser, users, targetFs);

								// do not agree on targets
								if (!isAgree(targetFs, targetAnnosPerUser)) {
									linkFSes.remove(roleFs);
								}
							}
						}
						// do not agree on some role features
						else {
							linkFSes.remove(roleFs);
						}
					}

					ArrayFS array = baseFs.getCAS().createArrayFS(linkFSes.size());
					array.copyFromArray(linkFSes.toArray(new FeatureStructure[linkFSes.size()]), 0, 0, linkFSes.size());
					baseFs.setFeatureValue(roleFeature, array);
				}
			}
		}

		return aJCases.get(CurationPanel.CURATION_USER);
	}

	/**
	 * 
	 *
	 * Do not check on agreement on Position and SOfa feature - already checked
	 */
	private static boolean isBasicFeature(Feature aFeature) {
		return aFeature.getName().equals(CAS.FEATURE_FULL_NAME_SOFA)
				|| aFeature.toString().equals("uima.cas.AnnotationBase:sofa");
	}

	private static void getAllAnnosOnPosition(Map<String, JCas> aJCases,
			Map<String, List<FeatureStructure>> aAnnosPerUser, Set<String> aUsers, FeatureStructure aMergeAnno) {
		for (String usr : aUsers) {
			if (!aAnnosPerUser.containsKey(usr)) {
				List<FeatureStructure> fssAtThisPosition = getFSAtPosition(aJCases, aMergeAnno, usr);
				aAnnosPerUser.put(usr, fssAtThisPosition);
			} else {
				List<FeatureStructure> fssAtThisPosition = getFSAtPosition(aJCases, aMergeAnno, usr);
				aAnnosPerUser.get(usr).addAll(fssAtThisPosition);
			}
		}
	}

	private static void setAllRoleAnnosOnPosition(Map<String, JCas> aJCases, Map<String, ArrayFS> slotAnnosPerUser,
			Set<String> aUsers, FeatureStructure aBaseAnno, Feature aFeature) {
		Type t = aBaseAnno.getType();
		int begin = ((AnnotationFS) aBaseAnno).getBegin();
		int end = ((AnnotationFS) aBaseAnno).getEnd();

		for (String usr : aUsers) {
			for (FeatureStructure baseFS : CasUtil.selectCovered(aJCases.get(usr).getCas(), t, begin, end)) {
				// if non eqal stacked annotations with slot feature exists, get
				// the right one
				if (isSameAnno(aBaseAnno, baseFS)) {
					ArrayFS roleFs = (ArrayFS) BratAjaxCasUtil.getFeatureFS(baseFS, aFeature.getShortName());
					slotAnnosPerUser.put(usr, roleFs);
					break;
				}
			}
		}
	}

	/**
	 * Returns list of Annotations on this particular position (basically when
	 * stacking is allowed)
	 * 
	 * @return
	 */
	private static List<FeatureStructure> getFSAtPosition(Map<String, JCas> aJCases, FeatureStructure fs,
			String aUser) {
		Type t = fs.getType();
		int begin = ((AnnotationFS) fs).getBegin();
		int end = ((AnnotationFS) fs).getEnd();

		List<FeatureStructure> fssAtThisPosition = new ArrayList<>();
		for (FeatureStructure fss : CasUtil.selectCovered(aJCases.get(aUser).getCas(), t, begin, end)) {
			fssAtThisPosition.add(fss);
		}
		return fssAtThisPosition;
	}

	/**
	 * Returns true if a span annotation agrees on all features values
	 * (including null/empty as agreement) and no stacking is found in this
	 * position
	 */
	public static boolean isAgree(FeatureStructure aMergeFs, Map<String, List<FeatureStructure>> aAnnosPerUser) {
		for (String usr : aAnnosPerUser.keySet()) {
			boolean agree = false;
			for (FeatureStructure usrFs : aAnnosPerUser.get(usr)) {
				// same on all non slot feature values
				if (isSameAnno(aMergeFs, usrFs)) {
					if (!agree) { // this anno is the same with the others
						agree = true;
					} else if (agree) { // this is a stacked annotation
						return false;
					}
				}
			}
			// do not match in at least one user annotation in this position
			if (!agree) {
				return false;
			}
		}
		return true;
	}

	public static boolean isRoleAgree(FeatureStructure aMergeFs, Map<String, ArrayFS> aAnnosPerUser) {
		for (String usr : aAnnosPerUser.keySet()) {
			boolean agree = false;
			if (aAnnosPerUser.get(usr) == null) {
				return false;
			}
			for (FeatureStructure usrFs : aAnnosPerUser.get(usr).toArray()) {
				// same on all non slot feature values
				if (isSameAnno(aMergeFs, usrFs)) {
					if (!agree) { // this anno is the same with the others
						agree = true;
					}
				}
			}
			// do not match in at least one user annotation in this position
			if (!agree) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Return true if these two annotations agree on every non slot features
	 */
	public static boolean isSameAnno(FeatureStructure aFirstFS, FeatureStructure aSeconFS) {

		for (Feature f : getAllFeatures(aFirstFS)) {

			// the annotations are already in the same position
			if (isBasicFeature(f)) {

				continue;
			}

			if (!isLinkMode(aFirstFS, f)) {

				// check if attache type exists
				try {
					FeatureStructure attachFs1 = aFirstFS.getFeatureValue(f);
					FeatureStructure attachFs2 = aSeconFS.getFeatureValue(f);
					if (!isSameAnno(attachFs1, attachFs2)) {
						return false;
					}
				} catch (Exception e) {
					// no attach tyep -- continue
				}
				// assume null as equal
				if (getFeatureValue(aFirstFS, f) == null && getFeatureValue(aSeconFS, f) == null) {
					continue;
				}
				if (getFeatureValue(aFirstFS, f) == null && getFeatureValue(aSeconFS, f) != null) {
					return false;
				}
				if (getFeatureValue(aFirstFS, f) != null && getFeatureValue(aSeconFS, f) == null) {
					return false;
				}
				if (!getFeatureValue(aFirstFS, f).equals(getFeatureValue(aSeconFS, f))) {
					return false;
				}
			}
		}
		return true;
	}

	private static Feature[] getAllFeatures(FeatureStructure aFS) {
		Feature[] cachedSortedFeatures = new Feature[aFS.getType().getNumberOfFeatures()];
		int i = 0;
		for (Feature f : aFS.getType().getFeatures()) {
			cachedSortedFeatures[i] = f;
			i++;
		}
		return cachedSortedFeatures;
	}

	/**
	 * Returns true if this is slot feature
	 */
	private static boolean isLinkMode(FeatureStructure aFs, Feature aFeature) {
		try {
			ArrayFS slotFs = (ArrayFS) BratAjaxCasUtil.getFeatureFS(aFs, aFeature.getShortName());
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Get the feature value of this {@code Feature} on this annotation
	 */
	public static Object getFeatureValue(FeatureStructure aFS, Feature aFeature) {
		switch (aFeature.getRange().getName()) {
		case CAS.TYPE_NAME_STRING:
			return aFS.getFeatureValueAsString(aFeature);
		case CAS.TYPE_NAME_BOOLEAN:
			return aFS.getBooleanValue(aFeature);
		case CAS.TYPE_NAME_FLOAT:
			return aFS.getFloatValue(aFeature);
		case CAS.TYPE_NAME_INTEGER:
			return aFS.getIntValue(aFeature);
		case CAS.TYPE_NAME_BYTE:
			return aFS.getByteValue(aFeature);
		case CAS.TYPE_NAME_DOUBLE:
			return aFS.getDoubleValue(aFeature);
		case CAS.TYPE_NAME_LONG:
			aFS.getLongValue(aFeature);
		case CAS.TYPE_NAME_SHORT:
			aFS.getShortValue(aFeature);
		default:
			return null;
		// return aFS.getFeatureValue(aFeature);
		}
	}

}