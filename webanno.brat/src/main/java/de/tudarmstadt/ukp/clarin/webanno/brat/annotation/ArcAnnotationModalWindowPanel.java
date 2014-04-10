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
package de.tudarmstadt.ukp.clarin.webanno.brat.annotation;

import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil.selectByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeUtil.getAdapter;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;

import com.googlecode.wicket.jquery.ui.kendo.combobox.ComboBox;
import com.googlecode.wicket.jquery.ui.kendo.combobox.ComboBoxRenderer;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.ArcAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.ChainAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

/**
 * A panel that is used to display an annotation modal dialog for arc
 * annotation.
 * 
 * @author Seid Muhie Yimam
 * 
 */
public class ArcAnnotationModalWindowPanel extends Panel {
	private static final long serialVersionUID = -2102136855109258306L;

	@SpringBean(name = "documentRepository")
	private RepositoryService repository;

	@SpringBean(name = "annotationService")
	private AnnotationService annotationService;

	@SpringBean(name = "jsonConverter")
	private MappingJacksonHttpMessageConverter jsonConverter;

	// A flag to keep checking if new annotation is to be made or an existing
	// annotation is double
	// clciked.
	private boolean isModify = false;

	// currently, we have one directional chain annotation and the "reveres"
	// button not needed
	private boolean ischain = false;
	// The selected Layer
	private AnnotationLayer selectedLayer;

	// The selected feature
	private AnnotationFeature selectedFeature;

	private Model<Tag> tagsModel;
	private Model<AnnotationLayer> layersModel;
	private Model<AnnotationFeature> featuresModel;

	private ComboBox<Tag> tags;

	private final AnnotationDialogForm annotationDialogForm;
	private final BratAnnotatorModel bratAnnotatorModel;

	private int selectedArcId = -1;
	private int originSpanId, targetSpanId;
	private String selectedArcType;

	List<AnnotationFeature> spanFeatures = new ArrayList<AnnotationFeature>();

	private class AnnotationDialogForm extends Form<SelectionModel> {
		private static final long serialVersionUID = -4104665452144589457L;

