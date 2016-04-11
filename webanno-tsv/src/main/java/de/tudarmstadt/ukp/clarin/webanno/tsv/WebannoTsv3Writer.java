/*******************************************************************************
 * Copyright 2014
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
package de.tudarmstadt.ukp.clarin.webanno.tsv;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.selectCovered;
import static org.apache.uima.fit.util.CasUtil.selectFS;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.apache.uima.fit.util.JCasUtil.selectCovered;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

import org.apache.commons.io.IOUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureStructureImpl;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.tsv.util.AnnotationUnit;
import de.tudarmstadt.ukp.dkpro.core.api.io.JCasFileWriter_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

/**
 * Export annotations in TAB separated format. Header includes information about
 * the UIMA type and features The number of columns are depend on the number of
 * types/features exist. All the spans will be written first and subsequently
 * all the relations. relation is given in the form of Source--&gt;Target and
 * the RelationType is added to the Target token. The next column indicates the
 * source of the relation (the source of the arc drown)
 *
 *
 */

public class WebannoTsv3Writer extends JCasFileWriter_ImplBase {

	/**
	 * Name of configuration parameter that contains the character encoding used
	 * by the input files.
	 */
	public static final String PARAM_ENCODING = ComponentParameters.PARAM_SOURCE_ENCODING;
	@ConfigurationParameter(name = PARAM_ENCODING, mandatory = true, defaultValue = "UTF-8")
	private String encoding;

	public static final String PARAM_FILENAME_SUFFIX = "filenameSuffix";
	@ConfigurationParameter(name = PARAM_FILENAME_SUFFIX, mandatory = true, defaultValue = ".tsv")
	private String filenameSuffix;

	public static final String PARAM_SPAN_LAYERS = "spanLayers";
	@ConfigurationParameter(name = PARAM_SPAN_LAYERS, mandatory = true, defaultValue = {})
	private List<String> spanLayers;

	public static final String PARAM_SLOT_FEATS = "slotFeatures";
	@ConfigurationParameter(name = PARAM_SLOT_FEATS, mandatory = true, defaultValue = {})
	private List<String> slotFeatures;

	public static final String PARAM_LINK_TYPES = "linkTypes";
	@ConfigurationParameter(name = PARAM_LINK_TYPES, mandatory = true, defaultValue = {})
	private List<String> linkTypes;

	public static final String PARAM_SLOT_TARGETS = "slotTargets";
	@ConfigurationParameter(name = PARAM_SLOT_TARGETS, mandatory = true, defaultValue = {})
	private List<String> slotTargets;

	public static final String PARAM_CHAIN_LAYERS = "chainLayers";
	@ConfigurationParameter(name = PARAM_CHAIN_LAYERS, mandatory = true, defaultValue = {})
	private List<String> chainLayers;

	public static final String PARAM_RELATION_LAYERS = "relationLayers";
	@ConfigurationParameter(name = PARAM_RELATION_LAYERS, mandatory = true, defaultValue = {})
	private List<String> relationLayers;

	private static final String TAB = "\t";
	private static final String LF = "\n";
	private static final String DEPENDENT = "Dependent";
	private static final String GOVERNOR = "Governor";
	private static final String REF_REL = "referenceRelation";
	private static final String CHAIN = "Chain";
	private static final String LINK = "Link";
	private static final String FIRST = "first";
	private static final String NEXT = "next";
	public static final String SP = "T_SP"; // span annotation type
	public static final String CH = "T_CH"; // chain annotation type
	public static final String RL = "T_RL"; // relation annotation type
	public static final String ROLE = "ROLE_";
	public static final String BT = "BT_"; // base type for the relation
											// annotation
	private List<AnnotationUnit> units = new ArrayList<>();
	// number of subunits under this Annotation Unit
	private Map<AnnotationUnit, Integer> subUnits = new HashMap<>();
	private Map<String, Set<String>> featurePerLayer = new LinkedHashMap<>();
	private Map<AnnotationUnit, String> unitsLineNumber = new HashMap<>();
	private Map<AnnotationUnit, String> sentenceUnits = new HashMap<>();
	private Map<AnnotationUnit, String> sentenceBeginEnd = new HashMap<>();
	private Map<String, Map<AnnotationUnit, List<List<String>>>> annotationsPerPostion = new HashMap<>();
	private Map<Feature, Type> slotFeatureTypes = new HashMap<>();
	private Map<Integer, Integer> annotaionRef = new HashMap<>();
	private Map<String, Map<AnnotationUnit, Integer>> unitRef = new HashMap<>();
	private Map<String, String> slotLinkTypes = new HashMap<>();
	
