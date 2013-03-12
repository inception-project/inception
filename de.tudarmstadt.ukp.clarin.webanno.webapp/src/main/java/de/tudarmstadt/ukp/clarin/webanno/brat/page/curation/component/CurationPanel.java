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
package de.tudarmstadt.ukp.clarin.webanno.brat.page.curation.component;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.CasCopier;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.codehaus.jackson.JsonGenerator;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
import org.uimafit.util.CasUtil;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.AnnotationType;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.CasToBratJson;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.Entity;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.page.curation.CasDiff;
import de.tudarmstadt.ukp.clarin.webanno.brat.page.curation.component.model.AnnotationState;
import de.tudarmstadt.ukp.clarin.webanno.brat.page.curation.component.model.BratCurationVisualizer;
import de.tudarmstadt.ukp.clarin.webanno.brat.page.curation.component.model.CurationContainer;
import de.tudarmstadt.ukp.clarin.webanno.brat.page.curation.component.model.CurationSegment;
import de.tudarmstadt.ukp.clarin.webanno.brat.page.curation.component.model.CurationUserSegment2;
import de.tudarmstadt.ukp.clarin.webanno.brat.page.curation.component.model.SentenceState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

/**
 *
 * @author Andreas Straninger
 * Main Panel for the curation page. It displays a box with the
 * complete text on the left side and a box for a selected sentence
 * on the right side.
 *
 */
public class CurationPanel extends Panel {

	private final static Log LOG = LogFactory.getLog(CurationPanel.class);

    @SpringBean(name = "jsonConverter")
    private MappingJacksonHttpMessageConverter jsonConverter;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

    @SpringBean(name = "annotationService")
    private AnnotationService annotationService;

    /**
     * Map for tracking curated spans. Key contains the address of the span,
     * the value contains the username from which the span has been selected
     */
    private Map<Integer, String> changes = new HashMap<Integer, String>();

    private ListView<CurationUserSegment2> curationListView;

    private CurationSegment curationSegment;

    ListView<CurationSegment> textListView;

    /**
	 * Class for combining an on click ajax call and a label
	 */
	class AjaxLabel extends Label {

		private AbstractAjaxBehavior click;

		public AjaxLabel(String id, String label, AbstractAjaxBehavior click) {
			super(id, label);
			this.click = click;
		}

		@Override
		public void onComponentTag(ComponentTag tag) {
			// add onclick handler to the browser
			// if clicked in the browser, the function
			//     click.response(AjaxRequestTarget target) is called on the server side
			tag.put("onclick", "wicketAjaxGet('"+click.getCallbackUrl()+"')");
		}


	}