		public AnnotationDialogForm(String id, final ModalWindow aModalWindow) {
			super(id);

			final FeedbackPanel feedbackPanel = new FeedbackPanel(
					"feedbackPanel");
			add(feedbackPanel);
			feedbackPanel.setOutputMarkupId(true);
			feedbackPanel.add(new AttributeModifier("class", "info"));
			feedbackPanel.add(new AttributeModifier("class", "error"));

			// if it is new arc annotation
			if (selectedArcId == -1) {
				// for rapid annotation, pre-fill previous annotation type again
				if (bratAnnotatorModel.getRememberedArcLayer() != null
						&& selectedLayer.getName().equals(
								bratAnnotatorModel.getRememberedArcLayer()
										.getName())) {
					layersModel = new Model<AnnotationLayer>(selectedLayer);
					featuresModel = new Model<AnnotationFeature>(
							bratAnnotatorModel.getRememberedArcFeature());
					tagsModel = new Model<Tag>(
							bratAnnotatorModel.getRememberedArcTag());
				} else {
					layersModel = new Model<AnnotationLayer>(selectedLayer);
					featuresModel = new Model<AnnotationFeature>(
							selectedFeature);
					tagsModel = new Model<Tag>(null);
				}

			} else {
				layersModel = new Model<AnnotationLayer>(selectedLayer);
				featuresModel = new Model<AnnotationFeature>(selectedFeature);
				Tag tag;
				try {
					tag = annotationService.getTag(selectedArcType,
							selectedFeature.getTagset());
					tagsModel = new Model<Tag>(tag);
				} catch (Exception e) { // It is a tag which is not in the tag
										// list.
					// If we allow user to add Tags from the monitor, we can do
					// as follows
					// post 1.0.0
					/*
					 * tag = new Tag();
					 * tag.setName(TypeUtil.getLabel(selectedArcType));
					 * tag.setTagSet(selectedtTagSet); try {
					 * annotationService.createTag(tag,
					 * bratAnnotatorModel.getUser()); } catch (IOException e1) {
					 * error(e1.getMessage()); } tagsModel = new
					 * Model<Tag>(tag);
					 */
					// Otherwise just clear the tag and the user select from the
					// existing tag lists
					tagsModel = new Model<Tag>(null);
				}

			}

			for (AnnotationFeature feature : annotationService
					.listAnnotationFeature(selectedLayer)) {
				if (feature.getName().equals(
						WebAnnoConst.COREFERENCE_TYPE_FEATURE)) {
					continue;
				}
				spanFeatures.add(feature);
			}

			tags = new ComboBox<Tag>("tags", new Model<String>(
					tagsModel.getObject() == null ? "" : tagsModel.getObject()
							.getName()),
					annotationService.listTags(selectedFeature.getTagset()),
					new ComboBoxRenderer<Tag>("name", "name"));
			add(tags);

			add(new DropDownChoice<AnnotationLayer>("layers", layersModel,
					Arrays.asList(new AnnotationLayer[] { selectedLayer }))
					.setNullValid(false)
					.setChoiceRenderer(new ChoiceRenderer<AnnotationLayer>() {
						private static final long serialVersionUID = 1L;

						@Override
						public Object getDisplayValue(AnnotationLayer aObject) {
							return aObject.getUiName();
						}
					}).setOutputMarkupId(true));

			add(new DropDownChoice<AnnotationFeature>("features",
					featuresModel, spanFeatures) {
				private static final long serialVersionUID = -508831184292402704L;

				@Override
				protected void onSelectionChanged(
						AnnotationFeature aNewSelection) {
					tagsModel.setObject(new Tag());

					updateTagsComboBox();

				}

				@Override
				protected boolean wantOnSelectionChangedNotifications() {
					return true;
				}

				@Override
				protected CharSequence getDefaultChoice(String aSelectedValue) {
					return "";
				}

			}.setChoiceRenderer(new ChoiceRenderer<AnnotationFeature>() {
				private static final long serialVersionUID = 1L;

				@Override
				public Object getDisplayValue(AnnotationFeature aObject) {
					return aObject.getTagset() == null ? aObject.getUiName()
							+ "[Free Span]" : aObject.getUiName();
				}
			}).setOutputMarkupId(true));

			add(new AjaxButton("annotate") {
				private static final long serialVersionUID = 8922161039500097566L;

				@Override
				public void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm) {
					JCas jCas;
					try {
						jCas = getCas(bratAnnotatorModel);

						if (tags.getModelObject() == null) {
							aTarget.add(feedbackPanel);
							error("No Tag is selected");
						} else if (!annotationService.existsTag(
								tags.getModelObject(),
								selectedFeature.getTagset())) {
							aTarget.add(feedbackPanel);
							error(tags.getModelObject()
									+ " is not in the tag list. Please choose form the existing tags");
						} else {
							Tag selectedTag = annotationService.getTag(
									tags.getModelObject(),
									selectedFeature.getTagset());

							AnnotationFS originFs = selectByAddr(jCas,
									originSpanId);
							AnnotationFS targetFs = selectByAddr(jCas,
									targetSpanId);

							// save this annotation detail for next time
							// annotation
							bratAnnotatorModel
									.setRememberedArcLayer(selectedLayer);
							bratAnnotatorModel
									.setRememberedArcFeature(selectedFeature);
							bratAnnotatorModel.setRememberedArcTag(selectedTag);

							TypeAdapter adapter = getAdapter(selectedLayer,
									annotationService);
							if (adapter instanceof ArcAdapter) {
								((ArcAdapter) adapter)
										.setCrossMultipleSentence(selectedLayer
												.isCrossSentence());

								((ArcAdapter) adapter).add(
										selectedTag.getName(), originFs,
										targetFs, jCas, bratAnnotatorModel);
							} else {
								((ChainAdapter) adapter).add(selectedTag
										.getName(), jCas, originFs.getBegin(),
										targetFs.getEnd(), originFs, targetFs,
										selectedTag.getTagSet().getFeature());
							}

							// update timestamp now
							int sentenceNumber = BratAjaxCasUtil
									.getSentenceNumber(jCas,
											originFs.getBegin());
							bratAnnotatorModel.getDocument()
									.setSentenceAccessed(sentenceNumber);
							repository.updateTimeStamp(
									bratAnnotatorModel.getDocument(),
									bratAnnotatorModel.getUser(),
									bratAnnotatorModel.getMode());

							repository.updateJCas(bratAnnotatorModel.getMode(),
									bratAnnotatorModel.getDocument(),
									bratAnnotatorModel.getUser(), jCas);

							if (bratAnnotatorModel.isScrollPage()) {
								int start = originFs.getBegin();
								updateSentenceAddressAndOffsets(jCas, start);
							}
							bratAnnotatorModel
									.setMessage("The arc annotation ["
											+ selectedTag.getName()
											+ "] is added");

							aModalWindow.close(aTarget);
						}
					} catch (UIMAException e) {
						error(ExceptionUtils.getRootCauseMessage(e));
					} catch (ClassNotFoundException e) {
						error(e.getMessage());
					} catch (IOException e) {
						error(e.getMessage());
					} catch (BratAnnotationException e) {
						aTarget.add(feedbackPanel);
						error(e.getMessage());
					}

				}
			}.add(new Behavior() {
				private static final long serialVersionUID = -3612493911620740735L;

				@Override
				public void renderHead(Component component,
						IHeaderResponse response) {
					super.renderHead(component, response);
					response.renderOnLoadJavaScript("$('#"
							+ component.getMarkupId() + "').focus();");
				}
			}));

