/*******************************************************************************
 * Copyright 2012
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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.taskdefs.condition.HasMethod;
import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.tsv.util.AnnotationUnit;
import de.tudarmstadt.ukp.dkpro.core.api.io.JCasResourceCollectionReader_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * This class reads a WebAnno compatible TSV files and create annotations from
 * the information provided. The very beginning of the file, the header,
 * mentions the existing annotation layers with their feature structure
 * information Annotation layers are separated by # character and features by |
 * character. If the layer is a relation annotation, it includes the string
 * AttachToType=... where the attach type is expressed.There is no Chain TSV
 * reader Writer yet.
 *
 *
 */
public class WebannoCustomTsvReader extends JCasResourceCollectionReader_ImplBase {

	private static final String TAB = "\t";
	private static final String LF = "\n";

	private String fileName;
	private Map<String, Token> indexedTokens;
	private int columns = 2;// token number + token columns (minimum required)
	private Map<Type, Set<Feature>> spanLayers = new LinkedHashMap<Type, Set<Feature>>();
	private Map<Type, Set<Feature>> relationLayers = new LinkedHashMap<Type, Set<Feature>>();
	private Map<Feature, Type> roleLinks = new HashMap<>();
	private Map<Feature, Type> roleTargets = new HashMap<>();
	private Map<Feature, Type> slotLinkTypes = new HashMap<>();
	private Map<Type, Set<Feature>> chainLayers = new LinkedHashMap<Type, Set<Feature>>();
	private StringBuilder coveredText = new StringBuilder();
	// for each type, for each unit, annotations per position
	private Map<Type, Map<AnnotationUnit, List<String>>> annotationsPerPostion = new LinkedHashMap<>();
	private List<AnnotationUnit> units = new ArrayList<>();
	private Map<String, AnnotationUnit> token2Units = new HashMap<>();

	// record the annotation at ref position when it is mutiple token annotation
	private Map<Type, Map<AnnotationUnit, Map<Integer, AnnotationFS>>> annoUnitperAnnoFs = new HashMap<>();

	public void convertToCas(JCas aJCas, InputStream aIs, String aEncoding) throws IOException

	{
		DocumentMetaData documentMetadata = DocumentMetaData.get(aJCas);
		fileName = documentMetadata.getDocumentTitle();
		// setLayerAndFeature(aJCas, aIs, aEncoding);

		setAnnotations(aJCas, aIs, aEncoding);
		aJCas.setDocumentText(coveredText.toString());
	}

