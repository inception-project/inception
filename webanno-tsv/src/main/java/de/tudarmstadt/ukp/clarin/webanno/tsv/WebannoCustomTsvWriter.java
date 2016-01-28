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
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureStructureImpl;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;

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

public class WebannoCustomTsvWriter extends JCasFileWriter_ImplBase {

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

	public static final String SPAN_LAYERS = "spanLayers";
	@ConfigurationParameter(name = SPAN_LAYERS, mandatory = true, defaultValue = {})
	private List<String> spanLayers;

	public static final String Chain_LAYERS = "chainLayers";
	@ConfigurationParameter(name = Chain_LAYERS, mandatory = true, defaultValue = {})
	private List<String> chainLayers;

	public static final String RELATION_LAYERS = "relationLayers";
	@ConfigurationParameter(name = RELATION_LAYERS, mandatory = true, defaultValue = {})
	private List<String> relationLayers;

	private static final String TAB = "\t";
	private static final String LF = "\n";
	private final String DEPENDENT = "Dependent";
	private final String GOVERNOR = "Governor";
	private final String CHAIN = "Chain";
	private final String LINK = "Link";
	private final String FIRST = "first";
	private final String NEXT = "next";

	private List<AnnotationUnit> units = new ArrayList<>();;
	// number of subunits under this Annotation Unit
	private Map<AnnotationUnit, Integer> subUnits = new HashMap<>();
	private Map<String, Set<String>> featurePerLayer = new LinkedHashMap<>();
	private Map<AnnotationUnit, String> unitsLineNumber = new HashMap<>();
	private Map<AnnotationUnit, String> sentenceUnits = new HashMap<>();
	private Map<String, Map<AnnotationUnit, List<List<String>>>> annotationsoerPostion = new HashMap<>();

