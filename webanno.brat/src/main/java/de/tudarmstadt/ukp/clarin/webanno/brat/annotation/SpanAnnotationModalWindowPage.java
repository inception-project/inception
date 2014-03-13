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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpSession;

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
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;

import com.googlecode.wicket.jquery.ui.kendo.combobox.ComboBox;
import com.googlecode.wicket.jquery.ui.kendo.combobox.ComboBoxRenderer;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.AnnotationTypeConstant;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAjaxCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.brat.controller.TypeUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

/**
 * A page that is used to display an annotation modal dialog for span annotation
 *
 * @author Seid Muhie Yimam
 *
 */
public class SpanAnnotationModalWindowPage extends WebPage {
	private static final long serialVersionUID = -2102136855109258306L;

	@SpringBean(name = "documentRepository")
	private RepositoryService repository;

	@SpringBean(name = "annotationService")
	private AnnotationService annotationService;

	@SpringBean(name = "jsonConverter")
	private MappingJacksonHttpMessageConverter jsonConverter;

	ComboBox<Tag> tags;
	boolean isModify = false;
	TagSet selectedtTagSet;

	Model<TagSet> tagSetsModel;
	Model<String> tagsModel;
	private final AnnotationDialogForm annotationDialogForm;
	private final BratAnnotatorModel bratAnnotatorModel;
	private final int beginOffset;
	private final int endOffset;
	private String selectedText = null;
	int selectedSpanId = -1;
	String selectedSpanType;

	private class AnnotationDialogForm extends Form<SelectionModel> {
		private static final long serialVersionUID = -4104665452144589457L;