	private Map<Type, Integer> layerMaps = new LinkedHashMap<>();

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		OutputStream docOS = null;
		try {
			docOS = getOutputStream(aJCas, filenameSuffix);
			setSlotLinkTypes();
			setLinkMaps(aJCas);
			setTokenSentenceAddress(aJCas);
			setSpanAnnotation(aJCas);
			setChainAnnotation(aJCas);
			setRelationAnnotation(aJCas);
			writeHeader(docOS);
			for (AnnotationUnit unit : units) {
				if (sentenceUnits.containsKey(unit)) {
					// TODO: This removes any in-line line breaks
					IOUtils.write(LF + "#Text=" + sentenceBeginEnd.get(unit) + "#"
							+ sentenceUnits.get(unit).replace(LF, "") + LF, docOS, encoding);
				}
				if (unit.isSubtoken) {
					IOUtils.write(
							unitsLineNumber.get(unit) + TAB + unit.begin + "-" + unit.end + TAB + unit.token + TAB,
							docOS, encoding);

				} else {
					IOUtils.write(
							unitsLineNumber.get(unit) + TAB + unit.begin + "-" + unit.end + TAB + unit.token + TAB,
							docOS, encoding);
				}
				for (String type : featurePerLayer.keySet()) {
					List<List<String>> annos = annotationsPerPostion.getOrDefault(type, new HashMap<>())
							.getOrDefault(unit, new ArrayList<>());
					List<String> merged = null;
					for (List<String> annofs : annos) {
						if (merged == null) {
							merged = annofs;
						} else {

							for (int i = 0; i < annofs.size(); i++) {
								merged.set(i, merged.get(i) + "||" + annofs.get(i));
							}
						}
					}
					if (merged != null) {
						for (String anno : merged) {
							IOUtils.write(anno + TAB, docOS, encoding);
						}
					} // No annotation of this type in this layer
					else {
						for (String feature : featurePerLayer.get(type)) {
							IOUtils.write("_" + TAB, docOS, encoding);
						}
					}
				}
				IOUtils.write(LF, docOS, encoding);
			}
		} catch (Exception e) {
			throw new AnalysisEngineProcessException(e);
		} finally {
			closeQuietly(docOS);
		}
	}

	private void setSlotLinkTypes() {
		int i = 0;
		for (String f : slotFeatures) {
			slotLinkTypes.put(f, linkTypes.get(i));
			i++;
		}
	}
	
	private void setLinkMaps(JCas aJCas) {
		for (String l : spanLayers) {
			if (l.equals(Token.class.getName())) {
				continue;
			}
			Type type = getType(aJCas.getCas(), l);
			layerMaps.put(type, layerMaps.size() + 1);
		}
		for (String l : chainLayers) {
			Type type = getType(aJCas.getCas(), l + LINK);
			layerMaps.put(type, layerMaps.size() + 1);
		}
		for (String l : relationLayers) {
			Type type = getType(aJCas.getCas(), l);
			layerMaps.put(type, layerMaps.size() + 1);
		}
	}

	/**
	 * Write headers, in the sequence <br>
	 * Type TAB List(Features sep by TAB)
	 * 
	 * @param docOS
	 * @throws IOException
	 */
	private void writeHeader(OutputStream docOS) throws IOException {
		IOUtils.write("#FORMAT=WebAnno TSV 3" + LF, docOS, encoding);
		for (String type : featurePerLayer.keySet()) {
			String annoType;
			if (spanLayers.contains(type)) {
				annoType = SP;
			} else if (relationLayers.contains(type)) {
				annoType = RL;
			} else {
				annoType = CH;
			}
			IOUtils.write("#" + annoType + "=" + type + "|", docOS, encoding);
			StringBuffer fsb = new StringBuffer();
			for (String feature : featurePerLayer.get(type)) {
				if (fsb.length() < 1) {
					fsb.append(feature);
				} else {
					fsb.append("|" + feature);
				}
			}
			IOUtils.write(fsb.toString() + LF, docOS, encoding);
		}
		IOUtils.write(LF, docOS, encoding);
	}

	private void setSpanAnnotation(JCas aJCas) {
		int i = 0;
		// store slot targets for each slot features
		for (String l : spanLayers) {
			Type type = getType(aJCas.getCas(), l);
			for (Feature f : type.getFeatures()) {
				if (slotFeatures != null && slotFeatures.contains(f.getName())) {
					slotFeatureTypes.put(f, getType(aJCas.getCas(), slotTargets.get(i)));
					i++;
				}
			}
		}

		for (String l : spanLayers) {
			if (l.equals(Token.class.getName())) {
				continue;
			}
			Map<AnnotationUnit, List<List<String>>> annotationsPertype;
			if (annotationsPerPostion.get(l) == null) {
				annotationsPertype = new HashMap<>();

			} else {
				annotationsPertype = annotationsPerPostion.get(l);
			}
			Type type = getType(aJCas.getCas(), l);
			for (AnnotationFS fs : CasUtil.select(aJCas.getCas(), type)) {
				AnnotationUnit unit = new AnnotationUnit(fs.getBegin(), fs.getEnd(), false, fs.getCoveredText());
				// annotation is per Token
				if (units.contains(unit)) {
					setSpanAnnoPerFeature(annotationsPertype, type, fs, unit, false, false);
				}
				// Annotation is on sub-token or multiple tokens
				else {
					SubTokenAnno sta = new SubTokenAnno();
					sta.setBegin(fs.getBegin());
					sta.setEnd(fs.getEnd());
					sta.setText(fs.getCoveredText());
					boolean isMultiToken = isMultiToken(fs);
					boolean isFirst = true;
					Set<AnnotationUnit> sus = new LinkedHashSet<>();
					for (AnnotationUnit newUnit : getSubUnits(sta, sus)) {
						setSpanAnnoPerFeature(annotationsPertype, type, fs, newUnit, isMultiToken, isFirst);
						isFirst = false;
					}
				}
			}
			if (annotationsPertype.keySet().size() > 0) {
				annotationsPerPostion.put(l, annotationsPertype);
			}
		}
	}

	private void setChainAnnotation(JCas aJCas) {
		for (String l : chainLayers) {
			if (l.equals(Token.class.getName())) {
				continue;
			}

			Map<AnnotationUnit, List<List<String>>> annotationsPertype = null;
			Type type = getType(aJCas.getCas(), l + CHAIN);
			Feature chainFirst = type.getFeatureByBaseName(FIRST);
			int chainNo = 1;
			for (FeatureStructure chainFs : selectFS(aJCas.getCas(), type)) {
				AnnotationFS linkFs = (AnnotationFS) chainFs.getFeatureValue(chainFirst);
				AnnotationUnit unit = getUnit(linkFs.getBegin(), linkFs.getEnd(), linkFs.getCoveredText());
				Type lType = linkFs.getType();

				// this is the layer with annotations
				l = lType.getName();
				if (annotationsPerPostion.get(l) == null) {
					annotationsPertype = new HashMap<>();

				} else {
					annotationsPertype = annotationsPerPostion.get(l);
				}
				Feature linkNext = linkFs.getType().getFeatureByBaseName(NEXT);
				int linkNo = 1;
				while (linkFs != null) {
					AnnotationFS nextLinkFs = (AnnotationFS) linkFs.getFeatureValue(linkNext);
					if (nextLinkFs != null) {
						addChinFeatureAnno(annotationsPertype, lType, linkFs, unit, linkNo, chainNo);
					} else {
						addChinFeatureAnno(annotationsPertype, lType, linkFs, unit, linkNo, chainNo);
					}
					linkFs = nextLinkFs;
					linkNo++;
					if (nextLinkFs != null)
						unit = getUnit(linkFs.getBegin(), linkFs.getEnd(), linkFs.getCoveredText());
				}
				if (annotationsPertype.keySet().size() > 0) {
					annotationsPerPostion.put(l, annotationsPertype);
				}
				chainNo++;
			}
		}
	}

	private void setRelationAnnotation(JCas aJCas) {
		for (String l : relationLayers) {
			if (l.equals(Token.class.getName())) {
				continue;
			}
			Map<AnnotationUnit, List<List<String>>> annotationsPertype;
			if (annotationsPerPostion.get(l) == null) {
				annotationsPertype = new HashMap<>();

			} else {
				annotationsPertype = annotationsPerPostion.get(l);
			}
			Type type = getType(aJCas.getCas(), l);
			Feature dependentFeature = null;
			Feature governorFeature = null;

			for (Feature feature : type.getFeatures()) {
				if (feature.getShortName().equals(DEPENDENT)) {
					dependentFeature = feature;
				}
				if (feature.getShortName().equals(GOVERNOR)) {
					governorFeature = feature;
				}
			}
			for (AnnotationFS fs : CasUtil.select(aJCas.getCas(), type)) {
				AnnotationFS depFs = (AnnotationFS) fs.getFeatureValue(dependentFeature);
				AnnotationFS govFs = (AnnotationFS) fs.getFeatureValue(governorFeature);
				if (type.getName().equals(Dependency.class.getName())) {
					depFs = ((Token) depFs).getPos();
					govFs = ((Token) govFs).getPos();
				}

				AnnotationUnit govUnit = getUnit(govFs.getBegin(), govFs.getEnd(), govFs.getCoveredText());
				AnnotationUnit depUnit = getUnit(depFs.getBegin(), depFs.getEnd(), depFs.getCoveredText());
				// sometimes there are POS annotation attached to the token  but
				// not in the POS type
				int govRef = annotaionRef.get(((FeatureStructureImpl) govFs).getAddress()) == null
						? getRefId(govFs.getType(), govFs, govUnit)
						: annotaionRef.get(((FeatureStructureImpl) govFs).getAddress());
				int depRef = annotaionRef.get(((FeatureStructureImpl) depFs).getAddress()) == null
						? getRefId(depFs.getType(), depFs, govUnit)
						: annotaionRef.get(((FeatureStructureImpl) depFs).getAddress());
				setRelationAnnoPerFeature(annotationsPertype, type, fs, depUnit, govUnit, govRef, depRef,
						govFs.getType());

			}
			if (annotationsPertype.keySet().size() > 0) {
				annotationsPerPostion.put(l, annotationsPertype);
			}
		}
	}

	private boolean isMultiToken(AnnotationFS aFs) {

		for (AnnotationUnit unit : units) {
			if (unit.begin <= aFs.getBegin() && unit.end > aFs.getBegin() && unit.end < aFs.getEnd()) {
				return true;
			}
		}
		return false;
	}

	private AnnotationUnit getUnit(int aBegin, int aEnd, String aText) {
		for (AnnotationUnit unit : units) {
			if (unit.begin == aBegin && unit.end == aEnd) {
				return unit;
			}
		}
		return new AnnotationUnit(aBegin, aEnd, false, aText);
	}

	private Set<AnnotationUnit> getSubUnits(SubTokenAnno aSTA, Set<AnnotationUnit> aSubUnits) {
		List<AnnotationUnit> tmpUnits = new ArrayList<>(units);
		for (AnnotationUnit unit : units) {
			// this is a sub-token annotation
			if (unit.begin <= aSTA.getBegin() && aSTA.getBegin() <= unit.end && aSTA.getEnd() <= unit.end) {
				AnnotationUnit newUnit = new AnnotationUnit(aSTA.getBegin(), aSTA.getEnd(), false, aSTA.getText());

				updateUnitLists(tmpUnits, unit, newUnit);

				aSubUnits.add(newUnit);
			}
			// if sub-token annotation crosses multiple tokens
			else if (unit.begin <= aSTA.getBegin() && aSTA.getBegin() < unit.end && aSTA.getEnd() > unit.end) {
				int thisSubTextLen = unit.end - aSTA.begin;

				AnnotationUnit newUnit = new AnnotationUnit(aSTA.getBegin(), unit.end, false,
						aSTA.getText().substring(0, thisSubTextLen));
				aSubUnits.add(newUnit);

				updateUnitLists(tmpUnits, unit, newUnit);

				aSTA.setBegin(getNextUnitBegin(aSTA.getBegin()));
				
				aSTA.setText(aSTA.getText().trim().substring(thisSubTextLen));
				getSubUnits(aSTA, aSubUnits);
			} else if (unit.end > aSTA.end) {
				break;
			}
		}
		units = new ArrayList<>(tmpUnits);
		return aSubUnits;
	}

	private int getNextUnitBegin(int aSTABegin) {
		for (AnnotationUnit unit : units) {
			if (unit.begin > aSTABegin && !unit.isSubtoken) {
				return unit.begin;
			}
		}
		// this is the last token
		return aSTABegin;
	}

	/**
	 * If there is at least one non-sub-token annotation whose begin is larger
	 * than this one, it is a multiple tokens (or crossing multiple tokens)
	 * annotation
	 * 
	 * @param aBegin
	 * @param aEnd
	 * @return
	 */
	private boolean isMultipleToken(int aBegin, int aEnd) {
		for (AnnotationUnit unit : units) {
			if (unit.begin > aBegin && unit.begin < aEnd && !unit.isSubtoken) {
				return true;
			}
		}
		// this is the last token
		return false;
	}

	private void updateUnitLists(List<AnnotationUnit> tmpUnits, AnnotationUnit unit, AnnotationUnit newUnit) {
		if (!tmpUnits.contains(newUnit)) {
			newUnit.isSubtoken = true;
			// is this sub-token already there
			if (!tmpUnits.contains(newUnit)) {
				tmpUnits.add(tmpUnits.indexOf(unit) + 1, newUnit);
				subUnits.put(unit, subUnits.getOrDefault(unit, 0) + 1);
				unitsLineNumber.put(newUnit, unitsLineNumber.get(unit) + "." + subUnits.get(unit));
			}
		}
	}

	private void setSpanAnnoPerFeature(Map<AnnotationUnit, List<List<String>>> annotationsPertype, Type type,
			AnnotationFS fs, AnnotationUnit unit, boolean aIsMultiToken, boolean aIsFirst) {
		List<String> annoPerFeatures = new ArrayList<>();
		featurePerLayer.putIfAbsent(type.getName(), new LinkedHashSet<>());
		int ref = getRefId(type, fs, unit);
		for (Feature feature : type.getFeatures()) {
			if (feature.toString().equals("uima.cas.AnnotationBase:sofa")
					|| feature.toString().equals("uima.tcas.Annotation:begin")
					|| feature.toString().equals("uima.tcas.Annotation:end") || feature.getShortName().equals(GOVERNOR)
					|| feature.getShortName().equals(DEPENDENT) || feature.getShortName().equals(FIRST)
					|| feature.getShortName().equals(NEXT)) {
				continue;
			}

			// if slot feature
			if (slotFeatures != null && slotFeatures.contains(feature.getName())) {
				if (fs.getFeatureValue(feature) != null) {
					ArrayFS array = (ArrayFS) fs.getFeatureValue(feature);
					StringBuffer sbRole = new StringBuffer();
					StringBuffer sbTarget = new StringBuffer();
					for (FeatureStructure linkFS : array.toArray()) {
						String role = linkFS.getStringValue(linkFS.getType().getFeatureByBaseName("role"));
						AnnotationFS targetFs = (AnnotationFS) linkFS
								.getFeatureValue(linkFS.getType().getFeatureByBaseName("target"));
						Type tType = targetFs.getType();

						AnnotationUnit firstUnit = getFirstUnit(targetFs);
						ref = getRefId(tType, targetFs, firstUnit);

						if (role == null) {
							role = feature.getName();
						} else {
							// Escape special character
							role = replaceEscapeChars(role);
						}
						if (sbRole.length() < 1) {
							sbRole.append(role);
							// record the actual target type column number if slot target is
							// uima.tcas.Annotation
							int targetTypeNumber = 0;
							if (slotFeatureTypes.get(feature).getName().equals(CAS.TYPE_NAME_ANNOTATION)) {
								targetTypeNumber = layerMaps.get(tType);
							}
							sbTarget.append(
									unitsLineNumber.get(firstUnit) + (targetTypeNumber ==0 ? "" : "-" + targetTypeNumber)
											+ (ref > 0 ? "[" + ref + "]" : ""));
						} else {
							sbRole.append("|");
							sbTarget.append("|");
							sbRole.append(role);
							int targetTypeNumber = 0;
							if (slotFeatureTypes.get(feature).getName().equals(CAS.TYPE_NAME_ANNOTATION)) {
								targetTypeNumber = layerMaps.get(tType);
							}
							sbTarget.append(
									unitsLineNumber.get(firstUnit) + (targetTypeNumber ==0 ? "" : "-" + targetTypeNumber)
											+ (ref > 0 ? "[" + ref + "]" : ""));
						}
					}
					annoPerFeatures.add(sbRole.toString().isEmpty() ? "_" : sbRole.toString());
					annoPerFeatures.add(sbTarget.toString().isEmpty() ? "_" : sbTarget.toString());
				} else {
					// setting it to null
					annoPerFeatures.add("_");
					annoPerFeatures.add("_");
				}
				featurePerLayer.get(type.getName())
						.add(ROLE + feature.getName() + "_" + slotLinkTypes.get(feature.getName()));
				featurePerLayer.get(type.getName()).add(slotFeatureTypes.get(feature).getName());
			} else {
				String annotation = fs.getFeatureValueAsString(feature);
				if (annotation == null) {
					annotation = feature.getName();
				} else {
					// Escape special character
					annotation = replaceEscapeChars(annotation);
				}
				annotation = annotation + (ref > 0 ? "[" + ref + "]" : "");
				// only add BIO markers to multiple annotations
				setAnnoFeature(aIsMultiToken, aIsFirst, annoPerFeatures, annotation);

				featurePerLayer.get(type.getName()).add(feature.getShortName());
			}
		}
		annotationsPertype.putIfAbsent(unit, new ArrayList<>());
		annotationsPertype.get(unit).add(annoPerFeatures);
	}

	/**
	 * 
	 * @param aAnnotationsPertype
	 *            store annotations per type associated with the annotation
	 *            units
	 * @param aType
	 *            the coreference annotation type
	 * @param aFs
	 *            the feature structure
	 * @param aUnit
	 *            the current annotation unit of the coreference chain
	 * @param aLinkNo
	 *            a reference to the link in a chain, starting at one for the
	 *            first link and n for the last link in the chain
	 * @param achainNo
	 *            a reference to the chain, starting at 1 for the first chain
	 *            and n for the last chain where n is the number of coreference
	 *            chains the document
	 */

	private void addChinFeatureAnno(Map<AnnotationUnit, List<List<String>>> aAnnotationsPertype, Type aType,
			AnnotationFS aFs, AnnotationUnit aUnit, int aLinkNo, int achainNo) {
		featurePerLayer.putIfAbsent(aType.getName(), new LinkedHashSet<>());
		// StringBuffer sbAnnotation = new StringBuffer();
		// annotation is per Token
		if (units.contains(aUnit)) {
			setChainAnnoPerFeature(aAnnotationsPertype, aType, aFs, aUnit, aLinkNo, achainNo, false, false);
		}
		// Annotation is on sub-token or multiple tokens
		else {
			SubTokenAnno sta = new SubTokenAnno();
			sta.setBegin(aFs.getBegin());
			sta.setEnd(aFs.getEnd());
			sta.setText(aFs.getCoveredText());
			boolean isMultiToken = isMultiToken(aFs);
			boolean isFirst = true;
			Set<AnnotationUnit> sus = new LinkedHashSet<>();
			for (AnnotationUnit newUnit : getSubUnits(sta, sus)) {
				setChainAnnoPerFeature(aAnnotationsPertype, aType, aFs, newUnit, aLinkNo, achainNo, isMultiToken,
						isFirst);
				isFirst = false;
			}
		}
	}

	private void setChainAnnoPerFeature(Map<AnnotationUnit, List<List<String>>> aAnnotationsPertype, Type aType,
			AnnotationFS aFs, AnnotationUnit aUnit, int aLinkNo, int achainNo, boolean aMultiUnit, boolean aFirst) {
		List<String> annoPerFeatures = new ArrayList<>();
		for (Feature feature : aType.getFeatures()) {
			if (feature.toString().equals("uima.cas.AnnotationBase:sofa")
					|| feature.toString().equals("uima.tcas.Annotation:begin")
					|| feature.toString().equals("uima.tcas.Annotation:end") || feature.getShortName().equals(GOVERNOR)
					|| feature.getShortName().equals(DEPENDENT) || feature.getShortName().equals(FIRST)
					|| feature.getShortName().equals(NEXT)) {
				continue;
			}
			String annotation = aFs.getFeatureValueAsString(feature);
			
			if (annotation == null)
				annotation = feature.getName();
			else
				annotation = replaceEscapeChars(annotation);

			if (feature.getShortName().equals(REF_REL)) {
				annotation = annotation + "->" + achainNo + "-" + aLinkNo;
			} else if (aMultiUnit) {
				if (aFirst) {
					annotation = "B-" + annotation + "[" + achainNo + "]";
				} else {
					annotation = "I-" + annotation + "[" + achainNo + "]";
				}
			} else {
				annotation = annotation + "[" + achainNo + "]";
			}
			featurePerLayer.get(aType.getName()).add(feature.getShortName());

			annoPerFeatures.add(annotation);
		}
		aAnnotationsPertype.putIfAbsent(aUnit, new ArrayList<>());
		aAnnotationsPertype.get(aUnit).add(annoPerFeatures);
	}

	private void setRelationAnnoPerFeature(Map<AnnotationUnit, List<List<String>>> annotationsPertype, Type type,
			AnnotationFS fs, AnnotationUnit depUnit, AnnotationUnit govUnit, int aGovRef, int aDepRef, Type aDepType) {
		List<String> annoPerFeatures = new ArrayList<>();
		featurePerLayer.putIfAbsent(type.getName(), new LinkedHashSet<>());
		for (Feature feature : type.getFeatures()) {
			if (feature.toString().equals("uima.cas.AnnotationBase:sofa")
					|| feature.toString().equals("uima.tcas.Annotation:begin")
					|| feature.toString().equals("uima.tcas.Annotation:end") || feature.getShortName().equals(GOVERNOR)
					|| feature.getShortName().equals(DEPENDENT) || feature.getShortName().equals(FIRST)
					|| feature.getShortName().equals(NEXT)) {
				continue;
			}
			String annotation = fs.getFeatureValueAsString(feature);
			if (annotation == null) {
				annotation = feature.getName();
			}
			else{
				annotation = replaceEscapeChars(annotation);
			}
			annoPerFeatures.add(annotation);
			featurePerLayer.get(type.getName()).add(feature.getShortName());
		}
		// add the governor and dependent unit addresses (separated by _
		String govRef = unitsLineNumber.get(govUnit)
				+ ((aDepRef > 0 || aGovRef > 0) ? "[" + aGovRef + "_" + aDepRef + "]" : "");
		annoPerFeatures.add(govRef);
		featurePerLayer.get(type.getName()).add(BT + aDepType.getName());
		// the column for the dependent unit address
		annotationsPertype.putIfAbsent(depUnit, new ArrayList<>());
		annotationsPertype.get(depUnit).add(annoPerFeatures);
	}

	private String replaceEscapeChars(String annotation) {
		return annotation.replace("[", "\\[").replace("]", "\\]").replace("|", "\\|").replace("_", "\\_")
			.replace("->", "\\->");
	}

	private void setAnnoFeature(boolean aIsMultiToken, boolean aIsFirst, List<String> aAnnoPerFeatures,
			String annotation) {
		if (aIsMultiToken) {
			if (aIsFirst) {
				aAnnoPerFeatures.add("B-" + annotation);
			} else {
				aAnnoPerFeatures.add("I-" + annotation);
			}
		} else {
			aAnnoPerFeatures.add(annotation);
		}
	}

	private AnnotationUnit getFirstUnit(AnnotationFS targetFs) {
		SubTokenAnno sta = new SubTokenAnno();
		sta.setBegin(targetFs.getBegin());
		sta.setEnd(targetFs.getEnd());
		sta.setText(targetFs.getCoveredText());
		Set<AnnotationUnit> sus = new LinkedHashSet<>();
		AnnotationUnit firstUnit = null;
		for (AnnotationUnit u : getSubUnits(sta, sus)) {
			firstUnit = u;
			break;
		}
		return firstUnit;
	}

	/**
	 * Annotations of same type those: <br>
	 * 1) crosses multiple sentences AND <br>
	 * 2) repeated on the same unit (even if different value) <br>
	 * Will be referenced by a number so that re-importing or processing outside
	 * WebAnno can be easily distinguish same sets of annotations. This much
	 * Meaningful for relation/slot and chain annotations
	 * 
	 * @param type
	 *            The annotation type
	 * @param fs
	 *            the annotation
	 * @param unit
	 *            the annotation element (Token or sub-tokens)
	 * @return the reference number to be attached on this annotation value
	 */
	private int getRefId(Type type, AnnotationFS fs, AnnotationUnit unit) {
		if (annotaionRef.get(((FeatureStructureImpl) fs).getAddress()) == null) {
			int i = isMultipleToken(fs.getBegin(), fs.getEnd()) ? 1 : 0;
			unitRef.putIfAbsent(type.getName(), new HashMap<>());
			unitRef.get(type.getName()).put(unit, unitRef.get(type.getName()).getOrDefault(unit, 0) + i);
			annotaionRef.put(((FeatureStructureImpl) fs).getAddress(), unitRef.get(type.getName()).get(unit));
		} else {
			unitRef.get(type.getName()).put(unit, annotaionRef.get(((FeatureStructureImpl) fs).getAddress()));
		}
		return unitRef.get(type.getName()).get(unit);
	}

	private void setTokenSentenceAddress(JCas aJCas) {
		int sentNMumber = 1;
		for (Sentence sentence : select(aJCas, Sentence.class)) {
			int lineNumber = 1;
			for (Token token : selectCovered(Token.class, sentence)) {
				AnnotationUnit unit = new AnnotationUnit(token.getBegin(), token.getEnd(), false,
						token.getCoveredText());
				units.add(unit);
				if (lineNumber == 1) {
					sentenceUnits.put(unit, sentence.getCoveredText());
					sentenceBeginEnd.put(unit, sentence.getBegin() + "-" + sentence.getEnd());
				}
				unitsLineNumber.put(unit, sentNMumber + "-" + lineNumber);
				lineNumber++;
			}
			sentNMumber++;
		}

	}

	class SubTokenAnno {
		int begin;
		int end;
		String text;

		public int getBegin() {
			return begin;
		}

		public int getEnd() {
			return end;
		}

		public void setEnd(int end) {
			this.end = end;
		}

		public void setBegin(int begin) {
			this.begin = begin;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

	}
}