			add(new AjaxSubmitLink("delete") {
				private static final long serialVersionUID = 1L;

				@Override
				public void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm) {
					JCas jCas;
					try {
						jCas = getCas(bratAnnotatorModel);
						TypeAdapter adapter = getAdapter(selectedLayer,
								annotationService);
						adapter.delete(jCas, selectedArcId);
						repository.updateJCas(bratAnnotatorModel.getMode(),
								bratAnnotatorModel.getDocument(),
								bratAnnotatorModel.getUser(), jCas);

						// update timestamp now
						int sentenceNumber = BratAjaxCasUtil.getSentenceNumber(
								jCas,
								BratAjaxCasUtil.selectByAddr(jCas,
										selectedArcId).getBegin());
						bratAnnotatorModel.getDocument().setSentenceAccessed(
								sentenceNumber);
						repository.updateTimeStamp(
								bratAnnotatorModel.getDocument(),
								bratAnnotatorModel.getUser(),
								bratAnnotatorModel.getMode());

						if (bratAnnotatorModel.isScrollPage()) {
							AnnotationFS originFs = selectByAddr(jCas,
									originSpanId);
							int start = originFs.getBegin();
							updateSentenceAddressAndOffsets(jCas, start);
						}
						bratAnnotatorModel.setMessage("The arc annotation ["
								+ selectedArcType + "] is deleted");

					} catch (UIMAException e) {
						aTarget.add(feedbackPanel);
						error(ExceptionUtils.getRootCauseMessage(e));
					} catch (ClassNotFoundException e) {
						aTarget.add(feedbackPanel);
						error(e.getMessage());
					} catch (IOException e) {
						aTarget.add(feedbackPanel);
						error(e.getMessage());
					}
					aModalWindow.close(aTarget);
				}

				@Override
				public boolean isVisible() {
					return isModify;
				}
			});

