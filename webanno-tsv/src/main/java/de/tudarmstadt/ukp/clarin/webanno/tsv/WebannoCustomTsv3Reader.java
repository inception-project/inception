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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;
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
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 * This class reads a WebAnno compatible TSV files and create annotations from
 * the information provided. The the header of the file records the existing
 * annotation layers with their features names.<br>
 * If the annotation type or a feature in the type do not exist in the CAS, it
 * throws an error.<br>
 * Span types starts with the prefix <b> #T_SP=</b>. <br>
 * Relation types starts with the prefix <b> #T_RL=</b>. <br>
 * Chain types starts with the prefix <b> #T_CH=</b>. <br>
 * Slot features start with prefix <b> ROLE_</b>. <br>
 * All features of a type follows the the name separated by <b>|</b> character.
 * <br>
 */
public class WebannoCustomTsv3Reader extends JCasResourceCollectionReader_ImplBase {

	private static final String TAB = "\t";
	private static final String LF = "\n";
	private static final String REF_REL = "referenceRelation";
	private static final String REF_LINK = "referenceType";
	private static final String CHAIN = "Chain";
	private static final String FIRST = "first";
	private static final String NEXT = "next";
	public static final String ROLE = "ROLE_";
	public static final String BT = "BT_"; // base type for the relation
											// annotation
	private static final String DEPENDENT = "Dependent";
	private static final String GOVERNOR = "Governor";

	private String fileName;
	private int columns = 2;// token number + token columns (minimum required)
	private Map<Type, Set<Feature>> allLayers = new LinkedHashMap<Type, Set<Feature>>();
	/*
	 * private Map<Type, Set<Feature>> relationLayers = new LinkedHashMap<Type,
	 * Set<Feature>>(); private Map<Type, Set<Feature>> chainLayers = new
	 * LinkedHashMap<Type, Set<Feature>>();
	 */ private Map<Feature, Type> roleLinks = new HashMap<>();
	private Map<Feature, Type> roleTargets = new HashMap<>();
	private Map<Feature, Type> slotLinkTypes = new HashMap<>();
	private StringBuilder coveredText = new StringBuilder();
	// for each type, for each unit, annotations per position
	private Map<Type, Map<AnnotationUnit, List<String>>> annotationsPerPostion = new LinkedHashMap<>();
	private Map<Type, Map<Integer, Map<Integer, AnnotationFS>>> chainAnnosPerTyep = new HashMap<>();
	private List<AnnotationUnit> units = new ArrayList<>();
	private Map<String, AnnotationUnit> token2Units = new HashMap<>();
	private Map<AnnotationUnit, Token> units2Tokens = new HashMap<>();

	private Map<Type, Feature> depFeatures = new HashMap<>();
	private Map<Type, Type> depTypess = new HashMap<>();

	// record the annotation at ref position when it is multiple token
	// annotation
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

