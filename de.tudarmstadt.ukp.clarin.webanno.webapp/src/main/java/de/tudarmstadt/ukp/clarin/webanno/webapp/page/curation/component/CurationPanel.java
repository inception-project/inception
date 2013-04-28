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
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.UIMAException;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.uimafit.util.CasUtil;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotatorModel;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.AnnotationTypeConstant;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.CasToBratJson;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.Entity;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.AnnotationOption;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.AnnotationSelection;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.CasDiff;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model.AnnotationState;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model.BratCurationDocumentEditor;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model.BratCurationVisualizer;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model.CurationContainer;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model.CurationSegment;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model.CurationUserSegment2;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model.SentenceState;
import de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceLink;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;

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

    public final static String CURATION_USER = "CURATION_USER";

    /**
     * Map for tracking curated spans. Key contains the address of the span,
     * the value contains the username from which the span has been selected
     */
    private Map<String, Map<Integer, AnnotationSelection>> annotationSelectionByUsernameAndAddress = new HashMap<String, Map<Integer,AnnotationSelection>>();

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
    	    		/**
    	    		 * Method is called, if user has clicked on a span for merge
    	    		 */
					@Override
					protected void onMerge(AjaxRequestTarget aTarget) {
		                final IRequestParameters request = getRequest().getPostParameters();

		                StringValue action = request.getParameterValue("action");
		                if (!action.isEmpty() && action.toString().equals("selectSpanForMerge")) {
		                	// add span for merge
		                	String username = curationUserSegment.getUsername();
		                    Integer address = request.getParameterValue("id").toInteger();
		                    AnnotationSelection annotationSelection = annotationSelectionByUsernameAndAddress.get(username).get(address);

		                    SourceDocument sourceDocument = curationContainer.getSourceDocument();
		                    Project project = curationContainer.getProject();
		                    AnnotationDocument clickedAnnotationDocument = null;
		                    List<AnnotationDocument> annotationDocuments = repository.listAnnotationDocument(project, sourceDocument);
		                    for (AnnotationDocument annotationDocument : annotationDocuments) {
								if(annotationDocument.getUser().getUsername().equals(username)) {
									clickedAnnotationDocument = annotationDocument;
								}
							}
							if(annotationSelection != null) {
								try {
									JCas mergeJCas = repository.getCurationDocumentContent(sourceDocument);

									// remove old annotation selections (if present)
									for (AnnotationSelection as : annotationSelection.getAnnotationOption().getAnnotationSelections()) {
										Integer addressRemove = as.getAddressByUsername().get(CURATION_USER);
										if(addressRemove != null) {
											FeatureStructure fsRemove = mergeJCas.getLowLevelCas().ll_getFSForRef(addressRemove);
											mergeJCas.removeFsFromIndexes(fsRemove);
										}
									}
									// add new annotation selection
									if(clickedAnnotationDocument != null) {
										JCas clickedJCas = repository.getAnnotationDocumentContent(clickedAnnotationDocument);
										CasCopier copier = new CasCopier(clickedJCas.getCas(), mergeJCas.getCas());
										FeatureStructure fsClicked = clickedJCas.getLowLevelCas().ll_getFSForRef(address);
										FeatureStructure fsCopy = copier.copyFs(fsClicked);
										mergeJCas.getCas().addFsToIndexes(fsCopy);
									}
									// persist jcas
									User userLoggedIn = repository.getUser(SecurityContextHolder.getContext().getAuthentication().getName());
									repository.createCurationDocumentContent(mergeJCas, sourceDocument, userLoggedIn);

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

		                    }

		                }

						System.out.println("On Merge");

						updateRightSide(aTarget, outer, curationContainer);
						aTarget.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");
					}
    	    	};
    	    	curationVisualizer.setOutputMarkupId(true);
    	    	item2.add(curationVisualizer);
    	    }
    	};
    	curationListView.setOutputMarkupId(true);

    	MergePanel mergePanel = new MergePanel("mergeView", "Click on a sentence to view differences...");
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
                        target.appendJavaScript("Wicket.Window.unloadConfirmation=false;window.location.reload()");
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
		Project project = curationContainer.getProject();
		List<AnnotationDocument> annotationDocuments = repository.listAnnotationDocument(project, sourceDocument);
		Map<String, JCas> jCases = new HashMap<String, JCas>();
		JCas mergeJCas = null;
		try {
			mergeJCas = repository.getCurationDocumentContent(sourceDocument);
		} catch (UIMAException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ClassNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		User userLoggedIn = repository.getUser(SecurityContextHolder.getContext().getAuthentication().getName());

		// get cases from repository
		for (AnnotationDocument annotationDocument : annotationDocuments) {
			String username = annotationDocument.getUser().getUsername();
			if(annotationDocument.getState().equals(AnnotationDocumentState.FINISHED) || username.equals(CURATION_USER)) {
				try {
					JCas jCas = repository.getAnnotationDocumentContent(annotationDocument);
					jCases.put(username, jCas);

					// cleanup annotationSelections
					annotationSelectionByUsernameAndAddress.put(username, new HashMap<Integer, AnnotationSelection>());
				} catch (Exception e) {
					LOG.error("Unable to load document ["+annotationDocument+"]", e);
					error("Unable to load document ["+annotationDocument+"]: "+ExceptionUtils.getRootCauseMessage(e));
				}
			}
		}
		// add mergeJCas separately
		jCases.put(CURATION_USER, mergeJCas);

		// create cas for merge panel
		int numUsers = jCases.size();

		// get differing feature structures
    	List<Type> entryTypes = new LinkedList<Type>();
    	//entryTypes.add(CasUtil.getType(mergeJCas.getCas(), Token.class));
		//entryTypes.add(CasUtil.getType(mergeJCas.getCas(), Sentence.class));
		entryTypes.add(CasUtil.getType(mergeJCas.getCas(), POS.class));
		entryTypes.add(CasUtil.getType(mergeJCas.getCas(), CoreferenceLink.class));
		entryTypes.add(CasUtil.getType(mergeJCas.getCas(), Lemma.class));
		entryTypes.add(CasUtil.getType(mergeJCas.getCas(), NamedEntity.class));
		// TODO arc types
    	List<AnnotationOption> annotationOptions = null;
		try {
			annotationOptions = CasDiff.doDiff(entryTypes, jCases, curationSegment.getBegin(), curationSegment.getEnd());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		// fill lookup variable for annotation selections
		for (AnnotationOption annotationOption : annotationOptions) {
			for (AnnotationSelection annotationSelection : annotationOption.getAnnotationSelections()) {
				for (String username : annotationSelection.getAddressByUsername().keySet()) {
					if (!username.equals(CURATION_USER)) {
						Integer address = annotationSelection.getAddressByUsername().get(username);
						annotationSelectionByUsernameAndAddress.get(username).put(address, annotationSelection);
					}
				}
			}
		}

		List<CurationUserSegment2> sentences = new LinkedList<CurationUserSegment2>();

		boolean hasDiff = false;
		for (String username : jCases.keySet()) {
			if(!username.equals(CURATION_USER)) {
				Map<Integer, AnnotationSelection> annotationSelectionByAddress = new HashMap<Integer, AnnotationSelection>();
				for (AnnotationOption annotationOption : annotationOptions) {
					for (AnnotationSelection annotationSelection : annotationOption.getAnnotationSelections()) {
						if(annotationSelection.getAddressByUsername().containsKey(username)) {
							Integer address = annotationSelection.getAddressByUsername().get(username);
							annotationSelectionByAddress.put(address, annotationSelection);
						}
					}
				}
				JCas jCas = jCases.get(username);

				GetDocumentResponse response = this.getDocumentResponse(jCas, username);

				CurationUserSegment2 curationUserSegment2 = new CurationUserSegment2();
				curationUserSegment2.setCollectionData(getStringCollectionData(response, jCas, annotationSelectionByAddress, username, numUsers));
				curationUserSegment2.setDocumentResponse(getStringDocumentResponse(response));
				curationUserSegment2.setUsername(username);

				sentences.add(curationUserSegment2);
			}
		}
		if(!hasDiff && curationSegment.getSentenceState().equals(SentenceState.DISAGREE)) {
			curationSegment.setSentenceState(SentenceState.RESOLVED);
			textListView.setModelObject(curationContainer.getCurationSegments());
		}

		// update sentence list on the right side
		sentences = new LinkedList<CurationUserSegment2>(sentences);
		curationListView.setModelObject(sentences);

		CurationUserSegment2 mergeUserSegment = new CurationUserSegment2();
		GetDocumentResponse response = getDocumentResponse(mergeJCas, CURATION_USER);
		//mergeUserSegment.setCollectionData(getStringCollectionData(response, mergeJCas, addresses, username));
		mergeUserSegment.setCollectionData("{}");
		mergeUserSegment.setDocumentResponse(getStringDocumentResponse(response));
		BratAnnotatorModel bratAnnotatorModel = new BratAnnotatorModel();
		bratAnnotatorModel.setDocument(sourceDocument);
		bratAnnotatorModel.setProject(sourceDocument.getProject());
		bratAnnotatorModel.setUser(userLoggedIn);
		bratAnnotatorModel.setFirstSentenceAddress(curationSegment.getSentenceAddress().get(CURATION_USER));
		bratAnnotatorModel.setLastSentenceAddress(curationSegment.getSentenceAddress().get(CURATION_USER));
		bratAnnotatorModel.setSentenceAddress(curationSegment.getSentenceAddress().get(CURATION_USER));
        bratAnnotatorModel.setAnnotationLayers(new HashSet<TagSet>(annotationService
                .listTagSets(bratAnnotatorModel.getProject())));
		BratCurationDocumentEditor mergeVisualizer = new BratCurationDocumentEditor("mergeView", new Model<BratAnnotatorModel>(bratAnnotatorModel));
		/*
		BratCurationVisualizer mergeVisualizer = new BratCurationVisualizer("mergeView", new Model<CurationUserSegment2>(mergeUserSegment)) {
			@Override
			protected void onMerge(AjaxRequestTarget aTarget) {
				// Do nothing if clicked on entry in the merge panel
			}
		};
         */

    	// send response to the client
//		parent.replace(curationListView);
		parent.replace(mergeVisualizer);
		target.add(parent);

	}

	private GetDocumentResponse getDocumentResponse(JCas jCas, String username) {
        GetDocumentResponse response = new GetDocumentResponse();
        response.setText(jCas.getDocumentText());

        List<String> tagSetNames = new ArrayList<String>();
        tagSetNames.add(AnnotationTypeConstant.POS);
        tagSetNames.add(AnnotationTypeConstant.DEPENDENCY);
        tagSetNames.add(AnnotationTypeConstant.NAMEDENTITY);
        tagSetNames.add(AnnotationTypeConstant.COREFERENCE);
        tagSetNames.add(AnnotationTypeConstant.COREFRELTYPE);

        CasToBratJson casToBratJson = new CasToBratJson();

        BratAnnotatorModel bratAnnotatorModel = new  BratAnnotatorModel();
        bratAnnotatorModel.setSentenceAddress(curationSegment.getSentenceAddress().get(username));
        bratAnnotatorModel.setLastSentenceAddress(curationSegment.getSentenceAddress().get(username));
        bratAnnotatorModel.setWindowSize(1);

        casToBratJson.addTokenToResponse(jCas, response, bratAnnotatorModel);
        casToBratJson.addSentenceToResponse(jCas, response, bratAnnotatorModel);
        SpanAdapter.getPosAdapter().addToBrat(jCas, response, bratAnnotatorModel);
        casToBratJson.addCorefTypeToResponse(jCas, response, bratAnnotatorModel);
        SpanAdapter.getLemmaAdapter().addToBrat(jCas, response, bratAnnotatorModel);
        SpanAdapter.getNamedEntityAdapter().addToBrat(jCas, response, bratAnnotatorModel);
        // TODO does not work yet
        //ArcAdapter.getDependencyAdapter().addToBrat(jCas, response, bratAnnotatorModel);
        casToBratJson.addCoreferenceToResponse(jCas, response, bratAnnotatorModel);

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
			JCas jCas,
			Map<Integer, AnnotationSelection> annotationSelectionByAddress,
			String username, int numUsers) {
		Map<String, Map<String, Object>> entityTypes = new HashMap<String, Map<String,Object>>();

        for (Entity entity : response.getEntities()) {
        	// check if either address of entity has no changes ...
        	// ... or if entity has already been clicked on
        	int address = entity.getId();
        	AnnotationSelection annotationSelection = annotationSelectionByAddress.get(address);
        	if(annotationSelection == null) {
        		String label = entity.getType();
        		String type = entity.getType()+"_(NOT_SUPPORTED)";
        		entity.setType(type);
        		entityTypes.put(type, getEntity(type, label, AnnotationState.NOT_SUPPORTED));
        	} else if(annotationSelection.getAddressByUsername().size() == numUsers) {
        		String label = entity.getType();
        		String type = entity.getType()+"_(AGREE)";
        		entity.setType(type);
        		entityTypes.put(type, getEntity(type, label, AnnotationState.AGREE));
        	} else if(annotationSelection.getAddressByUsername().containsKey(CURATION_USER)) {
    			entityTypes.put(entity.getType(), getEntity(entity.getType(), entity.getType(), AnnotationState.USE));
        		String label = entity.getType();
        		String type = entity.getType()+"_(USE)";
        		entity.setType(type);
        		entityTypes.put(type, getEntity(type, label, AnnotationState.USE));
        	} else {
        		boolean doNotUse = false;
        		for (AnnotationSelection otherAnnotationSelection : annotationSelection.getAnnotationOption().getAnnotationSelections()) {
					if(otherAnnotationSelection.getAddressByUsername().containsKey(CURATION_USER)) {
						doNotUse = true;
						break;
					}
				}
        		if(doNotUse) {
        			entityTypes.put(entity.getType(), getEntity(entity.getType(), entity.getType(), AnnotationState.DO_NOT_USE));
        			String label = entity.getType();
        			String type = entity.getType()+"_(DO_NOT_USE)";
        			entity.setType(type);
        			entityTypes.put(type, getEntity(type, label, AnnotationState.DO_NOT_USE));
        		} else {
        			entityTypes.put(entity.getType(), getEntity(entity.getType(), entity.getType(), AnnotationState.DISAGREE));
        		}
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