			add(new AjaxSubmitLink("reverse") {
				private static final long serialVersionUID = 1L;

				@Override
				public void onSubmit(AjaxRequestTarget aTarget, Form<?> aForm) {
					aTarget.add(feedbackPanel);
					JCas jCas;
					try {
						jCas = getCas(bratAnnotatorModel);

						AnnotationFS idFs = selectByAddr(jCas, selectedArcId);

						jCas.removeFsFromIndexes(idFs);

						Tag selectedTag = tagsModel.getObject();

						AnnotationFS originFs = selectByAddr(jCas, originSpanId);
						AnnotationFS targetFs = selectByAddr(jCas, targetSpanId);

						// save this annotation detail for next time
						// annotation
						bratAnnotatorModel.setRememberedArcLayer(selectedLayer);
						bratAnnotatorModel
								.setRememberedArcFeature(selectedFeature);
						bratAnnotatorModel.setRememberedArcTag(selectedTag);

						TypeAdapter adapter = getAdapter(selectedLayer,
								annotationService);
						if (adapter instanceof ArcAdapter) {
							((ArcAdapter) adapter).add(selectedTag.getName(),
									targetFs, originFs, jCas,
									bratAnnotatorModel);
						} else {
							error("chains cannot be reversed");
							return;
						}

						repository.updateJCas(bratAnnotatorModel.getMode(),
								bratAnnotatorModel.getDocument(),
								bratAnnotatorModel.getUser(), jCas);
						// update timestamp now

						int sentenceNumber = BratAjaxCasUtil.getSentenceNumber(
								jCas, originFs.getBegin());
						bratAnnotatorModel.getDocument().setSentenceAccessed(
								sentenceNumber);
						repository.updateTimeStamp(
								bratAnnotatorModel.getDocument(),
								bratAnnotatorModel.getUser(),
								bratAnnotatorModel.getMode());

						if (bratAnnotatorModel.isScrollPage()) {
							int start = originFs.getBegin();
							updateSentenceAddressAndOffsets(jCas, start);
						}

						bratAnnotatorModel.setMessage("The arc annotation  ["
								+ selectedArcType + "] is reversed");

					} catch (UIMAException e) {
						error(ExceptionUtils.getRootCauseMessage(e));
					} catch (ClassNotFoundException e) {
						error(e.getMessage());
					} catch (IOException e) {
						error(e.getMessage());
					} catch (BratAnnotationException e) {
						aTarget.prependJavaScript("alert('" + e.getMessage()
								+ "')");
					}
					aModalWindow.close(aTarget);
				}

				@Override
				public boolean isVisible() {
					return isModify && !ischain;
				}
			});
		}

		private void updateTagsComboBox() {
			tags.remove();
			tags = new ComboBox<Tag>("tags", new Model<String>(
					tagsModel.getObject() == null ? "" : tagsModel.getObject()
							.getName()),
					annotationService.listTags(selectedFeature.getTagset()),
					new ComboBoxRenderer<Tag>("name", "name"));
			add(tags);
			;
		}
	}

	private void updateSentenceAddressAndOffsets(JCas jCas, int start) {
		int address = BratAjaxCasUtil.selectSentenceAt(jCas,
				bratAnnotatorModel.getSentenceBeginOffset(),
				bratAnnotatorModel.getSentenceEndOffset()).getAddress();
		bratAnnotatorModel.setSentenceAddress(BratAjaxCasUtil
				.getSentenceBeginAddress(jCas, address, start,
						bratAnnotatorModel.getProject(),
						bratAnnotatorModel.getDocument(),
						bratAnnotatorModel.getWindowSize()));

		Sentence sentence = selectByAddr(jCas, Sentence.class,
				bratAnnotatorModel.getSentenceAddress());
		bratAnnotatorModel.setSentenceBeginOffset(sentence.getBegin());
		bratAnnotatorModel.setSentenceEndOffset(sentence.getEnd());
	}

	private JCas getCas(BratAnnotatorModel aBratAnnotatorModel)
			throws UIMAException, IOException, ClassNotFoundException {

		if (aBratAnnotatorModel.getMode().equals(Mode.ANNOTATION)
				|| aBratAnnotatorModel.getMode().equals(Mode.AUTOMATION)
				|| aBratAnnotatorModel.getMode().equals(Mode.CORRECTION)
				|| aBratAnnotatorModel.getMode().equals(Mode.CORRECTION_MERGE)) {

			return repository.readJCas(aBratAnnotatorModel.getDocument(),
					aBratAnnotatorModel.getProject(),
					aBratAnnotatorModel.getUser());
		} else {
			return repository.getCurationDocumentContent(bratAnnotatorModel
					.getDocument());
		}
	}

	@SuppressWarnings("unused")
	static private class SelectionModel implements Serializable {
		private static final long serialVersionUID = -1L;

		public AnnotationLayer layers;
		public AnnotationFeature features;
		public Tag tags;
		private String selectedText;
	}

	public ArcAnnotationModalWindowPanel(String aId,
			final ModalWindow modalWindow,
			BratAnnotatorModel aBratAnnotatorModel, int aOriginSpanId,
			String aOriginSpanType, int aTargetSpanId, String aTargetSpanType) {
		super(aId);

		long layerId = Integer.parseInt(aOriginSpanType.substring(0,
				aOriginSpanType.indexOf("_")));

		AnnotationLayer spanLayer = annotationService.getLayer(layerId);
		if (spanLayer.isBuiltIn()
				&& spanLayer.getName().equals(POS.class.getName())) {
			this.selectedLayer = annotationService.getLayer(
					Dependency.class.getName(), WebAnnoConst.RELATION_TYPE,
					aBratAnnotatorModel.getProject());
		} else if (spanLayer.getType().equals(WebAnnoConst.CHAIN_TYPE)) {
			this.selectedLayer = spanLayer;// one layer both for the span and arc annotation
		} else {
			for (AnnotationLayer layer : annotationService
					.listAnnotationLayer(aBratAnnotatorModel.getProject())) {
				if (layer.getAttachType() != null
						&& layer.getAttachType().equals(spanLayer)) {
					this.selectedLayer = layer;
					break;
				}
			}
			this.selectedLayer = annotationService.getLayer(layerId);
		}

		if (spanLayer.getType().equals(WebAnnoConst.CHAIN_TYPE)) {
			for (AnnotationFeature feature : annotationService
					.listAnnotationFeature(spanLayer)) {
				if (feature.getName().equals(
						WebAnnoConst.COREFERENCE_RELATION_FEATURE)) {
					this.selectedFeature = feature;
					break;
				}
			}
		} else
			this.selectedFeature = annotationService.listAnnotationFeature(
					selectedLayer).get(0);

		this.originSpanId = aOriginSpanId;
		this.targetSpanId = aTargetSpanId;

		this.bratAnnotatorModel = aBratAnnotatorModel;
		annotationDialogForm = new AnnotationDialogForm("annotationDialogForm",
				modalWindow);
		add(annotationDialogForm);
	}

	public ArcAnnotationModalWindowPanel(String aId,
			final ModalWindow modalWindow,
			BratAnnotatorModel aBratAnnotatorModel, int aOriginSpanId,
			String aOriginSpanType, int aTargetSpanId, String aTargetSpanType,
			int selectedArcId, String aType) {
		super(aId);
		this.selectedArcId = selectedArcId;
		this.selectedArcType = aType.substring(aType.indexOf("_") + 1);

		String id = aType.substring(0, aType.lastIndexOf("_"));
		long layerId;
		if (id.contains("_")) {
			// for chain arcs, strip the first prefix, that is used for chain
			// colouring
			layerId = Integer.parseInt(id.substring(id.indexOf("_") + 1));
		} else {
			layerId = Integer.parseInt(id);
		}
		this.selectedLayer = annotationService.getLayer(layerId);
		if (selectedLayer.getType().equals(WebAnnoConst.CHAIN_TYPE))
			for (AnnotationFeature feature : annotationService
					.listAnnotationFeature(selectedLayer)) {
				if (feature.getName().equals(
						WebAnnoConst.COREFERENCE_RELATION_FEATURE)) {
					this.selectedFeature = feature;

				}
			}
		else
			this.selectedFeature = annotationService.listAnnotationFeature(
					selectedLayer).get(0);

		this.originSpanId = aOriginSpanId;
		this.targetSpanId = aTargetSpanId;

		this.bratAnnotatorModel = aBratAnnotatorModel;
		annotationDialogForm = new AnnotationDialogForm("annotationDialogForm",
				modalWindow);
		add(annotationDialogForm);
		this.isModify = true;

		if (selectedLayer.getType().equals(WebAnnoConst.RELATION_TYPE)) {
			ischain = false;
		}

	}
}