		public AnnotationDialogForm(String id, final ModalWindow aModalWindow) {
			super(id, new CompoundPropertyModel<SelectionModel>(
					new SelectionModel()));

			final FeedbackPanel feedbackPanel = new FeedbackPanel(
					"feedbackPanel");
			add(feedbackPanel);
			feedbackPanel.setOutputMarkupId(true);
			feedbackPanel.add(new AttributeModifier("class", "info"));
			feedbackPanel.add(new AttributeModifier("class", "error"));

			List<TagSet> spanLayers = new ArrayList<TagSet>();

			for (TagSet tagset : bratAnnotatorModel.getAnnotationLayers()) {
				if (tagset.getType().getType().equals("span")) {
					spanLayers.add(tagset);
				}

			}

			if (selectedSpanId != -1) {
				tagSetsModel = new Model<TagSet>(selectedtTagSet);
				tagSetsModel = new Model<TagSet>(selectedtTagSet);
				Tag tag;
				try {
					tag = annotationService.getTag(selectedSpanType,
							selectedtTagSet);
					tagsModel = new Model<String>(tag.getName());
				} catch (Exception e) {// It is a tag which is not in the tag
										// list.
					tagsModel = new Model<String>("");
				}
			} else if (bratAnnotatorModel.getRememberedSpanTagSet() != null
					&& conatinsTagSet(bratAnnotatorModel.getAnnotationLayers(),
							bratAnnotatorModel.getRememberedSpanTagSet())) {
				selectedtTagSet = bratAnnotatorModel.getRememberedSpanTagSet();
				tagSetsModel = new Model<TagSet>(selectedtTagSet);
				tagsModel = new Model<String>(bratAnnotatorModel
						.getRememberedSpanTag().getName());
				// for lemma,stem,comment...
				if (!selectedtTagSet.isShowTag()) {
					tagsModel.setObject(selectedText);
				}
			} else {
				selectedtTagSet = (spanLayers.get(0));
				tagSetsModel = new Model<TagSet>(selectedtTagSet);
				tagsModel = new Model<String>("");

				if (selectedtTagSet.getType().getName()
						.equals(AnnotationTypeConstant.LEMMA)) {
					tagsModel.setObject(selectedText);
				}
			}

			add(new Label("selectedText", selectedText));

			tags = new ComboBox<Tag>("tags", tagsModel,
					selectedtTagSet.isShowTag() ? annotationService
							.listTags(selectedtTagSet) : new ArrayList<Tag>(),
					new ComboBoxRenderer<Tag>("name", "name"));
			add(tags);

			add(new DropDownChoice<TagSet>("tagSets", tagSetsModel, spanLayers) {
				private static final long serialVersionUID = -508831184292402704L;

				@Override
				protected void onSelectionChanged(TagSet aNewSelection) {
					selectedtTagSet = aNewSelection;
					if (!aNewSelection.isShowTag()) {
						tagsModel.setObject(selectedText);
					} else {
						tagsModel.setObject("");
					}

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

			}.setChoiceRenderer(new ChoiceRenderer<TagSet>() {
				private static final long serialVersionUID = 1L;

				@Override
				public Object getDisplayValue(TagSet aObject) {
					return aObject.getName();
				}
			}).setOutputMarkupId(true));

			add(new AjaxButton("annotate") {
				private static final long serialVersionUID = 980971048279862290L;

				@Override
				protected void onSubmit(AjaxRequestTarget aTarget, Form<?> form) {
					try {
						JCas jCas = getCas(bratAnnotatorModel);

						Tag selectedTag;
						if (tags.getModelObject() == null) {
							aTarget.add(feedbackPanel);
							error("No Tag is selected!");
							return;
						}
						// Lemma/stem... do not have tagsets. use free layer.
						if (!selectedtTagSet.isShowTag()
								&& !annotationService.existsTag(
										tags.getModelObject(), selectedtTagSet)) {
							selectedTag = new Tag();
							selectedTag.setName(tags.getModelObject());
							selectedTag.setTagSet(selectedtTagSet);
							annotationService.createTag(selectedTag,
									bratAnnotatorModel.getUser());
						} else if (!annotationService.existsTag(
								tags.getModelObject(), selectedtTagSet)) {
							aTarget.add(feedbackPanel);
							error(tags.getModelObject()
									+ " is not in the tag list. Please choose form the existing tags");
							return;
						} else {
							selectedTag = annotationService.getTag(
									tags.getModelObject(), selectedtTagSet);
						}

						SpanAdapter adapter = (SpanAdapter) getAdapter(
								selectedtTagSet, annotationService);

						adapter.setLockToTokenOffsets(selectedtTagSet.getType()
								.isLockToTokenOffset());
						adapter.setAllowStacking(selectedtTagSet.getType()
								.isAllowSTacking());
						adapter.setAllowMultipleToken(selectedtTagSet.getType()
								.isMultipleTokens());

						adapter.add(jCas, beginOffset, endOffset,
								selectedTag.getName());

						repository.updateJCas(bratAnnotatorModel.getMode(),
								bratAnnotatorModel.getDocument(),
								bratAnnotatorModel.getUser(), jCas);

						// update timestamp now
						int sentenceNumber = BratAjaxCasUtil.getSentenceNumber(
								jCas, beginOffset);
						bratAnnotatorModel.getDocument().setSentenceAccessed(
								sentenceNumber);
						repository.updateTimeStamp(
								bratAnnotatorModel.getDocument(),
								bratAnnotatorModel.getUser(),
								bratAnnotatorModel.getMode());

						if (bratAnnotatorModel.isScrollPage()) {
							updateSentenceAddressAndOffsets(jCas, beginOffset);
						}

						bratAnnotatorModel
								.setRememberedSpanTagSet(selectedtTagSet);
						bratAnnotatorModel.setRememberedSpanTag(selectedTag);
						bratAnnotatorModel.setAnnotate(true);
						bratAnnotatorModel.setMessage("The span annotation ["
								+ selectedTag.getName() + "] is added");

						// A hack to rememeber the Visural DropDown display
						// value
						HttpSession session = ((ServletWebRequest) RequestCycle
								.get().getRequest()).getContainerRequest()
								.getSession();
						session.setAttribute("model", bratAnnotatorModel);

						aModalWindow.close(aTarget);

					} catch (UIMAException e) {
						aTarget.add(feedbackPanel);
						error(ExceptionUtils.getRootCauseMessage(e));
					} catch (ClassNotFoundException e) {
						aTarget.add(feedbackPanel);
						error(e.getMessage());
					} catch (IOException e) {
						aTarget.add(feedbackPanel);
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
					try {
						JCas jCas = getCas(bratAnnotatorModel);
						AnnotationFS fs = selectByAddr(jCas, selectedSpanId);
						TypeAdapter adapter = getAdapter(selectedtTagSet,
								annotationService);
						String attachFeatureName = adapter
								.getAttachFeatureName();

						Set<TypeAdapter> typeAdapters = new HashSet<TypeAdapter>();

						for (TagSet tagSet : annotationService
								.listTagSets(bratAnnotatorModel.getProject())) {
							typeAdapters.add(TypeUtil.getAdapter(tagSet,
									annotationService));
						}
						// delete associated relation annotation
						for (TypeAdapter ad : typeAdapters) {
							if (adapter.getAnnotationTypeName().equals(
									ad.getAnnotationTypeName())) {
								continue;
							}
							String fn = ad.getAttachFeatureName();
							if (fn == null) {
								continue;
							}
							if (fn.equals(attachFeatureName)) {
								Sentence thisSentence = BratAjaxCasUtil
										.getCurrentSentence(jCas, beginOffset,
												endOffset);
								ad.deleteBySpan(jCas, fs,
										thisSentence.getBegin(),
										thisSentence.getEnd());
								break;
							}
						}
						adapter.delete(jCas, selectedSpanId);
						repository.updateJCas(bratAnnotatorModel.getMode(),
								bratAnnotatorModel.getDocument(),
								bratAnnotatorModel.getUser(), jCas);
						// update timestamp now
						int sentenceNumber = BratAjaxCasUtil.getSentenceNumber(
								jCas, beginOffset);
						bratAnnotatorModel.getDocument().setSentenceAccessed(
								sentenceNumber);
						repository.updateTimeStamp(
								bratAnnotatorModel.getDocument(),
								bratAnnotatorModel.getUser(),
								bratAnnotatorModel.getMode());

						if (bratAnnotatorModel.isScrollPage()) {
							updateSentenceAddressAndOffsets(jCas, beginOffset);
						}

						Tag selectedTag;
						if (!selectedtTagSet.isShowTag()) {
							selectedTag = new Tag();
						} else {
							selectedTag = annotationService.getTag(
									tags.getModelObject(), selectedtTagSet);
						}

						bratAnnotatorModel
								.setRememberedSpanTagSet(selectedtTagSet);
						bratAnnotatorModel.setRememberedSpanTag(selectedTag);
						bratAnnotatorModel.setAnnotate(false);
						bratAnnotatorModel.setMessage("The span annotation ["
								+ selectedSpanType + "] is deleted");

						// A hack to rememeber the Visural DropDown display
						// value
						HttpSession session = ((ServletWebRequest) RequestCycle
								.get().getRequest()).getContainerRequest()
								.getSession();
						session.setAttribute("model", bratAnnotatorModel);
						aModalWindow.close(aTarget);
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
				}

				@Override
				public boolean isVisible() {
					return isModify;
				}
			});
		}

		private void updateTagsComboBox() {
			tags.remove();
			tags = new ComboBox<Tag>("tags", tagsModel,
					annotationService.listTags(selectedtTagSet),
					new ComboBoxRenderer<Tag>("name", "name"));
			add(tags);
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

	public class SelectionModel implements Serializable {
		private static final long serialVersionUID = -4178958678920895292L;
		public TagSet tagSets;
		public Tag tags;
		public String selectedText;
	}

	public SpanAnnotationModalWindowPage(ModalWindow modalWindow,
			BratAnnotatorModel aBratAnnotatorModel, String aSelectedText,
			int aBeginOffset, int aEndOffset) {
		this.beginOffset = aBeginOffset;
		this.endOffset = aEndOffset;

		this.selectedText = aSelectedText;

		this.bratAnnotatorModel = aBratAnnotatorModel;
		this.annotationDialogForm = new AnnotationDialogForm(
				"annotationDialogForm", modalWindow);
		add(annotationDialogForm);
	}

	public SpanAnnotationModalWindowPage(ModalWindow modalWindow,
			BratAnnotatorModel aBratAnnotatorModel, String aSelectedText,
			int aBeginOffset, int aEndOffset, String aType, int selectedSpanId) {
		this.selectedSpanId = selectedSpanId;
		this.selectedSpanType = aType.substring(aType.indexOf("_") + 1);

		long featureId = Integer
				.parseInt(aType.substring(0, aType.indexOf("_")));

		AnnotationFeature feature = this.annotationService
				.getFeature(featureId);

		this.selectedtTagSet = this.annotationService.getTagSet(feature,
				aBratAnnotatorModel.getProject());

		this.beginOffset = aBeginOffset;
		this.endOffset = aEndOffset;

		this.selectedText = aSelectedText;

		this.bratAnnotatorModel = aBratAnnotatorModel;
		this.annotationDialogForm = new AnnotationDialogForm(
				"annotationDialogForm", modalWindow);
		add(annotationDialogForm);
		this.isModify = true;
	}

	private boolean conatinsTagSet(Set<TagSet> aTagSets, TagSet aTagSet) {
		for (TagSet tagSet : aTagSets) {
			if (tagSet.getId() == aTagSet.getId()) {
				return true;
			}
		}
		return false;
	}
}