	public CurationPanel(String id, final CurationContainer curationContainer) {
		super(id);

		// add container for updating ajax
		final WebMarkupContainer outer = new WebMarkupContainer("outer");
		outer.setOutputMarkupId(true);
		add(outer);

		LinkedList<CurationUserSegment2> sentences = new LinkedList<CurationUserSegment2>();
		// update list of brat embeddings
    	curationListView = new ListView<CurationUserSegment2>("sentenceListView", sentences) {
    	    @Override
            protected void populateItem(ListItem<CurationUserSegment2> item2) {
    	    	final CurationUserSegment2 curationUserSegment = item2.getModelObject();
    	    	BratCurationVisualizer curationVisualizer = new BratCurationVisualizer("sentence", new Model<CurationUserSegment2>(curationUserSegment)) {
					@Override
					protected void onMerge(AjaxRequestTarget aTarget) {
		                final IRequestParameters request = getRequest().getPostParameters();

		                StringValue action = request.getParameterValue("action");
		                if (!action.isEmpty() && action.toString().equals("selectSpanForMerge")) {
		                    Integer address = request.getParameterValue("id").toInteger();
		                    String username = curationUserSegment.getUsername();
		                    changes.put(address, username);
		                }

		                System.out.println(changes);
						System.out.println("On Merge");

						updateRightSide(aTarget, outer, curationContainer);
					}
    	    	};
    	    	curationVisualizer.setOutputMarkupId(true);
    	    	item2.add(curationVisualizer);
    	    }
    	};
    	curationListView.setOutputMarkupId(true);

    	MergePanel mergePanel = new MergePanel("mergeView", "Merge View Dummy...");
    	outer.add(mergePanel);

    	outer.add(curationListView);

		// List view for the complete text on the left side. Each item is a sentence of the text
		textListView = new ListView<CurationSegment>("textListView", curationContainer.getCurationSegments()) {
			@Override
            protected void populateItem(ListItem<CurationSegment> item) {
			    final CurationSegment curationSegmentItem = item.getModelObject();

				// ajax call when clicking on a sentence on the left side
				final AbstractDefaultAjaxBehavior click = new AbstractDefaultAjaxBehavior() {

					@Override
					protected void respond(AjaxRequestTarget target) {
						curationSegment = curationSegmentItem;
						updateRightSide(target, getParent(), curationContainer);
					}

				};

				// add subcomponents to the component
				item.add(click);
				String colorCode = curationSegmentItem.getSentenceState().getColorCode();
				if(colorCode != null) {
					item.add(new SimpleAttributeModifier("style", "background-color:"+colorCode+";"));
				}
				Label currentSentence = new AjaxLabel("sentence",curationSegmentItem.getText(), click);
				item.add(currentSentence);
			}

		};
		// add subcomponents to the component
		textListView.setOutputMarkupId(true);
		outer.add(textListView);

	}