	/**
	 * Iterate through lines and create span annotations accordingly. For
	 * multiple span annotation, based on the position of the annotation in the
	 * line, update only the end position of the annotation
	 */
	private void setAnnotations(JCas aJCas, InputStream aIs, String aEncoding) throws IOException {

		// getting header information
		LineIterator lineIterator = IOUtils.lineIterator(aIs, aEncoding);
		while (lineIterator.hasNext()) {
			String line = lineIterator.next().trim();
			if (line.startsWith("#T_")) {
				setLayerAndFeature(aJCas, line);
				continue;
			}

			if (line.startsWith("#Text=")) {
				createSentence(aJCas, line);
				continue;
			}
			if (line.trim().isEmpty()) {
				continue;
			}

			int count = StringUtils.countMatches(line, "\t");

			if (columns != count) {
				throw new IOException(fileName + " This is not a valid TSV File. check this line: " + line);
			}
			String[] lines = line.split(TAB);

			int begin = Integer.parseInt(lines[1].split("-")[0]);
			int end = Integer.parseInt(lines[1].split("-")[1]);

			AnnotationUnit unit = createTokens(aJCas, lines, begin, end);

			int ind = 3;

			setSpanAnnosPerTypePerUnit(lines, unit, ind);
		}

		Map<Type, Map<AnnotationUnit, List<AnnotationFS>>> annosPerTypePerUnit = new HashMap<>();
		setAnnosPerUnit(aJCas, annosPerTypePerUnit);

		Map<Integer, AnnotationFS> multiTokUnits = new HashMap<>();
		for (Type type : annotationsPerPostion.keySet()) {
			for (AnnotationUnit unit : annotationsPerPostion.get(type).keySet()) {

				int end = unit.end;
				List<AnnotationFS> annos = annosPerTypePerUnit.get(type).get(unit);
				int j = 0;
				Map<AnnotationFS, List<FeatureStructure>> linkFSesPerAnno = new HashMap<>();
				boolean isSlot = false;
				Feature LinkeF = null;
				for (Feature feat : spanLayers.get(type)) {
					String anno = annotationsPerPostion.get(type).get(unit).get(j);
					if (!annotationsPerPostion.get(type).get(unit).get(0).equals("_")) {
						int i = 0;
						for (String mAnnos : anno.split("\\|\\|")) {
							// if it is a slot annotation (multiple slots per
							// single annotation
							// (Target1<--role1--Base--role2-->Target2)
							int slot = 0;
							for (String mAnno : mAnnos.split("\\|")) {
								int ref = 1;
								if (mAnno.endsWith("]")) {

									ref = Integer.valueOf(mAnno.substring(mAnno.indexOf("[") + 1, mAnno.length() - 1));
									mAnno = mAnno.substring(0, mAnno.indexOf("["));
								}
								if (mAnno.startsWith("B-")) {

									multiTokUnits.put(ref, annos.get(i));
									mAnno = mAnno.substring(2);
								}
								if (mAnno.startsWith("I-")) {

									Feature endF = type.getFeatureByBaseName(CAS.FEATURE_BASE_NAME_END);
									multiTokUnits.get(ref).setIntValue(endF, end);
									setAnnoRefPerUnit(unit, type, ref, multiTokUnits.get(ref));

								} else {
									if (mAnno.equals(feat.getName()))
										mAnno = null;
									if (roleLinks.containsKey(feat)) {
										LinkeF = feat;
										isSlot = true;
										FeatureStructure link = aJCas.getCas().createFS(slotLinkTypes.get(feat));
										Feature roleFeat = link.getType().getFeatureByBaseName("role");
										link.setStringValue(roleFeat, mAnno);
										linkFSesPerAnno.putIfAbsent(annos.get(i), new ArrayList<>());
										linkFSesPerAnno.get(annos.get(i)).add(link);

									} else if (roleTargets.containsKey(feat)) {

										FeatureStructure link = linkFSesPerAnno.get(annos.get(i)).get(slot);
										AnnotationUnit targetUnit = token2Units.get(mAnno);
										AnnotationFS targetFs = annosPerTypePerUnit.get(roleTargets.get(feat))
												.get(targetUnit).get(ref - 1);
										link.setFeatureValue(feat, targetFs);
										slot++;

									} else {

										annos.get(i).setFeatureValueFromString(feat, mAnno);
										aJCas.addFsToIndexes(annos.get(i));
										setAnnoRefPerUnit(unit, type, ref, annos.get(i));
									}

								}
							}
							i++;
						}
					}
					j++;
				}
				if (isSlot) {
					for (AnnotationFS anno : linkFSesPerAnno.keySet()) {
						ArrayFS array = anno.getCAS().createArrayFS(linkFSesPerAnno.get(anno).size());
						array.copyFromArray(
								linkFSesPerAnno.get(anno)
										.toArray(new FeatureStructure[linkFSesPerAnno.get(anno).size()]),
								0, 0, linkFSesPerAnno.get(anno).size());
						anno.setFeatureValue(LinkeF, array);
					}
					linkFSesPerAnno = new HashMap<>();
					isSlot = false;
				}
			}
		}
	}

	private int setSpanAnnosPerTypePerUnit(String[] lines, AnnotationUnit unit, int ind) {
		for (Type type : spanLayers.keySet()) {

			annotationsPerPostion.putIfAbsent(type, new LinkedHashMap<>());
			for (Feature f : spanLayers.get(type)) {
				annotationsPerPostion.get(type).put(unit,
						annotationsPerPostion.get(type).getOrDefault(unit, new ArrayList<>()));
				annotationsPerPostion.get(type).get(unit).add(lines[ind]);
				ind++;
				/*
				 * if (roleTargets.containsKey(f)) { ind++; }
				 */
			}
		}
		return ind;
	}

	private void setAnnosPerUnit(JCas aJCas, Map<Type, Map<AnnotationUnit, List<AnnotationFS>>> aAnnosPerTypePerUnit) {
		for (Type type : annotationsPerPostion.keySet()) {
			Map<AnnotationUnit, List<AnnotationFS>> annosPerUnit = new HashMap<>();
			for (AnnotationUnit unit : annotationsPerPostion.get(type).keySet()) {

				int begin = unit.begin;
				int end = unit.end;
				List<AnnotationFS> annos = new ArrayList<>();
				// if there are multiple annos
				int multAnnos = 1;
				for (String anno : annotationsPerPostion.get(type).get(unit)) {

					if (!anno.startsWith("[") && anno.split("\\|").length > multAnnos) {
						multAnnos = anno.split("\\|").length;
					}
				}

				for (int i = 0; i < multAnnos; i++) {

					annos.add(aJCas.getCas().createAnnotation(type, begin, end));
				}
				annosPerUnit.put(unit, annos);
			}
			aAnnosPerTypePerUnit.put(type, annosPerUnit);
		}
	}