			setAnnosPerTypePerUnit(lines, unit, ind);
		}

		Map<Type, Map<AnnotationUnit, List<AnnotationFS>>> annosPerTypePerUnit = new HashMap<>();
		setAnnosPerUnit(aJCas, annosPerTypePerUnit);
		addAnnotations(aJCas, annosPerTypePerUnit);
		addChainAnnotations(aJCas);
	}

	/**
	 * The individual link annotations are stored in a {@link TreeMap}
	 * (chainAnnosPerTye) with chain number and link number references, sorted
	 * in an ascending order <br>
	 * Iterate over each chain number and link number references and construct
	 * the chain
	 * 
	 * @param aJCas
	 */
	private void addChainAnnotations(JCas aJCas) {
		for (Type linkType : chainAnnosPerTyep.keySet()) {
			for (int chainNo : chainAnnosPerTyep.get(linkType).keySet()) {

				Type chainType = aJCas.getCas().getTypeSystem()
						.getType(linkType.getName().substring(0, linkType.getName().length() - 4) + CHAIN);
				Feature firstF = chainType.getFeatureByBaseName(FIRST);
				Feature nextF = linkType.getFeatureByBaseName(NEXT);
				FeatureStructure chain = aJCas.getCas().createFS(chainType);

				aJCas.addFsToIndexes(chain);
				AnnotationFS firstFs = chainAnnosPerTyep.get(linkType).get(chainNo).get(1);
				AnnotationFS linkFs = firstFs;
				chain.setFeatureValue(firstF, firstFs);
				for (int i = 2; i <= chainAnnosPerTyep.get(linkType).get(chainNo).size(); i++) {
					linkFs.setFeatureValue(nextF, chainAnnosPerTyep.get(linkType).get(chainNo).get(i));
					linkFs = chainAnnosPerTyep.get(linkType).get(chainNo).get(i);
				}
			}
		}
	}

	/**
	 * Importing span annotations including slot annotations
	 * 
	 * @param aJCas
	 * @param aAnnosPerTypePerUnit
	 */

	private void addAnnotations(JCas aJCas, Map<Type, Map<AnnotationUnit, List<AnnotationFS>>> aAnnosPerTypePerUnit) {

		for (Type type : annotationsPerPostion.keySet()) {
			Map<Integer, AnnotationFS> multiTokUnits = new HashMap<>();
			for (AnnotationUnit unit : annotationsPerPostion.get(type).keySet()) {

				int end = unit.end;
				List<AnnotationFS> annos = aAnnosPerTypePerUnit.get(type).get(unit);
				int j = 0;
				Map<AnnotationFS, List<FeatureStructure>> linkFSesPerAnno = new HashMap<>();
				boolean isSlot = false;
				Feature linkeF = null;
				for (Feature feat : allLayers.get(type)) {
					String anno = annotationsPerPostion.get(type).get(unit).get(j);
					if (!anno.equals("_")) {
						int i = 0;
						for (String mAnnos : anno.split("\\|\\|")) {
							// if it is a slot annotation (multiple slots per
							// single annotation
							// (Target1<--role1--Base--role2-->Target2)
							int slot = 0;
							for (String mAnno : mAnnos.split("\\|")) {
								int ref = 1;
								String depRef = "";
								if (mAnno.endsWith("]")) {
									depRef = mAnno.substring(mAnno.indexOf("[") + 1, mAnno.length() - 1);
									ref = depRef.contains("_") ? 1
											: Integer.valueOf(
													mAnno.substring(mAnno.indexOf("[") + 1, mAnno.length() - 1));
									mAnno = mAnno.substring(0, mAnno.indexOf("["));
								}
								if (mAnno.startsWith("B-")) {

									multiTokUnits.put(ref, annos.get(i));
									mAnno = mAnno.substring(2);
								}
								if (mAnno.startsWith("I-")) {

									Feature endF = type.getFeatureByBaseName(CAS.FEATURE_BASE_NAME_END);
									multiTokUnits.get(ref).setIntValue(endF, end);
									if (feat.getShortName().equals(REF_LINK)) {
										// since REF_REL do not start with BIO,
										// update it it...
										annos.set(i, multiTokUnits.get(ref));
									}
									setAnnoRefPerUnit(unit, type, ref, multiTokUnits.get(ref));

								} else {
									if (mAnno.equals(feat.getName()))
										mAnno = null;
									if (roleLinks.containsKey(feat)) {
										linkeF = feat;
										isSlot = true;
										FeatureStructure link = aJCas.getCas().createFS(slotLinkTypes.get(feat));
										Feature roleFeat = link.getType().getFeatureByBaseName("role");
										link.setStringValue(roleFeat, mAnno);
										linkFSesPerAnno.putIfAbsent(annos.get(i), new ArrayList<>());
										linkFSesPerAnno.get(annos.get(i)).add(link);

									} else if (roleTargets.containsKey(feat)) {

										FeatureStructure link = linkFSesPerAnno.get(annos.get(i)).get(slot);
										AnnotationUnit targetUnit = token2Units.get(mAnno);

										AnnotationFS targetFs = aAnnosPerTypePerUnit.get(roleTargets.get(feat))
												.get(targetUnit).get(ref - 1);
										link.setFeatureValue(feat, targetFs);
										slot++;

									} else if (feat.getShortName().equals(REF_REL)) {

										int chainNo = Integer.valueOf(mAnno.split("->")[1].split("-")[0]);
										int LinkNo = Integer.valueOf(mAnno.split("->")[1].split("-")[1]);
										chainAnnosPerTyep.putIfAbsent(type, new TreeMap<>());
										if (chainAnnosPerTyep.get(type).get(chainNo) != null
												&& chainAnnosPerTyep.get(type).get(chainNo).get(LinkNo) != null) {
											continue;
										}
										String refRel = mAnno.split("->")[0];
										annos.get(i).setFeatureValueFromString(feat, refRel);
										chainAnnosPerTyep.putIfAbsent(type, new TreeMap<>());
										chainAnnosPerTyep.get(type).putIfAbsent(chainNo, new TreeMap<>());
										chainAnnosPerTyep.get(type).get(chainNo).put(LinkNo, annos.get(i));

									} else if (feat.getShortName().equals(REF_LINK)) {

										annos.get(i).setFeatureValueFromString(feat, mAnno);
										aJCas.addFsToIndexes(annos.get(i));

									}

									else if (depFeatures.get(type) != null && depFeatures.get(type).equals(feat)) {

										int g = depRef.isEmpty() ? 1 : Integer.valueOf(depRef.split("_")[0]);
										int d = depRef.isEmpty() ? 1 : Integer.valueOf(depRef.split("_")[1]);
										Type depType = depTypess.get(type);
										AnnotationUnit govUnit = token2Units.get(mAnno);
										AnnotationFS govFs;
										AnnotationFS depFs;

										if (depType.getName().equals(POS.class.getName())) {
											depType = aJCas.getCas().getTypeSystem().getType(Token.class.getName());
											govFs = units2Tokens.get(govUnit);
											depFs = units2Tokens.get(unit);

										} else {
											govFs = aAnnosPerTypePerUnit.get(depType).get(govUnit).get(g - 1);
											depFs = aAnnosPerTypePerUnit.get(depType).get(unit).get(d - 1);
										}

										annos.get(i).setFeatureValue(feat, depFs);
										annos.get(i).setFeatureValue(type.getFeatureByBaseName(GOVERNOR), govFs);
										if (depFs.getBegin() <= annos.get(i).getBegin()) {
											Feature beginF = type.getFeatureByBaseName(CAS.FEATURE_BASE_NAME_BEGIN);
											annos.get(i).setIntValue(beginF, depFs.getBegin());
										} else {
											Feature endF = type.getFeatureByBaseName(CAS.FEATURE_BASE_NAME_END);
											annos.get(i).setIntValue(endF, depFs.getEnd());
										}

									} else {
										annos.get(i).setFeatureValueFromString(feat, mAnno);
										aJCas.addFsToIndexes(annos.get(i));
										setAnnoRefPerUnit(unit, type, ref, annos.get(i));
									}

								}
							}
							if (type.getName().equals(POS.class.getName())) {
								units2Tokens.get(unit).setPos((POS) annos.get(i));
							}
							i++;
						}
					}
					j++;
				}
				if (isSlot) {
					addSlotAnnotations(linkFSesPerAnno, linkeF);
					isSlot = false;
				}
			}
		}

	}

	/**
	 * update a base annotation with slot annotations
	 * 
	 * @param linkFSesPerAnno
	 *            contains list of slot annotations per a base annotation
	 * @param aLinkeF
	 *            The link slot annotation feature
	 */
	private void addSlotAnnotations(Map<AnnotationFS, List<FeatureStructure>> linkFSesPerAnno, Feature aLinkeF) {
		for (AnnotationFS anno : linkFSesPerAnno.keySet()) {
			ArrayFS array = anno.getCAS().createArrayFS(linkFSesPerAnno.get(anno).size());
			array.copyFromArray(
					linkFSesPerAnno.get(anno).toArray(new FeatureStructure[linkFSesPerAnno.get(anno).size()]), 0, 0,
					linkFSesPerAnno.get(anno).size());
			anno.setFeatureValue(aLinkeF, array);
		}
		linkFSesPerAnno = new HashMap<>();
	}

	/**
	 * Gets annotations from lines (of {@link AnnotationUnit}s) and save for the
	 * later access, while reading the document the first time. <br>
	 * 
	 * @param lines
	 *            TSV lines exported from WebAnno
	 * @param unit
	 *            the annotation unit (Token or sub-tokens)
	 * @param ind
	 *            index of the annotation, from the TAB separated annotations in
	 *            the TSV lines
	 */
	private void setAnnosPerTypePerUnit(String[] lines, AnnotationUnit unit, int ind) {
		for (Type type : allLayers.keySet()) {

			annotationsPerPostion.putIfAbsent(type, new LinkedHashMap<>());
			for (Feature f : allLayers.get(type)) {
				annotationsPerPostion.get(type).put(unit,
						annotationsPerPostion.get(type).getOrDefault(unit, new ArrayList<>()));
				annotationsPerPostion.get(type).get(unit).add(lines[ind]);
				ind++;
			}
		}
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

					if (anno.split("\\|\\|").length > multAnnos) {
						multAnnos = anno.split("\\|\\|").length;
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
			units2Tokens.put(unit, token);
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

	/**
	 * Get the type and feature information from the TSV file header
	 * 
	 * @param aJcas
	 * @param header
	 *            the header line
	 * @throws IOException
	 *             If the type or the feature do not exist in the CAs
	 */
	private void setLayerAndFeature(JCas aJcas, String header) throws IOException {
		try {
			StringTokenizer headerTk = new StringTokenizer(header, "#");
			while (headerTk.hasMoreTokens()) {
				String layerNames = headerTk.nextToken().trim();
				StringTokenizer layerTk = new StringTokenizer(layerNames, "|");

				Set<Feature> features = new LinkedHashSet<Feature>();
				String layerName = layerTk.nextToken().trim();
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
					Feature feature;

					if (ft.startsWith(BT)) {
						feature = layer.getFeatureByBaseName(DEPENDENT);
						depFeatures.put(layer, feature);
						depTypess.put(layer, CasUtil.getType(aJcas.getCas(), ft.substring(3)));
					} else {
						feature = layer.getFeatureByBaseName(ft);
					}
					if (ft.startsWith(ROLE)) {
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
				allLayers.put(layer, features);
			}
		} catch (Exception e) {
			throw new IOException(e.getMessage() + "\nTSV header:\n" + header);
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
