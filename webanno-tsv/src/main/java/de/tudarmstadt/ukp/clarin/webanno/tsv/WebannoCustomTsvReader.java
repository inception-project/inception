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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang.StringUtils;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.jcas.JCas;
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
	private Map<Feature, Type> roleTargets = new HashMap<>();
	private Map<Type, Set<Feature>> chainLayers = new LinkedHashMap<Type, Set<Feature>>();
	private StringBuilder coveredText = new StringBuilder();
	// for each type, for each unit, annotations per position

	private Map<Type, Map<String, Map<Feature, String>>> annotaionsPerUnits = new LinkedHashMap<>();

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
				String text = line.substring(6);
				String beginEnd = text.substring(0, text.indexOf("#"));
				text = text.substring(text.indexOf("#") + 1);

				int begin = Integer.parseInt(beginEnd.split("-")[0]);
				int end = Integer.parseInt(beginEnd.split("-")[1]);

				coveredText.append(text + LF);
				Sentence sentence = new Sentence(aJCas, begin, end);
				sentence.addToIndexes();
				continue;
			}
			if (line.trim().isEmpty()) {
				continue;
			}
			// some times, the sentence in #text= might have a new line which
			// break this reader,
			// so skip such lines
			if (!Character.isDigit(line.split(" ")[0].charAt(0))) {
				continue;
			}

			// If we are still unlucky, the line starts with a number from the
			// sentence but not
			// a token number, check if it didn't in the format NUM-NUM
			if (!Character.isDigit(line.split("-")[1].charAt(0))) {
				continue;
			}

			int count = StringUtils.countMatches(line, "\t");

			if (columns != count) {
				throw new IOException(fileName + " This is not a valid TSV File. check this line: " + line);
			}
			String[] lines = line.split(TAB);
			int begin = Integer.parseInt(lines[1].split("-")[0]);
			int end = Integer.parseInt(lines[1].split("-")[1]);

			int ind = 3;
			for (Type type : spanLayers.keySet()) {
				AnnotationFS newAnnotation = aJCas.getCas().createAnnotation(type, begin, end);
				for (Feature feat : spanLayers.get(type)) {
					if (!lines[ind].equals("_")) {					
						newAnnotation.setFeatureValueFromString(feat, lines[ind]);
						aJCas.addFsToIndexes(newAnnotation);					
					}
					ind++;
				}
				/*
				 * annotaionsPerUnits.putIfAbsent(type, new HashMap<>());
				 * 
				 * annotaionsPerUnits.get(type).putIfAbsent(lines[0], new
				 * HashMap<>()); for() //
				 * annotaionsPerUnits.get(type.getName()).get(lines[0]).put(key,
				 * value)
				 */ }
		}
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
				if (ft.startsWith("ROLE_")) {
					ft = ft.substring(5);
					String t = layerTk.nextToken().toString();
					columns++;
					Type tType = CasUtil.getType(aJcas.getCas(), t.substring(5));
					roleTargets.put(layer.getFeatureByBaseName(ft), tType);
				}
				Feature feature = layer.getFeatureByBaseName(ft);
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
