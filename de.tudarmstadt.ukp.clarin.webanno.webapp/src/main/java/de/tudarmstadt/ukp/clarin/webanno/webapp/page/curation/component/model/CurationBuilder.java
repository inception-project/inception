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
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model;

import static org.uimafit.util.JCasUtil.select;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.jcas.JCas;
import org.springframework.security.core.context.SecurityContextHolder;
import org.uimafit.util.CasUtil;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.AnnotationOption;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.AnnotationSelection;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.CasDiff;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.CurationPanel;
import de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceLink;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class CurationBuilder {

	private RepositoryService repository;

	private final static Log LOG = LogFactory.getLog(CurationPanel.class);

	public CurationBuilder(RepositoryService repository) {
		this.repository = repository;
	}

	public CurationContainer buildCurationContainer(Project aProject, SourceDocument sourceDocument) {
		CurationContainer curationContainer = new CurationContainer();
			// initialize Variables
			Map<Integer, Integer> segmentBeginEnd = new HashMap<Integer, Integer>();
			Map<Integer, Integer> segmentNumber = new HashMap<Integer, Integer>();
			Map<Integer, String> segmentText = new HashMap<Integer, String>();
			Map<String, Map<Integer, Integer>> segmentAdress = new HashMap<String, Map<Integer, Integer>>();
			// get annotation documents
			List<AnnotationDocument> annotationDocuments = repository
					.listAnnotationDocument(aProject, sourceDocument);

			Map<String, JCas> jCases = new HashMap<String, JCas>();
			AnnotationDocument randomAnnotationDocument = null;
			for (AnnotationDocument annotationDocument : annotationDocuments) {
				String username = annotationDocument.getUser().getUsername();
				if(annotationDocument.getState().equals(AnnotationDocumentState.FINISHED)) {
					try {
						JCas jCas = repository.getAnnotationDocumentContent(annotationDocument);

						if(randomAnnotationDocument == null) {
							randomAnnotationDocument = annotationDocument;
						}

						int sentenceNumber = 0;
						segmentAdress.put(username, new HashMap<Integer, Integer>());
						for (Sentence sentence : select(jCas, Sentence.class)) {
							sentenceNumber += 1;
							segmentBeginEnd.put(sentence.getBegin(), sentence.getEnd());
							segmentText.put(sentence.getBegin(), sentence.getCoveredText().toString());
							segmentNumber.put(sentence.getBegin(), sentenceNumber);
							segmentAdress.get(username).put(sentence.getBegin(), sentence.getAddress());
						}

						jCases.put(username, jCas);
					} catch (Exception e) {
						LOG.info("Skipping document due to exception ["+annotationDocument+"]", e);
					}
				}
			}

			// TODO Create pre-merged jcas if not exists for curation user

			JCas mergeJCas = null;
			try {
				mergeJCas = repository.getCurationDocumentContent(sourceDocument);
			} catch (Exception e) {
				// JCas does not exist
			}

			int numUsers = jCases.size();

			List<Type> entryTypes = null;
			// Create jcas, if it could not be loaded from the file system
			if (mergeJCas == null) {
				try {
					mergeJCas = repository.getAnnotationDocumentContent(randomAnnotationDocument);
				} catch (UIMAException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				entryTypes = getEntryTypes(mergeJCas);
				jCases.put(CurationPanel.CURATION_USER, mergeJCas);

		    	List<AnnotationOption> annotationOptions = null;
				try {
					int begin = 0;
					int end = mergeJCas.getDocumentText().length();
					annotationOptions = CasDiff.doDiff(entryTypes, jCases, begin, end);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				for (AnnotationOption annotationOption : annotationOptions) {
					// remove the featureStructure if more than 1 annotationSelection exists per annotationOption
					boolean removeFS = annotationOption.getAnnotationSelections().size() > 1;
					if(annotationOption.getAnnotationSelections().size() == 1) {
						removeFS = annotationOption.getAnnotationSelections().get(0).getAddressByUsername().size() <= numUsers;
					}
					for (AnnotationSelection annotationSelection : annotationOption.getAnnotationSelections()) {
						for (String username : annotationSelection.getAddressByUsername().keySet()) {
							if(username.equals(CurationPanel.CURATION_USER)) {
								Integer address = annotationSelection.getAddressByUsername().get(username);

								// removing disagreeing feature structures in mergeJCas
								if(removeFS && address != null) {
									FeatureStructure fs = mergeJCas.getLowLevelCas().ll_getFSForRef(address);
									if(!(fs instanceof Token)) {
										mergeJCas.getCas().removeFsFromIndexes(fs);
									}
								}
							}
						}
					}
				}

				User userLoggedIn = repository.getUser(SecurityContextHolder.getContext().getAuthentication().getName());
				try {
					repository.createCurationDocumentContent(mergeJCas, sourceDocument, userLoggedIn);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}

			segmentAdress.put(CurationPanel.CURATION_USER, new HashMap<Integer, Integer>());
			for (Sentence sentence : select(mergeJCas, Sentence.class)) {
				segmentAdress.get(CurationPanel.CURATION_USER).put(sentence.getBegin(), sentence.getAddress());
			}

	    	if(entryTypes == null) {
	    		entryTypes = getEntryTypes(mergeJCas);
	    	}

			for (Integer begin : segmentBeginEnd.keySet()) {
				Integer end = segmentBeginEnd.get(begin);

		    	List<AnnotationOption> annotationOptions = null;
				try {
					annotationOptions = CasDiff.doDiff(entryTypes, jCases, begin, end);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				Boolean hasDiff = false;
				for (AnnotationOption annotationOption : annotationOptions) {
					List<AnnotationSelection> annotationSelections = annotationOption.getAnnotationSelections();
					if (annotationSelections.size() > 1) {
						hasDiff = true;
					} else if(annotationSelections.size() == 1) {
						AnnotationSelection annotationSelection = annotationSelections.get(0);
						if(annotationSelection.getAddressByUsername().size() < numUsers) {
							hasDiff = true;
						}
					}
				}

				CurationSegment curationSegment = new CurationSegment();
				curationSegment.setBegin(begin);
				curationSegment.setEnd(end);
				if(hasDiff) {
					curationSegment.setSentenceState(SentenceState.DISAGREE);
				} else {
					curationSegment.setSentenceState(SentenceState.AGREE);
				}
				curationSegment.setText(segmentText.get(begin));
				curationSegment.setSentenceNumber(segmentNumber.get(begin));

				for (String username : segmentAdress.keySet()) {
					curationSegment.getSentenceAddress().put(username, segmentAdress.get(username).get(begin));
				}
				curationContainer.getCurationSegmentByBegin().put(begin, curationSegment);
			}

		return curationContainer;
	}

	private List<Type> getEntryTypes(JCas mergeJCas) {
		List<Type> entryTypes = new LinkedList<Type>();
		//entryTypes.add(CasUtil.getType(firstJCas.getCas(), Token.class));
		//entryTypes.add(CasUtil.getType(firstJCas.getCas(), Sentence.class));
		entryTypes.add(CasUtil.getType(mergeJCas.getCas(), POS.class));
		entryTypes.add(CasUtil.getType(mergeJCas.getCas(), CoreferenceLink.class));
		entryTypes.add(CasUtil.getType(mergeJCas.getCas(), Lemma.class));
		entryTypes.add(CasUtil.getType(mergeJCas.getCas(), NamedEntity.class));
		// TODO arc types
		return entryTypes;
	}

}
