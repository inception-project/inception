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
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component;

import java.io.IOException;
import java.util.LinkedList;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValue;
import org.springframework.security.core.context.SecurityContextHolder;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratAnnotator;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.BratCuratorUtility;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model.BratCurationVisualizer;
import de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model.CurationUserSegmentForAnnotationDocument;

/**
 * @author Seid Muhie Yimam
 * @author Richard Eckart de Castilho
 *
 */
public class CurationSegmentPanel extends WebMarkupContainer {
	private static final long serialVersionUID = 8736268179612831795L;
	private ListView<CurationUserSegmentForAnnotationDocument> sentenceListView;
	@SpringBean(name = "documentRepository")
	private RepositoryService repository;

	@SpringBean(name = "annotationService")
	private AnnotationService annotationService;

	/**
	 * Data models for {@link BratAnnotator}
	 */
	public void setModel(
			IModel<LinkedList<CurationUserSegmentForAnnotationDocument>> aModel) {
		setDefaultModel(aModel);
	}

	public void setModelObject(
			LinkedList<CurationUserSegmentForAnnotationDocument> aModel) {
		setDefaultModelObject(aModel);
	}

	@SuppressWarnings("unchecked")
	public IModel<LinkedList<CurationUserSegmentForAnnotationDocument>> getModel() {
		return (IModel<LinkedList<CurationUserSegmentForAnnotationDocument>>) getDefaultModel();
	}

	@SuppressWarnings("unchecked")
	public LinkedList<CurationUserSegmentForAnnotationDocument> getModelObject() {
		return (LinkedList<CurationUserSegmentForAnnotationDocument>) getDefaultModelObject();
	}

	public CurationSegmentPanel(String id,
			IModel<LinkedList<CurationUserSegmentForAnnotationDocument>> aModel) {
		super(id, aModel);
		// update list of brat embeddings
		sentenceListView = new ListView<CurationUserSegmentForAnnotationDocument>(
				"sentenceListView", aModel) {
			private static final long serialVersionUID = -5389636445364196097L;

			@Override
			protected void populateItem(
					ListItem<CurationUserSegmentForAnnotationDocument> item2) {
				final CurationUserSegmentForAnnotationDocument curationUserSegment = item2
						.getModelObject();
				BratCurationVisualizer curationVisualizer = new BratCurationVisualizer(
						"sentence",
						new Model<CurationUserSegmentForAnnotationDocument>(
								curationUserSegment)) {
					private static final long serialVersionUID = -1205541428144070566L;

					/**
					 * Method is called, if user has clicked on a span or an arc
					 * in the sentence panel. The span or arc respectively is
					 * identified and copied to the merge cas.
					 */
					@Override
					protected void onSelectAnnotationForMerge(
							AjaxRequestTarget aTarget) {
						final IRequestParameters request = getRequest()
								.getPostParameters();
						String username = SecurityContextHolder.getContext()
								.getAuthentication().getName();

						User user = repository.getUser(username);

						SourceDocument sourceDocument = curationUserSegment
								.getBratAnnotatorModel().getDocument();
						JCas annotationJCas = null;
						try {
							annotationJCas = curationUserSegment
									.getBratAnnotatorModel().getMode()
									.equals(Mode.CORRECTION) ? repository
									.getAnnotationDocumentContent(repository.getAnnotationDocument(sourceDocument, user))
									: repository
											.getCurationDocumentContent(sourceDocument);
						} catch (UIMAException e1) {
							error(ExceptionUtils.getRootCause(e1));
						} catch (IOException e1) {
							error(ExceptionUtils.getRootCause(e1));
						} catch (ClassNotFoundException e1) {
							error(ExceptionUtils.getRootCause(e1));
						}
						StringValue action = request
								.getParameterValue("action");
						// check if clicked on a span
						if (!action.isEmpty()
								&& action.toString().equals(
										"selectSpanForMerge")) {
							BratCuratorUtility.mergeSpan(request,
									curationUserSegment, annotationJCas,
									repository, annotationService);

						}
						// check if clicked on an arc
						else if (!action.isEmpty()
								&& action.toString()
										.equals("selectArcForMerge")) {
							// add span for merge
							// get information of the span clicked
							BratCuratorUtility.mergeArc(request,
									curationUserSegment, annotationJCas,
									repository, annotationService);
						}
						onChange(aTarget);
					}
				};
				curationVisualizer.setOutputMarkupId(true);
				item2.add(curationVisualizer);
			}
		};
		sentenceListView.setOutputMarkupId(true);
		add(sentenceListView);
	}

	protected void onChange(AjaxRequestTarget aTarget) {
		// Overriden in curationPanel
	}

	protected void isCorrection(AjaxRequestTarget aTarget) {
		// Overriden in curationPanel
	}
}