	private Map<Integer, Integer> annotaionRef = new HashMap<>();
	private Map<String, Map<AnnotationUnit, Integer>> unitRef = new HashMap<>();

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		OutputStream docOS = null;
		try {
			docOS = getOutputStream(aJCas, filenameSuffix);
			// convertToTsv(aJCas, docOS, encoding);
			setTokenSentenceAddress(aJCas);
			setSpanAnnotation(aJCas);
			setChainAnnotation(aJCas);
			setRelationAnnotation(aJCas);
			writeHeader(docOS);
			for (AnnotationUnit unit : units) {
				if (sentenceUnits.containsKey(unit)) {
					IOUtils.write(LF + "#" + sentenceUnits.get(unit) + LF, docOS, encoding);
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
					List<List<String>> annos = annotationsoerPostion.getOrDefault(type, new HashMap<>())
							.getOrDefault(unit, new ArrayList<>());
					List<String> merged = null;
					for (List<String> annofs : annos) {
						if (merged == null) {
							merged = annofs;
						} else {

							for (int i = 0; i < annofs.size(); i++) {
								merged.set(i, merged.get(i) + "|" + annofs.get(i));
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

	/**
	 * Write headers, in the sequence <br>
	 * Type TAB List(Features sep by TAB)
	 * 
	 * @param docOS
	 * @throws IOException
	 */
	private void writeHeader(OutputStream docOS) throws IOException {
		for (String type : featurePerLayer.keySet()) {
			String annoType;
			if (spanLayers.contains(type)) {
				annoType = "SPAN";
			} else if (chainLayers.contains(type)) {
				annoType = "CHAIN";
			} else {
				annoType = "RELATION";
			}
			IOUtils.write("#_" + annoType + "_" + type + "|", docOS, encoding);
			StringBuffer fsb = new StringBuffer();
			for (String feature : featurePerLayer.get(type)) {
				if (fsb.length() < 1) {
					fsb.append(feature);
				} else {
					fsb.append("|" + feature);
				}
			}
			IOUtils.write(fsb.toString(), docOS, encoding);
		}
		IOUtils.write(LF, docOS, encoding);
	}

	private void setSpanAnnotation(JCas aJCas) {
		for (String l : spanLayers) {
			if (l.equals(Token.class.getName())) {
				continue;
			}
			Map<AnnotationUnit, List<List<String>>> annotationsPertype;
			if (annotationsoerPostion.get(l) == null) {
				annotationsPertype = new HashMap<>();

			} else {
				annotationsPertype = annotationsoerPostion.get(l);
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
					for (AnnotationUnit newUnit : getUnits(sta, sus)) {
						setSpanAnnoPerFeature(annotationsPertype, type, fs, newUnit, isMultiToken, isFirst);
						isFirst = false;
					}
				}
			}
			if (annotationsPertype.keySet().size() > 0) {
				annotationsoerPostion.put(l, annotationsPertype);
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
			for (FeatureStructure chainFs : selectFS(aJCas.getCas(), type)) {
				AnnotationFS linkFs = (AnnotationFS) chainFs.getFeatureValue(chainFirst);
				AnnotationUnit unit = getUnit(linkFs.getBegin(), linkFs.getEnd(), linkFs.getCoveredText());
				Type lType = linkFs.getType();
				AnnotationUnit nextUnit = null;

				// this is the layer with annotations
				l = lType.getName();
				if (annotationsoerPostion.get(l) == null) {
					annotationsPertype = new HashMap<>();

				} else {
					annotationsPertype = annotationsoerPostion.get(l);
				}
				Feature linkNext = linkFs.getType().getFeatureByBaseName(NEXT);

				AnnotationFS tmpFs = linkFs;
				int ref = 1;
				AnnotationUnit tmpUnit = unit;
				while (tmpFs != null) {
					int tmpRef = getRefId(lType, tmpFs, tmpUnit);
					if (tmpRef > ref) {
						ref = tmpRef;
					}
					tmpFs = (AnnotationFS) tmpFs.getFeatureValue(linkNext);
					if (tmpFs != null)
						tmpUnit = getUnit(tmpFs.getBegin(), tmpFs.getEnd(), tmpFs.getCoveredText());
				}

				while (linkFs != null) {
					AnnotationFS nextLinkFs = (AnnotationFS) linkFs.getFeatureValue(linkNext);
					if (nextLinkFs != null) {
						nextUnit = getUnit(nextLinkFs.getBegin(), nextLinkFs.getEnd(), nextLinkFs.getCoveredText());
						addChinFeatureAnno(annotationsPertype, lType, linkFs, unit, nextUnit, ref);
					} else {
						nextUnit = null;
						addChinFeatureAnno(annotationsPertype, lType, linkFs, unit, nextUnit, ref);
					}
					linkFs = nextLinkFs;
					unit = nextUnit;

				}
				if (annotationsPertype.keySet().size() > 0) {
					annotationsoerPostion.put(l, annotationsPertype);
				}
			}
		}
	}

	private void setRelationAnnotation(JCas aJCas) {
		for (String l : relationLayers) {
			if (l.equals(Token.class.getName())) {
				continue;
			}
			Map<AnnotationUnit, List<List<String>>> annotationsPertype;
			if (annotationsoerPostion.get(l) == null) {
				annotationsPertype = new HashMap<>();

			} else {
				annotationsPertype = annotationsoerPostion.get(l);
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
					depFs = selectCovered(aJCas.getCas(), aJCas.getTypeSystem().getType(POS.class.getName()),
							depFs.getBegin(), depFs.getEnd()).get(0);
					govFs = selectCovered(aJCas.getCas(), aJCas.getTypeSystem().getType(POS.class.getName()),
							govFs.getBegin(), govFs.getEnd()).get(0);
				}

				AnnotationUnit depUnit = getUnit(depFs.getBegin(), depFs.getEnd(), depFs.getCoveredText());
				int govRef = annotaionRef.get(((FeatureStructureImpl) govFs).getAddress());
				int depRef = annotaionRef.get(((FeatureStructureImpl) depFs).getAddress());
				setRelationAnnoPerFeature(annotationsPertype, type, fs, depUnit, govRef, depRef, govFs.getType());

			}
			if (annotationsPertype.keySet().size() > 0) {
				annotationsoerPostion.put(l, annotationsPertype);
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
		List<AnnotationUnit> tmpUnits = new ArrayList<>(units);
		for (AnnotationUnit unit : units) {
			if (unit.begin == aBegin && unit.end == aEnd) {
				return unit;
			}
		}
		return new AnnotationUnit(aBegin, aEnd, false, aText);
	}

	private Set<AnnotationUnit> getUnits(SubTokenAnno aSTA, Set<AnnotationUnit> aSubUnits) {
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
				aSTA.setText(aSTA.getText().substring(thisSubTextLen + 1));
				getUnits(aSTA, aSubUnits);
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
				unitsLineNumber.put(newUnit, "-->" + unitsLineNumber.get(unit) + "." + subUnits.get(unit));
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
			try {
				ArrayFS array = (ArrayFS) fs.getFeatureValue(feature);
				StringBuffer sb = new StringBuffer();
				for (FeatureStructure linkFS : array.toArray()) {
					String role = linkFS.getStringValue(linkFS.getType().getFeatureByBaseName("role"));
					AnnotationFS targetFs = (AnnotationFS) linkFS
							.getFeatureValue(linkFS.getType().getFeatureByBaseName("target"));
					Type tType = targetFs.getType();

					AnnotationUnit firstUnit = getFirstUnit(targetFs);
					ref = getRefId(tType, targetFs, firstUnit);

					if (role == null) {
						role = feature.getName();
					}
					if (sb.length() < 1) {
						sb.append(role + "[" + unitsLineNumber.get(firstUnit) + (ref > 1 ? "[" + ref + "]" : "") + "]");
					} else {
						sb.append("|");
						sb.append(role + "[" + unitsLineNumber.get(firstUnit) + (ref > 1 ? "[" + ref + "]" : "") + "]");
					}
					featurePerLayer.get(type.getName())
							.add("_ROLE_" + linkFS.getType().getFeatureByBaseName("role").getName() + "["
									+ linkFS.getType().getFeatureByBaseName("target").getName() + "=" + tType.getName()
									+ "]");
				}
				annoPerFeatures.add(sb.toString());
				continue;
				// non-slot features
			} catch (Exception e) {
				String annotation = fs.getFeatureValueAsString(feature);
				if (annotation == null) {
					annotation = feature.getName();
				}
				annotation = annotation + (ref > 1 ? "[" + ref + "]" : "");
				// only add BIO markers to multiple annotations
				setAnnoFeature(aIsMultiToken, aIsFirst, annoPerFeatures, annotation);

				featurePerLayer.get(type.getName()).add(feature.getName());
			}

		}
		annotationsPertype.putIfAbsent(unit, new ArrayList<>());
		annotationsPertype.get(unit).add(annoPerFeatures);
	}

	private void addChinFeatureAnno(Map<AnnotationUnit, List<List<String>>> annotationsPertype, Type type,
			AnnotationFS fs, AnnotationUnit aUnit, AnnotationUnit aNextUnit, int aRef) {
		featurePerLayer.putIfAbsent(type.getName(), new LinkedHashSet<>());
		List<String> annoPerFeatures = new ArrayList<>();
		StringBuffer sbAnnotation = new StringBuffer();
		StringBuffer sbFeature = new StringBuffer();
		for (Feature feature : type.getFeatures()) {
			if (feature.toString().equals("uima.cas.AnnotationBase:sofa")
					|| feature.toString().equals("uima.tcas.Annotation:begin")
					|| feature.toString().equals("uima.tcas.Annotation:end") || feature.getShortName().equals(GOVERNOR)
					|| feature.getShortName().equals(DEPENDENT) || feature.getShortName().equals(FIRST)
					|| feature.getShortName().equals(NEXT)) {
				continue;
			}
			String annotation = fs.getFeatureValueAsString(feature) == null ? feature.getName()
					: fs.getFeatureValueAsString(feature);
			if (sbAnnotation.length() < 1) {
				sbAnnotation.append(annotation);

				sbFeature.append(feature.getName());
			} else {
				sbAnnotation.append("_");
				sbAnnotation.append(annotation);

				sbFeature.append("_");
				sbFeature.append(feature.getName());

			}
		}
		featurePerLayer.get(type.getName()).add(sbFeature.toString());
		if (aNextUnit != null) {
			sbAnnotation.append("[" + unitsLineNumber.get(aNextUnit) + "]");
		}

		if (aRef > 1) {
			sbAnnotation.append("[" + aRef + "]");
		}
		annotationsPertype.putIfAbsent(aUnit, new ArrayList<>());
		annoPerFeatures.add(sbAnnotation.toString());
		annotationsPertype.get(aUnit).add(annoPerFeatures);

	}

	private void setRelationAnnoPerFeature(Map<AnnotationUnit, List<List<String>>> annotationsPertype, Type type,
			AnnotationFS fs, AnnotationUnit unit, int aGovRef, int aDepRef, Type aDepType) {
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
			annotation = annotation + (aGovRef > 1 ? "[" + aGovRef + "]" : "");
			annoPerFeatures.add(annotation);
			featurePerLayer.get(type.getName()).add(feature.getName());
		}
		// add the dependent unit address
		String govRef = unitsLineNumber.get(unit) + (aDepRef > 1 ? "[" + aDepRef + "]" : "");
		annoPerFeatures.add(govRef);
		featurePerLayer.get(type.getName()).add(aDepType.getName());
		// the column for the dependent unit address
		annotationsPertype.putIfAbsent(unit, new ArrayList<>());
		annotationsPertype.get(unit).add(annoPerFeatures);
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
		for (AnnotationUnit u : getUnits(sta, sus)) {
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
			int i = isMultipleToken(fs.getBegin(), fs.getEnd()) ? 2 : 1;
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

	/**
	 * An UNIT to be exported in one line of a TSV file format (annotations
	 * separated by TAB character). <br>
	 * This UNIT can be a Token element or a sub-token element<br>
	 * Sub-token elements start with the "--"
	 *
	 */
	class AnnotationUnit {
		int begin;
		int end;
		String token;
		boolean isSubtoken;

		public AnnotationUnit(int aBegin, int aEnd, boolean aIsSubToken, String aToken) {
			this.begin = aBegin;
			this.end = aEnd;
			this.isSubtoken = aIsSubToken;
			this.token = aToken;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + begin;
			result = prime * result + end;
			result = prime * result + (isSubtoken ? 1231 : 1237);
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			AnnotationUnit other = (AnnotationUnit) obj;
			if (begin != other.begin)
				return false;
			if (end != other.end)
				return false;
			if (isSubtoken != other.isSubtoken)
				return false;
			return true;
		}
	}
}
