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
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

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

	public static final String RELATION_LAYERS = "relationLayers";
	@ConfigurationParameter(name = RELATION_LAYERS, mandatory = true, defaultValue = {})
	private List<String> relationLayers;

	private static final String TAB = "\t";
	private static final String LF = "\n";
	private final String DEPENDENT = "Dependent";
	private final String GOVERNOR = "Governor";
	private List<AnnotationUnit> units;
	private Map<String, Set<String>> featurePerLayer = new LinkedHashMap<>();
	private Map<AnnotationUnit, String> unitsLineNumbers;
	private Map<AnnotationUnit, String> sentenceUnits = new HashMap<>();
	private Map<String, Map<AnnotationUnit, List<List<String>>>> annotationsoerPostion;

	private Map<Integer, Integer> annotaionRef = new HashMap<>();
	private Map<String, Map<AnnotationUnit, Integer>> unitRef = new HashMap<>();
	// reference annotations per offset per type
	private Map<String, Map<String, Integer>> annosPerType = new HashMap<>();

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		OutputStream docOS = null;
		try {
			docOS = getOutputStream(aJCas, filenameSuffix);
			// convertToTsv(aJCas, docOS, encoding);
			setTokenSentenceAddress(aJCas);
			setAnnotation(aJCas);

			writeHeader(docOS);
			String tokenLineNumber = "";
			int subtokenLineNumber = 1;
			for (AnnotationUnit unit : units) {
				if (sentenceUnits.containsKey(unit)) {
					IOUtils.write(LF + "#" + sentenceUnits.get(unit) + LF, docOS, encoding);
				}
				if (unit.isSubtoken) {
					IOUtils.write("-->" + tokenLineNumber + "." + subtokenLineNumber + TAB + unit.begin + "-" + unit.end
							+ TAB + unit.token + TAB, docOS, encoding);
					subtokenLineNumber++;
				} else {
					/*
					 * IOUtils.write(unitsLineNumbers.get(unit) + TAB +
					 * unit.token + TAB, docOS, encoding); subtokenLineNumber =
					 * 1; tokenLineNumber = unitsLineNumbers.get(unit);
					 */
					IOUtils.write(
							unitsLineNumbers.get(unit) + TAB + unit.begin + "-" + unit.end + TAB + unit.token + TAB,
							docOS, encoding);
					subtokenLineNumber = 1;
					tokenLineNumber = unitsLineNumbers.get(unit);
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
					} // No annotation of this taype in this layer
					else {
						for (String feature : featurePerLayer.get(type)) {
							IOUtils.write("_" + TAB, docOS, encoding);
						}
					}
				}
				IOUtils.write(LF, docOS, encoding);
			}

			for (String layer : annotationsoerPostion.keySet()) {
				for (AnnotationUnit unit : annotationsoerPostion.get(layer).keySet()) {
					List<List<String>> annos = annotationsoerPostion.get(layer).get(unit);
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
					System.out.println(merged);
				}
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
			IOUtils.write(" # " + type + TAB, docOS, encoding);
			for (String feature : featurePerLayer.get(type)) {
				IOUtils.write(feature + TAB, docOS, encoding);
			}
		}
		IOUtils.write(LF, docOS, encoding);
	}

	private void setAnnotation(JCas aJCas) {
		annotationsoerPostion = new HashMap<>();
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
					setAnnoPerFeature(annotationsPertype, type, fs, unit, false, false);
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
						setAnnoPerFeature(annotationsPertype, type, fs, newUnit, isMultiToken, isFirst);
						isFirst = false;
					}
				}
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
			}
		}
	}

	private void setAnnoPerFeature(Map<AnnotationUnit, List<List<String>>> annotationsPertype, Type type,
			AnnotationFS fs, AnnotationUnit unit, boolean aIsMultiToken, boolean aIsFirst) {
		List<String> annoPerFeatures = new ArrayList<>();

		int ref = getRefId(type, fs, unit);

		for (Feature feature : type.getFeatures()) {
			if (feature.toString().equals("uima.cas.AnnotationBase:sofa")
					|| feature.toString().equals("uima.tcas.Annotation:begin")
					|| feature.toString().equals("uima.tcas.Annotation:end") || feature.getShortName().equals(GOVERNOR)
					|| feature.getShortName().equals(DEPENDENT)) {
				continue;
			}
			featurePerLayer.putIfAbsent(type.getName(), new LinkedHashSet<>());
			featurePerLayer.get(type.getName()).add(feature.getName());

/*			try {
				ArrayFS array = (ArrayFS) fs.getFeatureValue(feature);
				for (FeatureStructure linkFS : array.toArray()) {
					String role = linkFS.getStringValue(linkFS.getType().getFeatureByBaseName("role"));
					AnnotationFS target = (AnnotationFS) linkFS
							.getFeatureValue(linkFS.getType().getFeatureByBaseName("target"));
					System.out.println(role);
				}
			} catch (Exception e) {

			}*/

			String annotation = fs.getFeatureValueAsString(feature);
			if (annotation == null) {
				annotation = feature.getName();
			}
			annotation = annotation + (ref > 1 ? "[" + ref + "]" : "");
			// only add BIO markers to multiple annotations
			if (aIsMultiToken) {
				if (aIsFirst) {
					annoPerFeatures.add("B-" + annotation);
				} else {
					annoPerFeatures.add("I-" + annotation);
				}
			} else {
				annoPerFeatures.add(annotation);
			}

		}
		annotationsPertype.putIfAbsent(unit, new ArrayList<>());
		annotationsPertype.get(unit).add(annoPerFeatures);
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
		units = new ArrayList<>();
		unitsLineNumbers = new HashMap<>();
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
				unitsLineNumbers.put(unit, sentNMumber + "-" + lineNumber);
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