	protected void updateRightSide(AjaxRequestTarget target, MarkupContainer parent, CurationContainer curationContainer) {
		SourceDocument sourceDocument = curationContainer.getSourceDocument();
		List<AnnotationDocument> annotationDocuments = repository.listAnnotationDocument(sourceDocument);
		Map<String, CAS> casMap = new HashMap<String, CAS>();
		Map<String, JCas> jCases = new HashMap<String, JCas>();
		AnnotationDocument firstAnnotationDocument = null;

		// get cases from repository
		for (AnnotationDocument annotationDocument : annotationDocuments) {
			try {
				JCas jCas = repository.getAnnotationDocumentContent(annotationDocument);
				String username = annotationDocument.getUser().getUsername();
				jCases.put(username, jCas);
				casMap.put(username, jCas.getCas());
				firstAnnotationDocument = annotationDocument;
			} catch (Exception e) {
				LOG.error("Unable to load document ["+annotationDocument+"]", e);
				error("Unable to load document ["+annotationDocument+"]: "+ExceptionUtils.getRootCauseMessage(e));
			}
		}

		// create cas for merge panel
		JCas mergeJCas = null;
		try {
			mergeJCas = repository.getAnnotationDocumentContent(firstAnnotationDocument);
		} catch (Exception e) {
			LOG.error("Unable to load document ["+firstAnnotationDocument+"]", e);
			error("Unable to load document ["+firstAnnotationDocument+"]: "+ExceptionUtils.getRootCauseMessage(e));
		}
		casMap.put("", mergeJCas.getCas());

		// get differing feature structures
    	List<Type> entryTypes = new LinkedList<Type>();
    	entryTypes.add(CasUtil.getType(mergeJCas.getCas(), Token.class));
    	entryTypes.add(CasUtil.getType(mergeJCas.getCas(), NamedEntity.class));
    	Map<String, Set<FeatureStructure>> diffResult = null;
		try {
			diffResult = CasDiff.doDiff(entryTypes, casMap, curationSegment.getBegin(), curationSegment.getEnd());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		List<CurationUserSegment2> sentences = new LinkedList<CurationUserSegment2>();
		Set<Integer> addressesDisagree = new HashSet<Integer>();
		Set<Integer> addressesUse = new HashSet<Integer>();
		Set<Integer> addressesDoNotUse = new HashSet<Integer>();
		Map<String, Map<Integer, FeatureStructure>> differingFsByUsernameAndAddress = new HashMap<String, Map<Integer,FeatureStructure>>();
		boolean hasDiff = false;
		for (String username : jCases.keySet()) {
			differingFsByUsernameAndAddress.put(username, new HashMap<Integer, FeatureStructure>());
			JCas jCas = jCases.get(username);

			GetDocumentResponse response = this.getDocumentResponse(jCas);

			for(FeatureStructure fs : diffResult.get(username)) {
				Integer address = jCas.getLowLevelCas().ll_getFSRef(fs);
				if (!(fs instanceof Token)) {
					if(changes.containsKey(address)) {
						if(changes.get(address).equals(username)) {
							addressesUse.add(address);
						} else {
							addressesDoNotUse.add(address);
						}
					} else {
						addressesDisagree.add(address);
						hasDiff = true;
					}
				}
				differingFsByUsernameAndAddress.get(username).put(address, fs);
			}

			CurationUserSegment2 curationUserSegment2 = new CurationUserSegment2();
			curationUserSegment2.setCollectionData(getStringCollectionData(response, jCas, addressesDisagree, addressesUse, addressesDoNotUse, username));
			curationUserSegment2.setDocumentResponse(getStringDocumentResponse(response));
			curationUserSegment2.setUsername(username);

			sentences.add(curationUserSegment2);
		}
		if(!hasDiff && curationSegment.getSentenceState().equals(SentenceState.DISAGREE)) {
			curationSegment.setSentenceState(SentenceState.RESOLVED);
			textListView.setModelObject(curationContainer.getCurationSegments());
		}

		for (FeatureStructure fs : diffResult.get("")) {
        	int address = mergeJCas.getLowLevelCas().ll_getFSRef(fs);
        	// do not remove differing token because it may contain annotations, where all anotators agree
			if(!(fs instanceof Token)) {
				mergeJCas.getCas().removeFsFromIndexes(fs);
			}
			if(changes.containsKey(address)) {
				// copy token
				String username = changes.get(address);
				CAS sourceCas = casMap.get(username);
				CasCopier copier = new CasCopier(sourceCas, mergeJCas.getCas());
				FeatureStructure fsCopy = copier.copyFs(differingFsByUsernameAndAddress.get(username).get(address));
				mergeJCas.getCas().addFsToIndexes(fsCopy);
			}
		}

		// update sentence list on the right side
		sentences = new LinkedList<CurationUserSegment2>(sentences);
		curationListView.setModelObject(sentences);

		CurationUserSegment2 mergeUserSegment = new CurationUserSegment2();
		GetDocumentResponse response = getDocumentResponse(mergeJCas);
		//mergeUserSegment.setCollectionData(getStringCollectionData(response, mergeJCas, addresses, username));
		mergeUserSegment.setCollectionData("{}");
		mergeUserSegment.setDocumentResponse(getStringDocumentResponse(response));
		BratCurationVisualizer mergeVisualizer = new BratCurationVisualizer("mergeView", new Model<CurationUserSegment2>(mergeUserSegment)) {
			@Override
			protected void onMerge(AjaxRequestTarget aTarget) {
				// Do nothing if clicked on entry in the merge panel
			}
		};

    	// send response to the client
		parent.replace(curationListView);
		parent.replace(mergeVisualizer);
		target.add(parent);

	}

	private GetDocumentResponse getDocumentResponse(JCas jCas) {
        GetDocumentResponse response = new GetDocumentResponse();
        response.setText(jCas.getDocumentText());

        List<String> tagSetNames = new ArrayList<String>();
        tagSetNames.add(AnnotationType.POS);
        tagSetNames.add(AnnotationType.DEPENDENCY);
        tagSetNames.add(AnnotationType.NAMEDENTITY);
        tagSetNames.add(AnnotationType.COREFERENCE);
        tagSetNames.add(AnnotationType.COREFRELTYPE);

        CasToBratJson casToBratJson = new CasToBratJson(
                curationSegment.getSentenceAddress(),
                curationSegment.getSentenceAddress(),1, tagSetNames);

        casToBratJson.addTokenToResponse(jCas, response);
        casToBratJson.addSentenceToResponse(jCas, response);
        casToBratJson.addPosToResponse(jCas, response);
        casToBratJson.addCorefTypeToResponse(jCas, response);
        casToBratJson.addLemmaToResponse(jCas, response);
        casToBratJson.addNamedEntityToResponse(jCas, response);
        casToBratJson.addDependencyParsingToResponse(jCas, response);
        casToBratJson.addCoreferenceToResponse(jCas, response);

        /*
        try {
			AnalysisEngine anlysisEngine = AnalysisEngineFactory.createPrimitive(CASDumpWriter.class, CASDumpWriter.PARAM_OUTPUT_FILE, "-");
			anlysisEngine.process(jCas.getCas());
		} catch (ResourceInitializationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AnalysisEngineProcessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/

        return response;
	}

	private String getStringDocumentResponse(GetDocumentResponse aResponse) {
        String docData = "{}";
        // Serialize BRAT object model to JSON
        try {
            StringWriter out = new StringWriter();
            JsonGenerator jsonGenerator = jsonConverter.getObjectMapper().getJsonFactory()
                    .createJsonGenerator(out);
            jsonGenerator.writeObject(aResponse);
            docData = out.toString();
        }
        catch (IOException e) {
            error(ExceptionUtils.getRootCauseMessage(e));
        }

		return docData;
	}

	private String getStringCollectionData(GetDocumentResponse response,
			JCas jCas, Set<Integer> addressesDisagree, Set<Integer> addressesUse, Set<Integer> addressesDoNotUse, String username) {
		Map<String, Map<String, Object>> entityTypes = new HashMap<String, Map<String,Object>>();

        for (Entity entity : response.getEntities()) {
        	// check if either address of entity has no changes ...
        	// ... or if entity has already been clicked on
        	if(addressesDisagree.contains(entity.getId())) {
        		entityTypes.put(entity.getType(), getEntity(entity.getType(), entity.getType(), AnnotationState.DISAGREE));
        	} else if(addressesUse.contains(entity.getId())) {
        			entityTypes.put(entity.getType(), getEntity(entity.getType(), entity.getType(), AnnotationState.USE));
            		String label = entity.getType();
            		String type = entity.getType()+"_(USE)";
            		entity.setType(type);
            		entityTypes.put(type, getEntity(type, label, AnnotationState.USE));
        	} else if(addressesDoNotUse.contains(entity.getId())) {
        		entityTypes.put(entity.getType(), getEntity(entity.getType(), entity.getType(), AnnotationState.DO_NOT_USE));
        		String label = entity.getType();
        		String type = entity.getType()+"_(DO_NOT_USE)";
        		entity.setType(type);
        		entityTypes.put(type, getEntity(type, label, AnnotationState.DO_NOT_USE));
        	} else {
        		String label = entity.getType();
        		String type = entity.getType()+"_(AGREE)";
        		entity.setType(type);
        		entityTypes.put(type, getEntity(type, label, AnnotationState.AGREE));
        	}
		}

        Map<Object, Object> collection = new HashMap<Object, Object>();
		collection.put("entity_types", entityTypes.values());

        String collData = "{}";
        try {
        	StringWriter out = new StringWriter();
        	JsonGenerator jsonGenerator = jsonConverter.getObjectMapper().getJsonFactory()
        			.createJsonGenerator(out);
        	jsonGenerator.writeObject(collection);
        	collData = out.toString();
        }
        catch (IOException e) {
        	error(ExceptionUtils.getRootCauseMessage(e));
        }
        return collData;
	}

	private Map<String, Object> getEntity(String type, String label, AnnotationState annotationState) {
		Map<String, Object> entityType = new HashMap<String, Object>();
		entityType.put("type", type);
		entityType.put("labels", new String[]{type, label});
		String color = annotationState.getColorCode();
		entityType.put("bgColor", color);
		entityType.put("borderColor", "darken");
		return entityType;
	}

}