	private void setAnnoRefPerUnit(AnnotationUnit unit, Type type, int ref, AnnotationFS aAnnoFs) {
		annoUnitperAnnoFs.putIfAbsent(type, new HashMap<>());
		annoUnitperAnnoFs.get(type).putIfAbsent(unit, new HashMap<>());
		annoUnitperAnnoFs.get(type).get(unit).put(ref, aAnnoFs);
	}

	private AnnotationUnit createTokens(JCas aJCas, String[] lines, int begin, int end) {

		if (!lines[0].startsWith("-")) {
			Token token = new Token(aJCas, begin, end);
			AnnotationUnit unit = new AnnotationUnit(begin, end, false, "");
			units.add(unit);
			token.addToIndexes();
			token2Units.put(lines[0], unit);
			return unit;
		} else {
			AnnotationUnit unit = new AnnotationUnit(begin, end, true, "");
			units.add(unit);
			token2Units.put(lines[0], unit);
			return unit;
		}
	}

	private void createSentence(JCas aJCas, String line) {
		String text = line.substring(6);
		String beginEnd = text.substring(0, text.indexOf("#"));
		text = text.substring(text.indexOf("#") + 1);

		int begin = Integer.parseInt(beginEnd.split("-")[0]);
		int end = Integer.parseInt(beginEnd.split("-")[1]);

		coveredText.append(text + LF);
		Sentence sentence = new Sentence(aJCas, begin, end);
		sentence.addToIndexes();
	}

	private void setLayerAndFeature(JCas aJcas, String header) throws IOException {
		StringTokenizer headerTk = new StringTokenizer(header, "#");
		while (headerTk.hasMoreTokens()) {
			String layerNames = headerTk.nextToken().trim();
			StringTokenizer layerTk = new StringTokenizer(layerNames, "|");

			Set<Feature> features = new LinkedHashSet<Feature>();
			// get the layer name [ which are coded as 0_=span, 1_=chain, and
			// 2_=relation
			String layerName = layerTk.nextToken().trim();
			String layerType = layerName.substring(0, layerName.indexOf("="));
			layerName = layerName.substring(layerName.indexOf("=") + 1);

			Iterator<Type> types = aJcas.getTypeSystem().getTypeIterator();
			boolean layerExists = false;
			while (types.hasNext()) {

				if (types.next().getName().equals(layerName)) {
					layerExists = true;
					break;
				}
			}
			if (!layerExists) {
				throw new IOException(fileName + " This is not a valid TSV File. The layer " + layerName
						+ " is not created in the project.");
			}
			Type layer = CasUtil.getType(aJcas.getCas(), layerName);

			while (layerTk.hasMoreTokens()) {
				String ft = layerTk.nextToken().trim();
				columns++;
				Feature feature = layer.getFeatureByBaseName(ft);
				if (ft.startsWith("ROLE_")) {
					ft = ft.substring(5);
					String t = layerTk.nextToken().toString();
					columns++;
					Type tType = CasUtil.getType(aJcas.getCas(), t);
					String fName = ft.substring(0, ft.indexOf("_"));
					Feature slotF = layer.getFeatureByBaseName(fName.substring(fName.indexOf(":") + 1));
					if (slotF == null) {
						throw new IOException(fileName + " This is not a valid TSV File. The feature " + ft
								+ " is not created for the layer " + layerName);
					}
					features.add(slotF);
					roleLinks.put(slotF, tType);
					Type slotType = CasUtil.getType(aJcas.getCas(), ft.substring(ft.indexOf("_") + 1));
					Feature tFeatore = slotType.getFeatureByBaseName("target");
					if (tFeatore == null) {
						throw new IOException(fileName + " This is not a valid TSV File. The feature " + ft
								+ " is not created for the layer " + layerName);
					}
					roleTargets.put(tFeatore, tType);
					features.add(tFeatore);
					slotLinkTypes.put(slotF, slotType);
					continue;
				}

				if (feature == null) {
					throw new IOException(fileName + " This is not a valid TSV File. The feature " + ft
							+ " is not created for the layer " + layerName);
				}
				features.add(feature);
			}
			if (layerType.equals(WebannoCustomTsvWriter.SP)) {
				spanLayers.put(layer, features);
			} else if (layerType.equals(WebannoCustomTsvWriter.CH)) {
				chainLayers.put(layer, features);
			} else {
				relationLayers.put(layer, features);
			}

		}
	}

	public static final String PARAM_ENCODING = ComponentParameters.PARAM_SOURCE_ENCODING;
	@ConfigurationParameter(name = PARAM_ENCODING, mandatory = true, defaultValue = "UTF-8")
	private String encoding;

	@Override
	public void getNext(JCas aJCas) throws IOException, CollectionException {
		Resource res = nextFile();
		initCas(aJCas, res);
		InputStream is = null;
		try {
			is = res.getInputStream();
			convertToCas(aJCas, is, encoding);
		} finally {
			closeQuietly(is);
		}

	}
}
